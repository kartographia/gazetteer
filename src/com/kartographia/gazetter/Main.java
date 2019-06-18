package com.kartographia.gazetter;
import com.kartographia.gazetter.source.*;
import com.kartographia.gazetter.web.WebApp;
import javaxt.json.JSONObject;
import javaxt.utils.Console;
import javaxt.io.Jar;
import javaxt.sql.*;
import java.util.*;


//******************************************************************************
//**  Main
//******************************************************************************
/**
 *   Command line interface used to start the web server or to run specialized
 *   functions (e.g. ingest, maintenance scripts, tests, etc).
 *
 ******************************************************************************/

public class Main {

    public static final String UTF8_BOM = "\uFEFF";


  //**************************************************************************
  //** Main
  //**************************************************************************
  /** Entry point for the application.
   */
    public static void main(String[] arr) throws Exception {
        java.util.HashMap<String, String> args = Console.parseArgs(arr);


      //Get jar file
        Jar jar = new Jar(Main.class);
        javaxt.io.File jarFile = new javaxt.io.File(jar.getFile());


      //Get config file
        javaxt.io.File configFile = (args.containsKey("-config")) ?
            Config.getFile(args.get("-config"), jarFile) :
            new javaxt.io.File(jar.getFile().getParentFile(), "config.json");

        if (!configFile.exists()) {
            System.out.println("Could not find config file. Use the \"-config\" parameter to specify a path to a config");
            return;
        }


      //Read config file (json)
        JSONObject config = new JSONObject(configFile.getText());


      //Update relative paths to schema
        JSONObject schema = config.get("schema").toJSONObject();
        JSONObject gazetteer = schema.get("gazetter").toJSONObject();
        Config.updateFile("path", gazetteer, configFile);
        Config.updateFile("updates", gazetteer, configFile);
        schema.set("gazetter", gazetteer);


      //Initialize config
        Config.init(config, jar);
        Database database = Config.getDatabase();




      //Process command line args
        if (args.containsKey("-import")){


            javaxt.io.File file = new javaxt.io.File(args.get("-import"));
            if (file.exists()){

                if (file.getExtension().equals("shp")){
                    System.out.println(
                    "ERROR: Please specify a name of a source (e.g. \"VLIZ\") " +
                    "using the -import argument and use -path to specify a path " +
                    "to the shapefile");
                }
                else if (file.getExtension().equals("dictionary")){
                    updateCountryNames(file);
                }
                else{
                    java.io.BufferedReader br = null;
                    try{
                        long startTime = System.currentTimeMillis();

                        br = file.getBufferedReader("UTF-8");
                        String header = br.readLine();
                        if (header.startsWith(UTF8_BOM)) {
                            header = header.substring(1);
                        }
                        br.close();


                        if (header.startsWith("FEATURE_ID|")){
                            USGS.load(file, database);
                        }
                        else if (header.startsWith("RC\t")){
                            NGA.load(file, database);
                        }
                        else if (header.startsWith("USPS\t")){
                            USCensus.load(file, database);
                        }
                        else{
                            throw new Exception("Unknown file type");
                        }

                        System.out.println();
                        System.out.println("Ellapsed Time: " + Utils.getElapsedTime(startTime));
                    }
                    catch(Exception e){
                        try{ br.close(); } catch(Exception ex){}
                        e.printStackTrace();
                    }
                }
            }
            else{

                String source = args.get("-import");
                if (source==null) throw new Exception("Source is required");

                String path = args.get("-path");
                file = new javaxt.io.File(path);
                if (file.exists()){

                    if (source.equalsIgnoreCase("NGA")){
                        NGA.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("USGS")){
                        USGS.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("Census")){
                        USCensus.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("VMAP")){
                        VMAP.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("VLIZ")){
                        VLIZ.load(file, database);
                    }

                }
                else{
                    throw new Exception("Path is required");
                }
            }

        }
        else if (args.containsKey("-download")){

        }
        else if (args.containsKey("-update")){

        }
        else{
            try{
                if (!config.has("webserver")){
                    throw new Exception("Config file is missing \"webserver\" config information");
                }
                else{
                    JSONObject webConfig = config.get("webserver").toJSONObject();
                    Config.updateDir("webDir", webConfig, configFile);
                    Config.updateDir("logDir", webConfig, configFile);
                    Config.updateFile("keystore", webConfig, configFile);

                    new WebApp(config.get("webserver").toJSONObject(), database);
                    //SyncService.start();
                }
            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }


  //**************************************************************************
  //** updateCountryNames
  //**************************************************************************
  /** Used to update country names by adding records from the CountryNames
   *  dictionary developed for the TEX app.
   */
    private static void updateCountryNames(javaxt.io.File file) throws Exception {


      //Parse file and generate list of country names, grouped by country code
        HashMap<String, ArrayList<String>> countryNames = new HashMap<>();
        java.io.BufferedReader br = file.getBufferedReader("UTF-8");
        String row;
        while ((row = br.readLine()) != null){
            if (row.startsWith("#")) continue;

            String[] arr = row.split(";");
            String name = arr[0].replace("\"", "").trim();
            String cc = arr[1];

            ArrayList<String> names = countryNames.get(cc);
            if (names==null) {
                names = new ArrayList<String>();
                countryNames.put(cc, names);
            }
            names.add(name);
        }


      //Iterate through the list of names and update database as needed
        Source source = null;
        Iterator<String> it = countryNames.keySet().iterator();
        while (it.hasNext()){
            String cc = it.next();
            ArrayList<String> names = countryNames.get(cc);
            Place place = Place.get("type=","boundary","subtype=","country","country_code=", cc);
            if (place==null){
                System.out.println(
                    "Missing entry for " + cc + " in the database. " +
                    names.size() + " names will be skipped.");
            }
            else{

                boolean updatePlace = false;
                for (String name : names){
                    boolean addName = true;
                    for (Name placeName : place.getNames()){
                        if (placeName.getLanguageCode().equals("eng")){
                            if (placeName.getName().equalsIgnoreCase(name)){
                                addName = false;
                            }
                        }
                    }
                    if (addName){
                        Name placeName = new Name();
                        placeName.setName(name);
                        placeName.setLanguageCode("eng");
                        placeName.setType(3); //varient
                        if (source==null) source = Source.get("name=","PB");
                        placeName.setSource(source);
                        place.addName(placeName);
                        updatePlace = true;
                    }
                }

                if (updatePlace){
                    place.save();
                }
            }
        }
    }
}