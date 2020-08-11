package com.kartographia.gazetter.source;
import com.kartographia.gazetter.*;
import com.kartographia.gazetter.source.Utils.*;
import com.vividsolutions.jts.geom.*;

//javaxt includes
import javaxt.json.*;
import javaxt.sql.*;
import javaxt.io.*;
import javaxt.utils.ThreadPool;
import static javaxt.utils.Console.console;

//java includes
import java.util.*;
import java.util.zip.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;


//******************************************************************************
//**  NGA Data Loader
//******************************************************************************
/**
 *  Used to parse and load data from NGA's GEOnet Names Server (GNS):
 *  http://geonames.nga.mil/gns/html/index.html
 *
 *  Note that the NGA gazetter is updated approximately once a week. Also, note
 *  that the file format changes periodically. Information on the file format
 *  can be found here:
 *  http://geonames.nga.mil/gns/html/gis_countryfiles.html
 *
 ******************************************************************************/

public class NGA {

    private static String delimiter = "\t";
    //private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static PrecisionModel precisionModel = new PrecisionModel();
    private static GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 4326);


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to parse and load data into a database.
   *
   * @param file The "Entire country files dataset" from the following URL:
   *  http://geonames.nga.mil/gns/html/namefiles.html
   *
   *  Example:
   *  http://geonames.nga.mil/gns/html/cntyfile/geonames_20161107.zip
   *
   */
    public static void load(File file, int numThreads, Database database) throws Exception {
        Source source = Source.get("name=","NGA");


      //Get record count
        Counter counter = new Counter(getBufferedReader(file), true);




      //Create threads to process records in the file
        ThreadPool pool = new ThreadPool(numThreads){ //no limit?
            public void process(Object obj){
                String row = (String) obj;
                counter.updateCount();


              //Update column values
                String[] col = row.split(delimiter, -1);
                for (int i=0; i<col.length; i++){
                    col[i] = col[i].trim();
                    if (col[i].length()==0) col[i] = null;
                }




              //Check whether to insert or skip the record
                String fc = col[9].toUpperCase(); //Feature Class (e.g. P = Populated place)
                String dsg = col[10].toUpperCase(); //Feature Designation Code. Read more http://www.geonames.org/export/codes.html
                String[] type;
                if (fc.equals("P") || fc.equals("L") || fc.equals("S")){
                    type = typeMap.get(dsg);
                    if (type==null) return;
                }
                else return;



              //Parse fields
                Long id = Long.parseLong(col[1]); //Unique Feature Identifier (UFI)
                double latitude = Double.parseDouble(col[3]);
                double longitude = Double.parseDouble(col[4]);
                String countryCodes = col[12];
                String a1 = col[13];
                Integer pop = cint(col[14]);
                int nameType = getNameType(col[17]);

                String languageCode = col[18];
                if (languageCode!=null){
                    if (col[17].endsWith("S") && languageCode.equals("eng")){
                        languageCode = null;
                    }
                }


                int lastUpdate = cint(col[28].replace("-", "")); //2015-03-02 --> 20150302



              //Set rank using the Feature Designation Code
                int rank;

                if (dsg.equals("PPLC")){ //capital of a political entity
                    rank = 1;
                }
                else if (dsg.equals("PPLA")){ //seat of of a first-order administrative division (e.g. state capital)
                    rank = 2;
                }
                else if (dsg.equals("PPLA2")){ //seat of of a second-order administrative division (e.g. county)
                    rank = 3;
                }
                else if (dsg.equals("PPLA3")){ //seat of of a third-order administrative division
                    rank = 4;
                }
                else{
                    rank = 5;
                }


              //Update rank using the Display field
                int minScale = 1;
                String display = col[29];
                if (display!=null){
                    String[] arr = display.split(",");
                    Integer x = cint(arr[arr.length-1]);
                    if (x!=null) minScale = x;
                }

                if (rank==5){
                    if (minScale==9) rank = 3;
                }


              //Update rank using the Populated Place Class - A numerical scale
              //identifying the relative importance of a populated place. The
              //scale ranges from 1 (high) to 5 (low).
                Integer pc = cint(col[11]); //
                if (pc!=null){
                    if (pc<rank){
                        if (pc==1 && !dsg.equals("PPLC")) pc=2;
                        rank = pc;
                    }
                }


                /*
                //Update rank using population //<-- Population no longer maintained; contains no values
                if (pop!=null && rank>3){
                    if (pop>=150000){
                        rank = 3;
                    }
                    else if (pop<150000 && pop>20000){
                        rank = 4;
                    }
                }
                */


                for (String cc : countryCodes.split(",")){


                  //Save place
                    Place place = new Place();
                    place.setCountryCode(cc);
                    place.setAdmin1(a1);
                    //updateUKTerritories(place);



                    place.setGeom(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
                    place.setRank(rank);
                    place.setSource(source);
                    place.setSourceKey(id);
                    place.setSourceDate(lastUpdate);

                    if (type!=null){
                        place.setType(type[0]);
                        place.setSubtype(type[1]);
                    }


                    JSONObject json = new JSONObject();
                    json.set("population", pop);
                    json.set("dsg", dsg);
                    json.set("fc", fc);
                    json.set("pc", pc);
                    json.set("note", col[27]);
                    json.set("display", minScale);
                    place.setInfo(json);



                    try{
                        place.save();
                    }
                    catch(Exception e){
                        if (e instanceof IllegalStateException){
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



                  //Add place names
                    String name =    col[22]; //Full name
                    String altName = col[23]; //Same as the full name but the character/diacritic combinations and special characters are substituted
                                              //with QWERTY (visible U.S. English keyboard) characters while still maintaining casing and spaces.
                                              //This field also includes non-roman script based names which are stripped of vowel markings.


                    Name placeName = new Name();
                    placeName.setName(name);
                    placeName.setLanguageCode(languageCode);
                    placeName.setType(nameType);
                    placeName.setSource(source);
                    placeName.setSourceKey(id);
                    placeName.setSourceDate(lastUpdate);
                    placeName.setPlace(place);
                    try{
                        placeName.save();
                    }
                    catch(Exception e){
                        if (e instanceof IllegalStateException){
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


                    if (!name.equals(altName)){
                        placeName = new Name();
                        placeName.setName(altName);
                        placeName.setLanguageCode("eng");
                        placeName.setType(nameType);
                        placeName.setSource(source);
                        placeName.setSourceKey(id);
                        placeName.setSourceDate(lastUpdate);
                        placeName.setPlace(place);
                        try{
                            placeName.save();
                        }
                        catch(Exception e){
                            if (e instanceof IllegalStateException){
                                return;
                            }
                            else{ //probably a duplicate
                                String msg = e.getMessage();
                                if (msg.contains("duplicate key")){
                                    update(place);
                                }
                                else{
                                    console.log(e.getMessage());
                                }
                            }
                        }
                    }


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
                    java.sql.ResultSet r2 = placeQuery.executeQuery();
                    while (r2.next()) {
                        placeID = r2.getLong(1);
                        currDate = r2.getInt(2);
                    }
                    r2.close();

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
                        "select id, source_date from gazetter.place where source_id=? and source_key=? and country_code=?"
                    );
                    set("stmt", stmt);
                }
                return stmt;
            }

            public void exit(){
                PreparedStatement stmt = (PreparedStatement) get("stmt");
                if (stmt!=null) try{stmt.close();}catch(Exception e){}

                Connection conn = (Connection) get("conn");
                if (conn!=null) conn.close();
            }

        }.start();


      //Parse file
        BufferedReader br = getBufferedReader(file);
        br.readLine(); //Skip header
        String row;
        while ((row = br.readLine()) != null){
            pool.add(row);
        }
        br.close();

        pool.done();
        pool.join();
    }


  //**************************************************************************
  //** getNameType
  //**************************************************************************
  /** Returns a numeric key for a given a name type.
   *  @param s Name Type (NT). Options include:

    C = Conventional: A commonly used English-language name approved by the US
        Board on Geographic Names (BGN) for use in addition to, or in lieu of,
        a BGN-approved local official name or names, e.g., Rome, Alps, Danube
        River.

    N = Approved: The BGN-approved local official name for a geographic feature.
        Except for countries with more than one official language; there is
        normally only one such name for a feature wholly within a country.

    D = Unverified: A name from a source whose official status cannot be
        verified by the BGN.

    P = Provisional: A geographic name of an area for which the territorial
        status is not finally determined or not recognized by the United States.

    VA = Anglicized variant: An English-language name that is derived by
        modifying the local official name to render it more accessible or
        meaningful to an English-language user.

    V = Variant: A former name, name in local usage, or other spelling found on
        various sources.

    NS = Non-Roman Script: The non-Roman script form of the BGN-approved local
        official name for a geographic feature. Except for countries with more
        than one official language; there is normally only one such name for a
        feature wholly within a country.

    DS = Unverified Non-Roman Script: The non-Roman script form of a name from a
        source whose official status cannot be verified by the BGN.

    VS = Variant Non-Roman Script: The non-Roman script form of a former name,
        name in local usage, or other spelling found on various sources.
   */
    private static int getNameType(String s){

        int type;
        switch (s) {


          //English spellings
            case "C":
                type = 1;
                break;
            case "N":
                type = 2;
                break;
            case "D":
            case "P":
            case "V":
            case "VA":
                type = 3;
                break;


          //Non-Roman Script
            case "NS":
                type = 2;
                break;
            case "DS":
            case "VS":
                type = 3;
                break;


            default:
                throw new IllegalArgumentException("Invalid type: " + s);
        }

        return type;
    }


  //**************************************************************************
  //** updateUKTerritories
  //**************************************************************************
    private static void updateUKTerritories(Place city) {
        String cc = city.getCountryCode();
        if (cc.equals("AX") || cc.equals("DX")){
            city.setCountryCode("UK");
            city.setAdmin1(cc);
        }
    }


  //**************************************************************************
  //** getUpdates
  //**************************************************************************
  /** Used to scrape an NGA website and generate a list of country files that
   *  appear to be newer that what we have in the database.
   *  @return An array of JSON objects with country code, download link, and
   *  date in YYYYMMDD format (e.g. 20120729)
   */
    public static JSONArray getUpdates(Database database) throws SQLException {

        JSONArray arr = new JSONArray();


        Source source = Source.get("name=","NGA");
        LinkedHashMap<String, Integer> recentUpdates = Utils.getRecentUpdates(source, database);



        javaxt.http.Request request = new javaxt.http.Request("http://geonames.nga.mil/gns/html/namefiles.html");
        javaxt.http.Response response = request.getResponse();
        javaxt.html.Parser document = new javaxt.html.Parser(response.getText());
        javaxt.html.Element tbody = document.getElementByTagName("tbody");
        javaxt.html.Element[] rows = tbody.getElementsByTagName("tr");
        for (int i=1; i<rows.length; i++){ //Skip header

            try{
                javaxt.html.Element[] col = rows[i].getElementsByTagName("td");
                String link = col[0].getElementByTagName("a").getAttribute("href");
                String cc = col[1].getInnerText().toUpperCase();
                Integer date = cint(col[2].getInnerText().replace("-", ""));


              //Update country code
                if (cc.equals("AX") || cc.equals("DX")) cc = "UK";


                Integer lastUpdate = recentUpdates.get(cc);
                if (lastUpdate==null || date>lastUpdate){
                    link = javaxt.html.Parser.MapPath(link, request.getURL());
                    System.out.println(date + "\t" + cc + "\t" + link);

                    JSONObject json = new JSONObject();
                    json.set("cc", cc);
                    json.set("date", date);
                    json.set("link", link);
                    arr.add(json);
                }
            }
            catch(Exception e){
                //e.printStackTrace();
            }
        }
        return arr;
    }


  //**************************************************************************
  //** download
  //**************************************************************************
  /** Used to download the "Entire country files dataset" from the NGA website:
   *  http://geonames.nga.mil/gns/html/namefiles.html
   */
    public static File download(Directory downloadDir, boolean unzip) throws Exception {
        java.net.URL url = new java.net.URL("https://geonames.nga.mil/gns/html/namefiles.html");
        javaxt.http.Response response = new javaxt.http.Request(url).getResponse();
        if (response.getStatus()!=200) throw new Exception("Failed to connect to " + url);
        String html = response.getText();

        File file = null;
        for (javaxt.html.Element el : new javaxt.html.Parser(html).getElementsByTagName("a")){
            String text = el.getInnerText();
            if (text.equals("Entire country files dataset")){
                String href = el.getAttribute("href");
                String link = javaxt.html.Parser.MapPath(href, url);
                String path = new javaxt.utils.URL(link).getPath();
                if (path.endsWith("/")) path = path.substring(0, path.length()-1);
                String fileName = path.substring(path.lastIndexOf("/")+1);
                file = new File(downloadDir + "nga/" + fileName);
                if (!file.exists()){
                    response = new javaxt.http.Request(link).getResponse();
                    if (response.getStatus()!=200) throw new Exception("Failed to download " + link);
                    System.out.print("Downloading " + file.getName() + "...");
                    java.io.InputStream is = response.getInputStream();
                    file.write(is);
                    is.close();
                    System.out.println("Done!");
                }
                break;
            }
        }



        if (file==null) throw new Exception("Failed to find file to download");

        if (unzip){
            File countryFile = new File(file.getDirectory(), file.getName(false) + ".txt");
            ZipInputStream zip = new ZipInputStream(file.getInputStream());
            ZipEntry entry;
            while((entry = zip.getNextEntry())!=null){
                String fileName = entry.getName();
                if (fileName.equalsIgnoreCase("Countries.txt")){
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
    private static BufferedReader getBufferedReader(File file) throws Exception {
        if (file.getExtension().equals("zip")){
            ZipFile zipFile = new ZipFile(file.toFile());

            BufferedInputStream bis = new BufferedInputStream(file.getInputStream());
            ZipInputStream zip = new ZipInputStream(bis);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    final String fileName = zipEntry.getName();
                    if (fileName.equalsIgnoreCase("Countries.txt")){
                        InputStream is = zipFile.getInputStream(zipEntry);
                        return new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    }
                }
            }
        }
        else{
            return file.getBufferedReader("UTF-8");
        }
        return null;
    }


  //**************************************************************************
  //** downloadUpdates
  //**************************************************************************
  /** Used to download country updates from NGA.
   *  @return List of files that were downloaded.
   */
    public static ArrayList<File> downloadUpdates(JSONArray updates, Directory downloadDir){
        ArrayList<File> files = new ArrayList<File>();
        for (int i=0; i<updates.length(); i++){
            JSONObject json = updates.get(i).toJSONObject();
            String cc = json.get("cc").toString();
            Integer date = json.get("date").toInteger();
            String link = json.get("link").toString();

            File file = new File(downloadDir, cc + "_" + date +".txt");
            if (!file.exists()){
                javaxt.http.Response response = new javaxt.http.Request(link).getResponse();

                try{
                    ZipInputStream zip = new ZipInputStream(response.getInputStream());
                    ZipEntry entry;
                    while((entry = zip.getNextEntry())!=null){
                        String fileName = entry.getName();
                        if (fileName.equalsIgnoreCase(cc + ".txt")){
                            byte[] buffer = new byte[2048];
                            java.io.FileOutputStream output = file.getOutputStream();
                            int len;
                            while ((len = zip.read(buffer)) > 0){
                                output.write(buffer, 0, len);
                            }
                            output.close();
                            break;
                        }
                    }


                    zip.close();
                }
                catch(Exception e){
                }
            }

            if (file.exists()) files.add(file);
        }
        return files;
    }


  //**************************************************************************
  //** cint
  //**************************************************************************
  /** Used to convert a string to an int.
   */
    private static Integer cint(String str){
        try{
            return Integer.parseInt(str);
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** typeMap
  //**************************************************************************
  /** Used to map NGA DSG codes to a type and subtype
   */
    private static HashMap<String, String[]> typeMap = new HashMap<String, String[]>();
    static {
        typeMap.put("PP", new String[]{"administrative", "police post"});
        typeMap.put("PPQ", new String[]{"administrative", "abandoned police post"});
        typeMap.put("PRKGT", new String[]{"administrative", "park gate"});
        typeMap.put("PRKHQ", new String[]{"administrative", "park headquarters"});
        typeMap.put("PRN", new String[]{"administrative", "prison"});
        typeMap.put("PRNJ", new String[]{"administrative", "reformatory"});
        typeMap.put("PRNQ", new String[]{"administrative", "abandoned prison"});
        typeMap.put("PSTB", new String[]{"administrative", "border post"});
        typeMap.put("PSTC", new String[]{"administrative", "customs post"});
        typeMap.put("PSTP", new String[]{"administrative", "patrol post"});
        typeMap.put("ADMF", new String[]{"administrative", "administrative facility"});
        typeMap.put("CSTM", new String[]{"administrative", "customs house"});
        typeMap.put("CTHSE", new String[]{"administrative", "courthouse"});
        typeMap.put("STNC", new String[]{"administrative", "coast guard station"});
        typeMap.put("STNI", new String[]{"administrative", "inspection station"});
        typeMap.put("USGE", new String[]{"administrative", "United States Government Establishment"});
        typeMap.put("ZNF", new String[]{"administrative", "free trade zone"});
        typeMap.put("DIP", new String[]{"administrative", "diplomatic facility"});
        typeMap.put("GOVL", new String[]{"administrative", "local government office"});
        typeMap.put("TOLL", new String[]{"administrative", "toll gate/barrier"});
        typeMap.put("MKT", new String[]{"amenity", "market"});
        typeMap.put("PO", new String[]{"amenity", "post office"});
        typeMap.put("PRK", new String[]{"amenity", "park"});
        typeMap.put("RECG", new String[]{"amenity", "golf course"});
        typeMap.put("RECR", new String[]{"amenity", "racetrack"});
        typeMap.put("RHSE", new String[]{"amenity", "resthouse"});
        typeMap.put("RLGR", new String[]{"amenity", "retreat"});
        typeMap.put("ASYL", new String[]{"amenity", "asylum"});
        typeMap.put("ATHF", new String[]{"amenity", "athletic field"});
        typeMap.put("BLDG", new String[]{"amenity", "building(s)"});
        typeMap.put("CMN", new String[]{"amenity", "common"});
        typeMap.put("CSNO", new String[]{"amenity", "casino"});
        typeMap.put("CTRB", new String[]{"amenity", "business center"});
        typeMap.put("CTRCM", new String[]{"amenity", "community center"});
        typeMap.put("CTRF", new String[]{"amenity", "facility center"});
        typeMap.put("CTRM", new String[]{"amenity", "medical center"});
        typeMap.put("FCL", new String[]{"amenity", "facility"});
        typeMap.put("RSRT", new String[]{"amenity", "resort"});
        typeMap.put("GDN", new String[]{"amenity", "garden(s)"});
        typeMap.put("GHSE", new String[]{"amenity", "guest house"});
        typeMap.put("HSP", new String[]{"amenity", "hospital"});
        typeMap.put("HSPC", new String[]{"amenity", "clinic"});
        typeMap.put("HSPD", new String[]{"amenity", "dispensary"});
        typeMap.put("HSPL", new String[]{"amenity", "leprosarium"});
        typeMap.put("HTL", new String[]{"amenity", "hotel"});
        typeMap.put("SNTR", new String[]{"amenity", "sanatorium"});
        typeMap.put("SPA", new String[]{"amenity", "spa"});
        typeMap.put("STDM", new String[]{"amenity", "stadium"});
        typeMap.put("VETF", new String[]{"amenity", "veterinary facility"});
        typeMap.put("ZOO", new String[]{"amenity", "zoo"});
        typeMap.put("BAN", new String[]{"amenity", "bank"});
        typeMap.put("BLDO", new String[]{"amenity", "office building"});
        typeMap.put("RET", new String[]{"amenity", "store"});
        typeMap.put("SHOPC", new String[]{"amenity", "shopping center or mall"});
        typeMap.put("MNMT", new String[]{"cultural", "monument"});
        typeMap.put("MUS", new String[]{"cultural", "museum"});
        typeMap.put("OBPT", new String[]{"cultural", "observation point"});
        typeMap.put("PAL", new String[]{"cultural", "palace"});
        typeMap.put("PYR", new String[]{"cultural", "pyramid"});
        typeMap.put("PYRS", new String[]{"cultural", "pyramids"});
        typeMap.put("AMTH", new String[]{"cultural", "amphitheater"});
        typeMap.put("ANS", new String[]{"cultural", "ancient site"});
        typeMap.put("ARCH", new String[]{"cultural", "arch"});
        typeMap.put("BTL", new String[]{"cultural", "battlefield"});
        typeMap.put("BUR", new String[]{"cultural", "burial cave(s)"});
        typeMap.put("CARN", new String[]{"cultural", "cairn"});
        typeMap.put("CSTL", new String[]{"cultural", "castle"});
        typeMap.put("RUIN", new String[]{"cultural", "ruin(s)"});
        typeMap.put("HLT", new String[]{"cultural", "halting place"});
        typeMap.put("HSTS", new String[]{"cultural", "historical site"});
        typeMap.put("SQR", new String[]{"cultural", "square"});
        typeMap.put("OBS", new String[]{"education", "observatory"});
        typeMap.put("OBSR", new String[]{"education", "radio observatory"});
        typeMap.put("CTRA", new String[]{"education", "atomic center"});
        typeMap.put("ITTR", new String[]{"education", "research institute"});
        typeMap.put("SCH", new String[]{"education", "school"});
        typeMap.put("SCHA", new String[]{"education", "agricultural school"});
        typeMap.put("SCHC", new String[]{"education", "college"});
        typeMap.put("SCHN", new String[]{"education", "maritime school"});
        typeMap.put("STNB", new String[]{"education", "scientific research base"});
        typeMap.put("STNE", new String[]{"education", "experiment station"});
        typeMap.put("STNF", new String[]{"education", "forest station"});
        typeMap.put("SCHT", new String[]{"education", "technical school"});
        typeMap.put("ML", new String[]{"industry", "mill(s)"});
        typeMap.put("MLM", new String[]{"industry", "ore treatment plant"});
        typeMap.put("MLO", new String[]{"industry", "olive oil mill"});
        typeMap.put("MLSG", new String[]{"industry", "sugar mill"});
        typeMap.put("MLSGQ", new String[]{"industry", "former sugar mill"});
        typeMap.put("MLSW", new String[]{"industry", "sawmill"});
        typeMap.put("MLWND", new String[]{"industry", "windmill"});
        typeMap.put("MLWTR", new String[]{"industry", "water mill"});
        typeMap.put("MN", new String[]{"industry", "mine(s)"});
        typeMap.put("MNA", new String[]{"industry", "mining area"});
        typeMap.put("MNAU", new String[]{"industry", "gold mine(s)"});
        typeMap.put("MNC", new String[]{"industry", "coal mine(s)"});
        typeMap.put("MNCR", new String[]{"industry", "chrome mine(s)"});
        typeMap.put("MNCU", new String[]{"industry", "copper mine(s)"});
        typeMap.put("MNDT", new String[]{"industry", "diatomite mine(s)"});
        typeMap.put("MNFE", new String[]{"industry", "iron mine(s)"});
        typeMap.put("MNN", new String[]{"industry", "salt mine(s)"});
        typeMap.put("MNNI", new String[]{"industry", "nickel mine(s)"});
        typeMap.put("MNPB", new String[]{"industry", "lead mine(s)"});
        typeMap.put("MNPL", new String[]{"industry", "placer mine(s)"});
        typeMap.put("MNQ", new String[]{"industry", "abandoned mine"});
        typeMap.put("MNQR", new String[]{"industry", "quarry(-ies)"});
        typeMap.put("MNSN", new String[]{"industry", "tin mine(s)"});
        typeMap.put("NSY", new String[]{"industry", "nursery(-ies)"});
        typeMap.put("OILF", new String[]{"industry", "oilfield"});
        typeMap.put("OILJ", new String[]{"industry", "oil pipeline junction"});
        typeMap.put("OILQ", new String[]{"industry", "abandoned oil well"});
        typeMap.put("OILR", new String[]{"industry", "oil refinery"});
        typeMap.put("OILT", new String[]{"industry", "tank farm"});
        typeMap.put("OILW", new String[]{"industry", "oil well"});
        typeMap.put("PEAT", new String[]{"industry", "peat cutting area"});
        typeMap.put("PMPO", new String[]{"industry", "oil pumping station"});
        typeMap.put("RNCH", new String[]{"industry", "ranch(es)"});
        typeMap.put("AGRF", new String[]{"industry", "agricultural facility"});
        typeMap.put("BSTN", new String[]{"industry", "baling station"});
        typeMap.put("COLF", new String[]{"industry", "coalfield"});
        typeMap.put("CRRL", new String[]{"industry", "corral(s)"});
        typeMap.put("CTRS", new String[]{"industry", "space center"});
        typeMap.put("DARY", new String[]{"industry", "dairy"});
        typeMap.put("EST", new String[]{"industry", "estate(s)"});
        typeMap.put("ESTB", new String[]{"industry", "banana plantation"});
        typeMap.put("ESTC", new String[]{"industry", "cotton plantation"});
        typeMap.put("ESTO", new String[]{"industry", "oil palm plantation"});
        typeMap.put("ESTR", new String[]{"industry", "rubber plantation"});
        typeMap.put("ESTSG", new String[]{"industry", "sugar plantation"});
        typeMap.put("ESTSL", new String[]{"industry", "sisal plantation"});
        typeMap.put("ESTT", new String[]{"industry", "tea plantation"});
        typeMap.put("ESTX", new String[]{"industry", "section of estate"});
        typeMap.put("FNDY", new String[]{"industry", "foundry"});
        typeMap.put("FRM", new String[]{"industry", "farm"});
        typeMap.put("FRMQ", new String[]{"industry", "abandoned farm"});
        typeMap.put("FRMS", new String[]{"industry", "farms"});
        typeMap.put("FRMT", new String[]{"industry", "farmstead"});
        typeMap.put("GASF", new String[]{"industry", "gasfield"});
        typeMap.put("GOSP", new String[]{"industry", "gas-oil separator plant"});
        typeMap.put("MFG", new String[]{"industry", "factory"});
        typeMap.put("MFGB", new String[]{"industry", "brewery"});
        typeMap.put("MFGC", new String[]{"industry", "cannery"});
        typeMap.put("MFGCU", new String[]{"industry", "copper works"});
        typeMap.put("MFGLM", new String[]{"industry", "limekiln"});
        typeMap.put("MFGPH", new String[]{"industry", "phosphate works"});
        typeMap.put("MFGQ", new String[]{"industry", "abandoned factory"});
        typeMap.put("MFGSG", new String[]{"industry", "sugar refinery"});
        typeMap.put("SHPF", new String[]{"industry", "sheepfold"});
        typeMap.put("SHSE", new String[]{"industry", "storehouse"});
        typeMap.put("STBL", new String[]{"industry", "stable"});
        typeMap.put("STNW", new String[]{"industry", "whaling station"});
        typeMap.put("TNKD", new String[]{"industry", "cattle dipping tank"});
        typeMap.put("AQC", new String[]{"industry", "aquaculture facility"});
        typeMap.put("PMPW", new String[]{"infrastructure", "water pumping station"});
        typeMap.put("PS", new String[]{"infrastructure", "power station"});
        typeMap.put("PSH", new String[]{"infrastructure", "hydroelectric power station"});
        typeMap.put("COMC", new String[]{"infrastructure", "communication center"});
        typeMap.put("DAM", new String[]{"infrastructure", "dam"});
        typeMap.put("DAMQ", new String[]{"infrastructure", "ruined dam"});
        typeMap.put("DAMSB", new String[]{"infrastructure", "sub-surface dam"});
        typeMap.put("DPOF", new String[]{"infrastructure", "fuel depot"});
        typeMap.put("STNM", new String[]{"infrastructure", "meteorological station"});
        typeMap.put("STNR", new String[]{"infrastructure", "radio station"});
        typeMap.put("STNS", new String[]{"infrastructure", "satellite station"});
        typeMap.put("TOWR", new String[]{"infrastructure", "tower"});
        typeMap.put("TRMO", new String[]{"infrastructure", "oil pipeline terminal"});
        typeMap.put("WTRW", new String[]{"infrastructure", "waterworks"});
        typeMap.put("FIRE", new String[]{"infrastructure", "fire station"});
        typeMap.put("LNDF", new String[]{"infrastructure", "landfill"});
        typeMap.put("PSN", new String[]{"infrastructure", "nuclear power station"});
        typeMap.put("STMGS", new String[]{"infrastructure", "stream gauging station"});
        typeMap.put("SWT", new String[]{"infrastructure", "sewage treatment plant"});
        typeMap.put("MVA", new String[]{"military", "maneuver area"});
        typeMap.put("NVB", new String[]{"military", "naval base"});
        typeMap.put("RNGA", new String[]{"military", "artillery range"});
        typeMap.put("AIRB", new String[]{"military", "airbase"});
        typeMap.put("BRKS", new String[]{"military", "barracks"});
        typeMap.put("FT", new String[]{"military", "fort"});
        typeMap.put("INSM", new String[]{"military", "military installation"});
        typeMap.put("MFGM", new String[]{"military", "munitions plant"});
        typeMap.put("MILB", new String[]{"military", "military base"});
        typeMap.put("SCHM", new String[]{"military", "military school"});
        typeMap.put("CMP", new String[]{"populated place", "camp(s)"});
        typeMap.put("CMPL", new String[]{"populated place", "logging camp"});
        typeMap.put("CMPLA", new String[]{"populated place", "labor camp"});
        typeMap.put("CMPMN", new String[]{"populated place", "mining camp"});
        typeMap.put("CMPO", new String[]{"populated place", "oil camp"});
        typeMap.put("CMPQ", new String[]{"populated place", "abandoned camp"});
        typeMap.put("CMPRF", new String[]{"populated place", "refugee camp"});
        typeMap.put("MSQE", new String[]{"religious", "mosque"});
        typeMap.put("MSSN", new String[]{"religious", "mission"});
        typeMap.put("MSSNQ", new String[]{"religious", "abandoned mission"});
        typeMap.put("MSTY", new String[]{"religious", "monastery"});
        typeMap.put("NOV", new String[]{"religious", "novitiate"});
        typeMap.put("PGDA", new String[]{"religious", "pagoda"});
        typeMap.put("PPLR", new String[]{"religious", "religious populated place"});
        typeMap.put("RLG", new String[]{"religious", "religious site"});
        typeMap.put("CH", new String[]{"religious", "church"});
        typeMap.put("CMTY", new String[]{"religious", "cemetery"});
        typeMap.put("CTRR", new String[]{"religious", "religious center"});
        typeMap.put("CVNT", new String[]{"religious", "convent"});
        typeMap.put("GRVE", new String[]{"religious", "grave"});
        typeMap.put("HERM", new String[]{"religious", "hermitage"});
        typeMap.put("SHRN", new String[]{"religious", "shrine"});
        typeMap.put("TMB", new String[]{"religious", "tomb(s)"});
        typeMap.put("TMPL", new String[]{"religious", "temple(s)"});
        typeMap.put("GHAT", new String[]{"religious", "ghāt"});
        typeMap.put("PPL", new String[]{"residential", "populated place"});
        typeMap.put("PPLA", new String[]{"residential", "seat of a first-order administrative division"});
        typeMap.put("PPLC", new String[]{"residential", "capital of a political entity"});
        typeMap.put("PPLL", new String[]{"residential", "populated locality"});
        typeMap.put("PPLQ", new String[]{"residential", "abandoned populated place"});
        typeMap.put("PPLS", new String[]{"residential", "populated places"});
        typeMap.put("PPLW", new String[]{"residential", "destroyed populated place"});
        typeMap.put("PPLX", new String[]{"residential", "section of populated place"});
        typeMap.put("DEVH", new String[]{"residential", "housing development"});
        typeMap.put("HSE", new String[]{"residential", "house(s)"});
        typeMap.put("HSEC", new String[]{"residential", "country house"});
        typeMap.put("HUT", new String[]{"residential", "hut"});
        typeMap.put("HUTS", new String[]{"residential", "huts"});
        typeMap.put("LEPC", new String[]{"residential", "leper colony"});
        typeMap.put("STLMT", new String[]{"residential", "Israeli settlement"});
        typeMap.put("PPLA2", new String[]{"residential", "seat of a second-order administrative division"});
        typeMap.put("PPLA3", new String[]{"residential", "seat of a third-order administrative division"});
        typeMap.put("PPLA4", new String[]{"residential", "seat of a fourth-order administrative division"});
        typeMap.put("BLDA", new String[]{"residential", "apartment building"});
        typeMap.put("PPLF", new String[]{"residential", "farm village"});
        typeMap.put("PIER", new String[]{"transportation", "pier"});
        typeMap.put("PKLT", new String[]{"transportation", "parking lot"});
        typeMap.put("PRT", new String[]{"transportation", "port"});
        typeMap.put("AIRF", new String[]{"transportation", "airfield"});
        typeMap.put("AIRH", new String[]{"transportation", "heliport"});
        typeMap.put("AIRP", new String[]{"transportation", "airport"});
        typeMap.put("AIRQ", new String[]{"transportation", "abandoned airfield"});
        typeMap.put("BDG", new String[]{"transportation", "bridge"});
        typeMap.put("BTYD", new String[]{"transportation", "boatyard"});
        typeMap.put("DCKD", new String[]{"transportation", "dry dock"});
        typeMap.put("DCKY", new String[]{"transportation", "dockyard"});
        typeMap.put("RSTN", new String[]{"transportation", "railroad station"});
        typeMap.put("RSTNQ", new String[]{"transportation", "abandoned railroad station"});
        typeMap.put("RSTP", new String[]{"transportation", "railroad stop"});
        typeMap.put("RSTPQ", new String[]{"transportation", "abandoned railroad stop"});
        typeMap.put("FY", new String[]{"transportation", "ferry"});
        typeMap.put("LDNG", new String[]{"transportation", "landing"});
        typeMap.put("LOCK", new String[]{"transportation", "lock(s)"});
        typeMap.put("LTHSE", new String[]{"transportation", "lighthouse"});
        typeMap.put("MAR", new String[]{"transportation", "marina"});
        typeMap.put("STPS", new String[]{"transportation", "steps"});
        typeMap.put("AIRG", new String[]{"transportation", "hangar"});
        typeMap.put("AIRT", new String[]{"transportation", "terminal"});
        typeMap.put("FYT", new String[]{"transportation", "ferry terminal"});
        typeMap.put("GARG", new String[]{"transportation", "parking garage"});
        typeMap.put("RDCR", new String[]{"transportation", "traffic circle"});
        typeMap.put("RDIN", new String[]{"transportation", "intersection"});
        typeMap.put("SUBS", new String[]{"transportation", "subway station"});
        typeMap.put("SUBW", new String[]{"transportation", "subway"});
        typeMap.put("TRANT", new String[]{"transportation", "transit terminal"});
    }
}