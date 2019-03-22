package com.kartographia.gazetter.source;
import com.kartographia.gazetter.*;
import com.kartographia.gazetter.source.Utils.*;
import com.vividsolutions.jts.geom.*;
import javaxt.json.*;
import javaxt.sql.*;
import java.util.*;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.math.BigDecimal;

//******************************************************************************
//**  USGS Data Loader
//******************************************************************************
/**
 *  Used to create/update records in the gazetteer published by the USGS:
 *  http://geonames.usgs.gov/domestic/download_data.htm
 *
 *  File specs:
 *  http://geonames.usgs.gov/domestic/states_fileformat.htm
 ******************************************************************************/

public class USGS {

    private static String delimiter = "\\|";
    private static int washingtonDC = 531871;
    private static int[] stateCapitals = new int[]{
        1024242,1035849,1080996,1102140,1167861,1213649,1219851,1245051,1266887,
        1384879,1404263,1454997,1461834,1499957,1533353,1558347,1581834,1609077,
        1625035,1629914,1652484,165344,1659564,201738,213160,217882,308416,
        351615,366212,400590,426595,44784,452890,465961,485477,517517,581636,
        595031,617565,662851,711543,758233,802116,83350,837279,863976,873303,
        884540,936823,977310
    };
    private static HashMap<String, String> usTerritories = getTerritories();
    private static PrecisionModel precisionModel = new PrecisionModel();
    private static GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 4326);


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to parse and load data into a database.
   *  @param file "Populated Places" found under "Topical Gazetteers". Example:
   *  http://geonames.usgs.gov/docs/stategaz/POP_PLACES_20161001.zip
   */
    public static void load(javaxt.io.File file, Database database) throws Exception {
        Source source = Source.get("name=","USGS");


      //Get record count
        Counter counter = new Counter(file, true);


      //Create prepared statement to find places
        java.sql.PreparedStatement placeQuery;
        javaxt.sql.Connection conn = database.getConnection();
        placeQuery = conn.getConnection().prepareStatement(
            "select id, source_date from place where source_id=? and source_key=? and country_code=?"
        );


      //Parse file
        java.io.BufferedReader br = file.getBufferedReader("UTF-8");
        br.readLine(); //Skip header
        String row;
        while ((row = br.readLine()) != null){
            counter.updateCount();


          //Parse columns
            String[] col = row.split(delimiter, -1);
            long id = Long.parseLong(col[0]);
            String name = col[1];
            String subType = col[2];
            String type = typeMap.get(subType);
            if (type==null) continue;
            String cc = "US";
            String a1 = col[3];
            String a2 = col[5];
            BigDecimal lat = new BigDecimal(col[9]);
            BigDecimal lon = new BigDecimal(col[10]);
            Integer lastUpdate = getDate(col[19]); //Date edited (e.g. 05/13/2010)
            if (lastUpdate==null) lastUpdate = getDate(col[18]); //Date created (e.g. 03/31/1981)

            JSONObject info = new JSONObject();
            info.set("latitude", lat);
            info.set("longitude", lon);
            Geometry geom = geometryFactory.createPoint(
                new Coordinate(lon.doubleValue(), lat.doubleValue())
            );

            Integer rank = getRank(id);
            if (!type.equals("residential")) rank = null;

            if (name.endsWith("(historical)")){
                name = name.substring(0, name.length()-"(historical)".length()).trim();
                info.set("note", "historical");
            }


          //Instantiate Place
            Place place = new Place();
            place.setCountryCode(cc);
            place.setAdmin1(a1);
            place.setAdmin2(a2);
            place.setGeom(geom);
            place.setType(type);
            place.setSubtype(subType);
            place.setRank(rank);
            place.setSource(source);
            place.setSourceKey(id);
            place.setSourceDate(lastUpdate);
            place.setInfo(info);

            updateUSTerritories(place);
            name = updateBuildings(name, place);


          //Save place
            try{
                place.save();
            }
            catch(Exception e){
                //probably a duplicate

                Long placeID = null;
                Integer currDate = null;
                placeQuery.setLong(1, source.getID());
                placeQuery.setLong(2, id);
                placeQuery.setString(3, place.getCountryCode());
                java.sql.ResultSet r2 = placeQuery.executeQuery();
                while (r2.next()) {
                    placeID = r2.getLong(1);
                    currDate = r2.getInt(2);
                }
                r2.close();

                place.setID(placeID);


                if (lastUpdate>currDate){
                    ChangeRequest cr = new ChangeRequest();
                    cr.setPlaceID(placeID);
                    cr.setInfo(place.toJson());
                    cr.save();
                }
            }


          //Instantiate PlaceName
            PlaceName placeName = new PlaceName();
            placeName.setName(name);
            placeName.setLanguageCode("eng");
            placeName.setType(2);
            placeName.setPlace(place);


          //Save PlaceName
            try{
                placeName.save();
            }
            catch(Exception e){
                //probably a duplicate
            }
        }


      //Clean-up
        placeQuery.close();
        conn.close();
        br.close();
    }


  //**************************************************************************
  //** getDate
  //**************************************************************************
  /** Converts a date in MM/DD/YYYY format and returns YYYYMMDD
   */
    private static Integer getDate(String str){
        try{
            String[] arr = str.split("/");
            return Integer.parseInt(arr[2]+arr[0]+arr[1]);
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** getRank
  //**************************************************************************
  /** Used to set the default rank of a city using a list of known feature IDs
   *  for the national and state capitals. The tank will be updated using
   *  population data in a post-processing step.
   */
    private static int getRank(long id) {

        if (id==washingtonDC) return 1;

        for (int i : stateCapitals){
            if (id==i) return 2;
        }

        return 5;
    }


  //**************************************************************************
  //** updateUSTerritories
  //**************************************************************************
  /** Used to update country codes for cities located in US territories. Cities
   *  in US territories are assigned a FIPS country code instead of "US" (e.g.
   *  "VQ" for the US Virgin Islands and "RQ" for Puerto Rico). Also, "state"
   *  capitals are updated to a rank of 1.
   */
    private static void updateUSTerritories(Place place) {

        String a1 = place.getAdmin1();
        String fips_cc = usTerritories.get(a1);
        if (fips_cc!=null){
            place.setCountryCode(fips_cc);

            Integer rank = place.getRank();
            if (rank!=null && rank==2){
                place.setRank(1);
            }
        }
    }


  //**************************************************************************
  //** getTerritories
  //**************************************************************************
  /** Returns a map of ISO 3166-2 country codes and FIPS country codes. The
   *  keys in  the map are ISO codes that USGS uses and the corresponding
   *  values are the FIPS Country codes that NGA uses.
   */
    private static HashMap<String, String> getTerritories(){
        HashMap<String, String> cc = new HashMap<String, String>();
        cc.put("AS", "AQ");
        cc.put("MP", "CQ");
        cc.put("FM", "FM");
        cc.put("GU", "GQ");
        cc.put("PW", "PS");
        cc.put("MH", "RM");
        cc.put("PR", "RQ");
        cc.put("VI", "VQ");
        return cc;
    }


  //**************************************************************************
  //** checkForUpdates
  //**************************************************************************
    public int checkForUpdates(Database db){
        return 0;
    }


  //**************************************************************************
  //** downloadUpdates
  //**************************************************************************
    public javaxt.io.File[] downloadUpdates(Database db, javaxt.io.Directory downloadDir){
        return new javaxt.io.File[0];
    }


  //**************************************************************************
  //** updateBuildings
  //**************************************************************************
  /** Used to update the type and subtype of a "Building"
   */
    private static String updateBuildings(String name, Place place){
        String type = place.getType();
        String subType = place.getSubtype();


      //Update name
        if (name.endsWith("Shopping Center")){
            subType = "shopping center or mall";
            name = name.substring(0, name.length()-"Shopping Center".length()).trim();
        }



      //Update types and subtypes for buildings
        if (subType.equalsIgnoreCase("Building")){
            if (name.contains("Police") || name.contains("Sheriff") || name.contains("Highway Patrol")){
                type = "administrative";
                subType = "police post";
            }
            else if (name.contains("Jail") || name.contains("Penitentiary") || name.contains("Prison") ||
                name.contains("Correctional") || name.contains("Corrections") || name.contains("Correction") ||
                name.contains("Detention") || name.contains("Juvenile") || name.contains("Security Center")){
                type = "administrative";
                subType = "prison";
            }
            else if (name.contains("Court")){
                type = "administrative";
                subType = "courthouse";
            }
            else if (name.contains("Port of Entry")){
                type = "administrative";
                subType = "border post";
            }
            else if (name.contains("Department of ") || name.contains("United States") || name.contains("Federal Center")){
                type = "administrative";
                subType = "administrative facility";
            }
            else if (name.contains("City Hall") || name.contains("Town Hall") || name.contains("Townhall") ||
                    name.contains("Mayor's Office") || name.contains("Governor Mansion") ||
                    name.contains("State Capitol") ){
                type = "administrative";
                subType = "local government office";
            }
            else if (name.contains("Embassy") || name.contains("Consulate")){
                type = "administrative";
                subType = "diplomatic facility";
            }
            else if (name.contains("Fire Department") || name.contains("Fire Company") || name.contains("Fire Station") ||
                    name.contains("Firehouse") || name.contains("Fire Authority") || name.contains("Fire District") ||
                    name.contains("Fire and ") ||name.contains("Volunteer Fire") ||
                    name.contains("EMS") || name.contains("HAZMAT") ||
                    name.contains("Emergency") || name.contains("Rescue")){
                type = "infrastructure";
                subType = "fire station";
            }
            else if (name.contains("Power Plant") || name.contains("Powerplant") || name.contains("Steam Plant") ||
                    name.contains("Power House") || name.contains("Powerhouse") || name.contains("Substation") ||
                    name.contains("Light and Power") || name.contains("Hydroelectric")){
                type = "infrastructure";
                subType = "power station";
            }
            else if (name.contains("Pumping Station")){
                type = "infrastructure";
                subType = "pumping station";
            }
            else if (name.contains("Hospital") || name.contains("Medical Center")){
                type = "amenity";
                subType = "hospital";
            }
            else if (name.contains("Museum") || name.contains("Gallery") || name.contains("Arts")){
                type = "cultural";
                subType = "museum";
            }
            else if (name.contains("Auditorium") || name.contains("Arena") || name.contains("Amphitheatre") ||
                    name.contains("Theatre") || name.contains("Theater") || name.contains("Coliseum") ||
                    name.contains("Convention Center") || name.contains("Civic Center")){
                type = "cultural";
                subType = "amphitheater";
            }
            else if (name.contains("College") || name.contains("University") || name.contains("International Studies")){
                type = "education";
                subType = "college";
            }
            else if (name.endsWith("Historic Site") ){
                name = name.substring(0, name.length()-"Historic Site".length()).trim();
                type = "cultural";
                subType = "historic site";
            }
            else if (name.contains("Library")){
                type = "amenity";
                subType = "library";
            }
            else if (name.contains("Railroad Station")){
                type = "transportation";
                subType = "railroad station";
            }
        }

        place.setType(type);
        place.setSubtype(subType);
        return name;
    }


  //**************************************************************************
  //** typeMap
  //**************************************************************************
  /** Used to map USGS features to a type
   */
    private static HashMap<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("Airport", "transportation");
        typeMap.put("Bridge", "transportation");
        typeMap.put("Building", "amenity");
        typeMap.put("Canal", "transportation");
        typeMap.put("Cemetery", "religious");
        typeMap.put("Church", "religious");
        typeMap.put("Crossing", "transportation");
        typeMap.put("Dam", "infrastructure");
        typeMap.put("Harbor", "transportation");
        typeMap.put("Hospital", "amenity");
        typeMap.put("Locale", "amenity");
        typeMap.put("Military", "military");
        typeMap.put("Mine", "industry");
        typeMap.put("Oilfield", "industry");
        typeMap.put("Park", "amenity");
        typeMap.put("Populated Place", "residential");
        typeMap.put("Post Office", "amenity");
        typeMap.put("School", "education");
        typeMap.put("Tower", "infrastructure");
        typeMap.put("Tunnel", "transportation");
        typeMap.put("Well", "industry");
    }
}