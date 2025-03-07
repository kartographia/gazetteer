package com.kartographia.gazetteer;
import com.kartographia.gazetteer.source.*;
import com.kartographia.gazetteer.data.CountryCodes;
import com.kartographia.gazetteer.data.CountryNames;

import java.util.*;

import javaxt.io.*;
import javaxt.sql.*;
import javaxt.express.*;
import static javaxt.utils.Console.console;
import static javaxt.express.utils.StringUtils.*;


//******************************************************************************
//**  Main
//******************************************************************************
/**
 *   Command line interface used to run specialized functions (e.g. download,
 *   ingest, maintenance scripts, tests, etc).
 *
 ******************************************************************************/

public class Main {

    public static final String UTF8_BOM = "\uFEFF";


  //**************************************************************************
  //** Main
  //**************************************************************************
  /** Entry point for the application.
   */
    public static void main(String[] arguments) throws Exception {
        HashMap<String, String> args = console.parseArgs(arguments);


      //Get jar file
        Jar jar = new Jar(Main.class);
        File jarFile = new File(jar.getFile());


      //Get config file
        File configFile = (args.containsKey("-config")) ?
            ConfigFile.getFile(args.get("-config"), jarFile) :
            new File(jar.getFile().getParentFile(), "config.json");

        if (!configFile.exists()){
            javaxt.io.Directory dir = jarFile.getDirectory();
            if (dir.getName().equals("dist") || dir.getName().equals("target")) {
                configFile = new javaxt.io.File(dir.getParentDirectory(), "config.json");
            }
        }

        if (!configFile.exists()) {
            System.out.println("Could not find config file. " +
            "Use the \"-config\" parameter to specify a path to a config");
            return;
        }


      //Initialize config
        Config.load(configFile);




      //Process command line args
        try{
            if (args.containsKey("-load") || args.containsKey("-import")){
                loadData(args);
            }
            else if (args.containsKey("-download")){
                download(args);
            }
            else if (args.containsKey("-update")){

            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


  //**************************************************************************
  //** loadData
  //**************************************************************************
    private static void loadData(HashMap<String, String> args) throws Exception {
        
        File file = new File(args.containsKey("-import") ?
             args.get("-import") : args.get("-load")
        );
        if (!file.exists()) throw new Exception("Invalid file");


        Database database = Config.getDatabase();

        Integer numThreads = console.getValue(args, "-threads", "-t").toInteger();
        if (numThreads==null) numThreads = 12;


        String source = args.get("-source");
        if (source==null){

            if (file.getExtension().equals("shp")){
                throw new Exception(
                "ERROR: Please specify a name of a source (e.g. \"VLIZ\") " +
                "using the -import argument and use -path to specify a path " +
                "to the shapefile");
            }
            else if (file.getExtension().equals("dictionary")){
                updateCountryNames(file);
            }
            else{

                try (java.io.BufferedReader br = file.getBufferedReader("UTF-8")){
                    long startTime = System.currentTimeMillis();

                    String header = br.readLine().toUpperCase();
                    if (header.startsWith(UTF8_BOM)) {
                        header = header.substring(1);
                    }
                    br.close();


                    if (header.startsWith("FEATURE_ID|")){
                        USGS.load(file, numThreads, database);
                    }
                    else if (header.startsWith("RC\t")){
                        CountryCodes countryCodes = new CountryCodes(Config.getData("countries/countries.csv"));
                        NGA.load(file, countryCodes, numThreads, database);
                    }
                    else if (header.startsWith("USPS\t")){
                        USCensus.load(file, database);
                    }
                    else{
                        throw new Exception("Unknown file type");
                    }

                    System.out.println();
                    System.out.println("Ellapsed Time: " + getElapsedTime(startTime));
                }
            }

        }
        else{
            if (source.equalsIgnoreCase("NGA")){
                CountryCodes countries = new CountryCodes(Config.getData("countries/countries.csv"));
                NGA.load(file, countries, numThreads, database);
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
                CountryCodes countryCodes = new CountryCodes(Config.getData("countries/countries.csv"));
                CountryNames countryNames = new CountryNames(Config.getData("countries/countries.txt"));
                NaturalEarth.load(file, countryCodes, countryNames, database);
            }
            else if (source.equalsIgnoreCase("Kartographia")){
                CountryNames countryNames = new CountryNames(file);
                CountryCodes countryCodes = new CountryCodes(Config.getData("countries/countries.csv"));
                Kartographia.load(countryNames, countryCodes, database);
            }
            else{
                throw new Exception("Invalid -source argument. Given \"" + source + "\".");
            }
        }
    }


  //**************************************************************************
  //** download
  //**************************************************************************
  /** Used to download and load data
   */
    private static void download(HashMap<String, String> args) throws Exception {
        Database database = Config.getDatabase();

        String source = args.get("-download");
        if (source==null) throw new Exception("Invalid -source argument. Source is required.");

        Directory downloadDir = new Directory(Config.get("downloads").toString());
        if (!downloadDir.exists()) downloadDir.create();
        if (!downloadDir.exists()) throw new Exception("Invalid download directory specified in the config file");


        Integer numThreads = console.getValue(args, "-threads", "-t").toInteger();
        if (numThreads==null) numThreads = 12;

        boolean loadData = (args.containsKey("-load") || args.containsKey("-import"));

        if (source.equalsIgnoreCase("NGA")){
            File file = NGA.download(downloadDir, false);
            if (loadData){
                CountryCodes countryCodes = new CountryCodes(Config.getData("countries/countries.csv"));
                NGA.load(file, countryCodes, numThreads, database);
            }
        }
        else if (source.equalsIgnoreCase("USGS")){
            File file = USGS.download(downloadDir, false);
            if (loadData){
                USGS.load(file, numThreads, database);
            }
        }
        else if (source.equalsIgnoreCase("NaturalEarth")){
            File file = NaturalEarth.download(downloadDir, false);
            if (loadData){
                CountryCodes countryCodes = new CountryCodes(Config.getData("countries/countries.csv"));
                CountryNames countryNames = new CountryNames(Config.getData("countries/countries.txt"));
                NaturalEarth.load(file, countryCodes, countryNames, database);
            }
        }
        else{
            throw new Exception("Invalid -source argument. Given \"" + source + "\".");
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