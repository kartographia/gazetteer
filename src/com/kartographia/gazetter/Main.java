package com.kartographia.gazetter;
import com.kartographia.gazetter.source.*;
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
                java.io.BufferedReader br = null;

                long startTime = System.currentTimeMillis();

                try{

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


                }
                catch(Exception e){
                    try{ br.close(); } catch(Exception ex){}
                    e.printStackTrace();
                }
                System.out.println();

                System.out.println("Ellapsed Time: " + Utils.getElapsedTime(startTime));
            }
            else{

                String path = args.get("-path");
                file = new javaxt.io.File(path);
                if (file.exists()){
                    String source = args.get("-import");
                    if (source==null) throw new Exception("Source is required");
                    if (source.equalsIgnoreCase("NGA")){
                        NGA.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("USGS")){
                        USGS.load(file, database);
                    }
                    else if (source.equalsIgnoreCase("Census")){
                        USCensus.load(file, database);
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
            //start server
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

}