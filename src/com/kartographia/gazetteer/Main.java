package com.kartographia.gazetteer;
import com.kartographia.gazetteer.utils.*;
import com.kartographia.gazetteer.source.*;

import java.util.*;

import javaxt.io.*;
import javaxt.sql.*;
import static javaxt.utils.Console.console;



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
        HashMap<String, String> args = console.parseArgs(arr);


      //Get jar file
        Jar jar = new Jar(Main.class);
        File jarFile = new File(jar.getFile());


      //Get config file
        File configFile = (args.containsKey("-config")) ?
            Config.getFile(args.get("-config"), jarFile) :
            new File(jar.getFile().getParentFile(), "config.json");

        if (!configFile.exists()) {
            System.out.println("Could not find config file. Use the \"-config\" parameter to specify a path to a config");
            return;
        }


      //Initialize config
        Config.load(configFile, jar);




      //Process command line args
        if (args.containsKey("-import")){
            Config.initDatabase();
            Database database = Config.getDatabase();

            int numThreads = 12;
            try{numThreads = Integer.parseInt(args.get("-t"));}catch(Exception e){}

            File file = new File(args.get("-import"));
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
                            USGS.load(file, numThreads, database);
                        }
                        else if (header.startsWith("RC\t")){
                            NGA.load(file, numThreads, database);
                        }
                        else if (header.startsWith("USPS\t")){
                            USCensus.load(file, database);
                        }
                        else{
                            throw new Exception("Unknown file type");
                        }

                        System.out.println();
                        System.out.println("Ellapsed Time: " + StringUtils.getElapsedTime(startTime));
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
                file = new File(path);
                if (file.exists()){

                    if (source.equalsIgnoreCase("NGA")){
                        NGA.load(file, numThreads, database);
                    }
                    else if (source.equalsIgnoreCase("USGS")){
                        USGS.load(file, numThreads, database);
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
                    else if (source.equalsIgnoreCase("NaturalEarth")){
                        NaturalEarth.load(file, database);
                    }
                }
                else{
                    throw new Exception("Path is required");
                }
            }

        }
        else if (args.containsKey("-download")){
            Config.initDatabase();
            Database database = Config.getDatabase();

            String source = args.get("-download");
            if (source==null) throw new Exception("Source is required");

            Directory downloadDir = new Directory(Config.get("downloads").toString());
            if (!downloadDir.exists()) downloadDir.create();
            if (!downloadDir.exists()) throw new Exception("Invalid download directory specified in the config file");


            int numThreads = 12;
            try{numThreads = Integer.parseInt(args.get("-t"));}catch(Exception e){}


            if (source.equalsIgnoreCase("NGA")){
                File file = NGA.download(downloadDir, false);
                NGA.load(file, numThreads, database);
            }
            else if (source.equalsIgnoreCase("USGS")){
                File file = USGS.download(downloadDir, false);
                USGS.load(file, numThreads, database);
            }
            else if (source.equalsIgnoreCase("NaturalEarth")){
                File file = NaturalEarth.download(downloadDir, false);
                NaturalEarth.load(file, database);
            }

        }
        else if (args.containsKey("-update")){

        }
    }


  //**************************************************************************
  //** updateCountryNames
  //**************************************************************************
  /** Used to update country names by adding records from the CountryNames
   *  dictionary developed for the TEX app.
   */
    private static void updateCountryNames(File file) throws Exception {


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


//                    for (Name placeName : place.getNames()){
//                        if (placeName.getLanguageCode().equals("eng")){
//                            if (placeName.getName().equalsIgnoreCase(name)){
//                                addName = false;
//                            }
//                        }
//                    }
//                    if (addName){
//                        Name placeName = new Name();
//                        placeName.setName(name);
//                        placeName.setLanguageCode("eng");
//                        placeName.setType(3); //varient
//                        if (source==null) source = Source.get("name=","PB");
//                        placeName.setSource(source);
//                        place.addName(placeName);
//                        updatePlace = true;
//                    }
                }

                if (updatePlace){
                    place.save();
                }
            }
        }
    }
}