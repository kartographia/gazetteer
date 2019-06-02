package com.kartographia.gazetter;
import com.kartographia.gazetter.source.*;
import com.kartographia.gazetter.web.WebApp;
import javaxt.json.JSONObject;
import javaxt.utils.Console;
import javaxt.io.Jar;
import javaxt.sql.*;


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
            getFile(args.get("-config"), jarFile) :
            new javaxt.io.File(jar.getFile().getParentFile(), "config.json");

        if (!configFile.exists()) {
            System.out.println("Could not find config file. Use the \"-config\" parameter to specify a path to a config");
            return;
        }


      //Initialize config
        JSONObject config = new JSONObject(configFile.getText());
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
                    updateDir("webDir", webConfig, configFile);
                    updateDir("logDir", webConfig, configFile);
                    updateFile("keystore", webConfig, configFile);

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
  //** getFile
  //**************************************************************************
  /** Returns a File for a given path
   *  @param path Full canonical path to a file or a relative path (relative
   *  to the jarFile)
   */
    private static javaxt.io.File getFile(String path, javaxt.io.File jarFile){
        javaxt.io.File file = new javaxt.io.File(path);
        if (!file.exists()){
            file = new javaxt.io.File(jarFile.MapPath(path));
        }
        return file;
    }


  //**************************************************************************
  //** updateDir
  //**************************************************************************
  /** Used to update a path to a directory defined in a config file. Resolves
   *  both canonical and relative paths (relative to the configFile).
   */
    private static void updateDir(String key, JSONObject config, javaxt.io.File configFile){
        if (config.has(key)){
            String path = config.get(key).toString();
            if (path==null){
                config.remove(key);
            }
            else{
                path = path.trim();
                if (path.length()==0){
                    config.remove(key);
                }
                else{

                    javaxt.io.Directory dir = new javaxt.io.Directory(path);
                    if (!dir.exists()) dir = new javaxt.io.Directory(configFile.MapPath(path));

                    if (dir.exists()){
                        config.set(key, dir.toString());
                    }
                    else{
                        config.remove(key);
                    }
                }
            }
        }
    }

  //**************************************************************************
  //** updateFile
  //**************************************************************************
  /** Used to update a path to a file defined in a config file. Resolves
   *  both canonical and relative paths (relative to the configFile).
   */
    private static void updateFile(String key, JSONObject config, javaxt.io.File configFile){
        if (config.has(key)){
            String path = config.get(key).toString();
            if (path==null){
                config.remove(key);
            }
            else{
                path = path.trim();
                if (path.length()==0){
                    config.remove(key);
                }
                else{

                    javaxt.io.File file = new javaxt.io.File(path);
                    if (!file.exists()) file = new javaxt.io.File(configFile.MapPath(path));

                    if (file.exists()){
                        config.set(key, file.toString());
                    }
                    else{
                        config.remove(key);
                    }
                }
            }
        }
    }

}