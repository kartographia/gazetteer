package com.kartographia.gazetteer.source;

import com.kartographia.gazetteer.*;
import com.kartographia.gazetteer.utils.*;
import com.kartographia.gazetteer.utils.Counter;

//javaxt includes
import javaxt.json.*;
import javaxt.sql.*;
import javaxt.io.*;
import javaxt.utils.ThreadPool;
import static javaxt.utils.Console.console;
import javaxt.express.utils.CSV;

//java includes
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.PreparedStatement;


import org.locationtech.jts.geom.*;


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

    private static Source source;
    private static String delimiter = "|";
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
  //** init
  //**************************************************************************
    private static void init(){
        if (source==null);
        try{
            String name = "USGS";
            source = Source.get("name=",name);
            if (source==null){
                source = new Source();
                source.setName(name);
                source.save();
            }
        }
        catch(Exception e){}
    }


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to parse and load data into a database.
   *  @param file "Populated Places" found under "Topical Gazetteers". Example:
   *  http://geonames.usgs.gov/docs/stategaz/POP_PLACES_20161001.zip
   */
    public static void load(javaxt.io.File file, int numThreads, Database database) throws Exception {
        init();


      //Get record count
        Counter counter = new Counter(getBufferedReader(file), true);

      //Get header
        CSV.Columns header = getHeader(file);


      //Create threads to process records in the file
        ThreadPool pool = new ThreadPool(numThreads, 1000){
            public void process(Object obj){
                String row = (String) obj;
                counter.updateCount();


              //Parse row
                CSV.Columns columns = CSV.getColumns(row, delimiter);
                columns.setHeader(header);


              //Parse columns
                long id = columns.get("feature_id").toLong();
                String name = columns.get("feature_name").toString();
                String subType = columns.get("feature_class").toString();
                String type = typeMap.get(subType);
                if (type==null) return;
                if (type.equalsIgnoreCase(subType)) subType = null;
                else subType = subType.toLowerCase();
                String cc = "US";
                String a1 = columns.get("state_alpha").toString();
                if (a1==null) a1 = stateCodes.get(columns.get("state_numeric").toString());
                String a2 = columns.get("county_name").toString();
                BigDecimal lat = columns.get("prim_lat_dec").toBigDecimal();
                BigDecimal lon = columns.get("prim_long_dec").toBigDecimal();
                Integer lastUpdate = getDate(columns.get("date_edited").toString()); //Date edited (e.g. 05/13/2010)
                if (lastUpdate==null) lastUpdate = getDate(columns.get("date_created").toString()); //Date created (e.g. 03/31/1981)

                JSONObject info = null;

                Geometry geom = geometryFactory.createPoint(
                    new Coordinate(lon.doubleValue(), lat.doubleValue())
                );

                Integer rank = getRank(id);
                if (!type.equals("populated place")) rank = null;

                if (name.endsWith("(historical)")){
                    name = name.substring(0, name.length()-"(historical)".length()).trim();
                    if (info==null) info = new JSONObject();
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


              //Update
                updateUSTerritories(place);
                name = updateBuildings(name, place);
                String uname = name.toUpperCase(Locale.US);



              //Save place
                try{
                    place.save();
                }
                catch(Exception e){
                    if (e instanceof IllegalStateException){
                        clear();
                        return;
                    }
                    else{
                        String msg = e.getMessage();
                        if (msg.contains("duplicate key")){
                            update(place);
                        }
                        else{
                            console.log(e.getMessage());
                        }
                    }
                }


              //Save name
                try (Recordset rs = getRecordset(place, uname)) {
                    if (rs.EOF) rs.addNew();
                    rs.setValue("name", name);
                    rs.setValue("uname", uname);
                    rs.setValue("type", 2);
                    rs.setValue("language_code", "eng");
                    rs.setValue("place_id", place.getID());
                    rs.setValue("source_id", source.getID());
                    rs.setValue("source_key", id);
                    rs.setValue("source_date", lastUpdate);
                    rs.update();
                }
                catch(Exception e){
                    console.log(e.getMessage());
                }

            }


            private void update(Place place){
                try{
                    Long placeID = null;
                    Integer currDate = null;
                    PreparedStatement placeQuery = getPlaceQuery();
                    placeQuery.setLong(1, source.getID());
                    placeQuery.setLong(2, place.getSourceKey());
                    placeQuery.setString(3, place.getCountryCode());
                    try (java.sql.ResultSet r2 = placeQuery.executeQuery()){
                        while (r2.next()) {
                            placeID = r2.getLong(1);
                            currDate = r2.getInt(2);
                        }
                    }

                    place.setID(placeID);

                    int lastUpdate = place.getSourceDate();
                    if (lastUpdate>currDate){
                        ChangeRequest cr = new ChangeRequest();
                        cr.setPlace(place);
                        cr.setInfo(place.toJson());
                        cr.save();
                    }
                }
                catch(Exception e){
                }
            }

            private PreparedStatement getPlaceQuery() throws Exception {
                PreparedStatement stmt = (PreparedStatement) get("stmt");
                if (stmt==null){
                    Connection conn = (Connection) get("conn");
                    if (conn==null){
                        conn = database.getConnection();
                        set("conn", conn);
                    }

                    stmt = conn.getConnection().prepareStatement(
                        "select id, source_date from gazetteer.place where source_id=? and source_key=? and country_code=?"
                    );
                    set("stmt", stmt);
                }
                return stmt;
            }


            private Recordset getRecordset(Place place, String uname) throws Exception {
                Connection conn = (Connection) get("c2");
                if (conn==null){
                    conn = database.getConnection();
                    set("c2", conn);
                }


                Recordset rs = (Recordset) get("rs");
                if (rs==null){
                    rs = new Recordset();
                    set("rs", rs);
                }


                rs.open("select * from gazetteer.name where place_id=" + place.getID() +
                " and uname='" + uname.replace("'", "''") + "'", conn, false);

                return rs;
            }

            public void exit(){
                PreparedStatement stmt = (PreparedStatement) get("stmt");
                if (stmt!=null) try{stmt.close();}catch(Exception e){}

                Connection conn = (Connection) get("conn");
                if (conn!=null) conn.close();

                Recordset rs = (Recordset) get("rs");
                if (rs!=null) rs.close();

                conn = (Connection) get("c2");
                if (conn!=null) conn.close();
            }

            private void clear(){
                List list = getQueue();
                synchronized(list){
                    list.clear();
                    done();
                }
            }

        }.start();





      //Parse file and add records to the pool
        try(java.io.BufferedReader br = getBufferedReader(file)){
            br.readLine();
            String row;
            while (!(row=CSV.readLine(br)).isEmpty()){
                pool.add(row);
            }
        }


        pool.done();
        pool.join();

        counter.stop();
    }


  //**************************************************************************
  //** download
  //**************************************************************************
  /** Used to download the "National File" from the USGS website
   */
    public static javaxt.io.File download(Directory downloadDir, boolean unzip) throws Exception {
        java.net.URL url = new java.net.URL(
        "https://www.usgs.gov/us-board-on-geographic-names/download-gnis-data");
        javaxt.http.Response response = new javaxt.http.Request(url).getResponse();
        if (response.getStatus()!=200) throw new Exception("Failed to connect to " + url);
        String html = response.getText();

        javaxt.io.File file = null;
        for (javaxt.html.Element el : new javaxt.html.Parser(html).getElementsByTagName("a")){
            String text = el.getInnerText();
            if (text.equals("All Names")){
                String href = el.getAttribute("href");
                String link = javaxt.html.Parser.MapPath(href, url);
                String path = new javaxt.utils.URL(link).getPath();
                if (path.endsWith("/")) path = path.substring(0, path.length()-1);
                String fileName = path.substring(path.lastIndexOf("/")+1);
                file = new javaxt.io.File(downloadDir + "usgs/" + fileName);
                if (!file.exists()){
                    response = new javaxt.http.Request(link).getResponse();
                    if (response.getStatus()!=200) throw new Exception("Failed to download " + link);
                    System.out.print("Downloading " + file + "...");
                    try (java.io.InputStream is = response.getInputStream()){
                        file.write(is);
                    }
                    System.out.println("Done!");
                }
                break;
            }
        }



        if (file==null) throw new Exception("Failed to find file to download");

        if (unzip){
            javaxt.io.File countryFile = new javaxt.io.File(file.getDirectory(), file.getName(false) + ".txt");
            ZipInputStream zip = new ZipInputStream(file.getInputStream());
            ZipEntry entry;
            while((entry = zip.getNextEntry())!=null){
                if (hasData(entry)){
                    byte[] buffer = new byte[2048];
                    java.io.FileOutputStream output = countryFile.getOutputStream();
                    int len;
                    while ((len = zip.read(buffer)) > 0){
                        output.write(buffer, 0, len);
                        output.flush();
                    }
                    output.close();
                    break;
                }
            }
            zip.close();
        }

        return file;
    }


  //**************************************************************************
  //** getBufferedReader
  //**************************************************************************
    private static BufferedReader getBufferedReader(javaxt.io.File file) throws Exception {
        if (file.getExtension().equals("zip")){
            ZipFile zipFile = new ZipFile(file.toFile());

            BufferedInputStream bis = new BufferedInputStream(file.getInputStream());
            ZipInputStream zip = new ZipInputStream(bis);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                if (hasData(zipEntry)){
                    InputStream is = zipFile.getInputStream(zipEntry);
                    return new BufferedReader(new InputStreamReader(is, "UTF-8"));
                }
            }
        }
        else{
            return file.getBufferedReader("UTF-8");
        }
        return null;
    }


  //**************************************************************************
  //** hasData
  //**************************************************************************
    private static boolean hasData(ZipEntry zipEntry){
        if (!zipEntry.isDirectory()) {
            String fileName = zipEntry.getName();
            int idx = fileName.lastIndexOf("/");
            if (idx>-1) fileName = fileName.substring(idx+1);
            if (fileName.startsWith("NationalFile_") ||
                fileName.startsWith("DomesticNames_National.txt")){
                return true;
            }
        }
        return false;
    }


  //**************************************************************************
  //** getHeader
  //**************************************************************************
    private static CSV.Columns getHeader(javaxt.io.File file) throws Exception {
        try(java.io.BufferedReader br = getBufferedReader(file)){
            return CSV.parseHeader(br, delimiter);
        }
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
        HashMap<String, String> cc = new HashMap<>();
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
        typeMap.put("Populated Place", "populated place");
        typeMap.put("Post Office", "amenity");
        typeMap.put("School", "education");
        typeMap.put("Tower", "infrastructure");
        typeMap.put("Tunnel", "transportation");
        typeMap.put("Well", "industry");
    }


  //**************************************************************************
  //** stateCodes
  //**************************************************************************
  /** Used to map state codes to 2-character abbreviations
   */
    private static HashMap<String, String> stateCodes = new HashMap<String, String>();
    static {
        stateCodes.put("01", "AL"); //Alabama
        stateCodes.put("02", "AK"); //Alaska
        stateCodes.put("04", "AZ"); //Arizona
        stateCodes.put("05", "AR"); //Arkansas
        stateCodes.put("06", "CA"); //California
        stateCodes.put("08", "CO"); //Colorado
        stateCodes.put("09", "CT"); //Connecticut
        stateCodes.put("10", "DE"); //Delaware
        stateCodes.put("11", "DC"); //District of Columbia
        stateCodes.put("12", "FL"); //Florida
        stateCodes.put("13", "GA"); //Georgia
        stateCodes.put("15", "HI"); //Hawaii
        stateCodes.put("16", "ID"); //Idaho
        stateCodes.put("17", "IL"); //Illinois
        stateCodes.put("18", "IN"); //Indiana
        stateCodes.put("19", "IA"); //Iowa
        stateCodes.put("20", "KS"); //Kansas
        stateCodes.put("21", "KY"); //Kentucky
        stateCodes.put("22", "LA"); //Louisiana
        stateCodes.put("23", "ME"); //Maine
        stateCodes.put("24", "MD"); //Maryland
        stateCodes.put("25", "MA"); //Massachusetts
        stateCodes.put("26", "MI"); //Michigan
        stateCodes.put("27", "MN"); //Minnesota
        stateCodes.put("28", "MS"); //Mississippi
        stateCodes.put("29", "MO"); //Missouri
        stateCodes.put("30", "MT"); //Montana
        stateCodes.put("31", "NE"); //Nebraska
        stateCodes.put("32", "NV"); //Nevada
        stateCodes.put("33", "NH"); //New Hampshire
        stateCodes.put("34", "NJ"); //New Jersey
        stateCodes.put("35", "NM"); //New Mexico
        stateCodes.put("36", "NY"); //New York
        stateCodes.put("37", "NC"); //North Carolina
        stateCodes.put("38", "ND"); //North Dakota
        stateCodes.put("39", "OH"); //Ohio
        stateCodes.put("40", "OK"); //Oklahoma
        stateCodes.put("41", "OR"); //Oregon
        stateCodes.put("42", "PA"); //Pennsylvania
        stateCodes.put("44", "RI"); //Rhode Island
        stateCodes.put("45", "SC"); //South Carolina
        stateCodes.put("46", "SD"); //South Dakota
        stateCodes.put("47", "TN"); //Tennessee
        stateCodes.put("48", "TX"); //Texas
        stateCodes.put("49", "UT"); //Utah
        stateCodes.put("50", "VT"); //Vermont
        stateCodes.put("51", "VA"); //Virginia
        stateCodes.put("53", "WA"); //Washington
        stateCodes.put("54", "WV"); //West Virginia
        stateCodes.put("55", "WI"); //Wisconsin
        stateCodes.put("56", "WY"); //Wyoming
        stateCodes.put("60", "AS"); //American Samoa
        stateCodes.put("66", "GU"); //Guam
        stateCodes.put("69", "MP"); //Commonwealth of the Northern Mariana Islands
        stateCodes.put("72", "PR"); //Puerto Rico
        stateCodes.put("74", "UM"); //U.S. Minor Outlying Islands
        stateCodes.put("78", "VI"); //United States Virgin Islands
    }

}