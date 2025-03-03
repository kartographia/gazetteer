package com.kartographia.gazetteer.source;

import com.kartographia.gazetteer.*;
import com.kartographia.gazetteer.data.*;
import com.kartographia.gazetteer.utils.Counter;


import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import javaxt.io.Directory;
import javaxt.io.File;
import javaxt.json.JSONObject;
import javaxt.sql.Database;
import static javaxt.utils.Console.console;

import openmap.Field;
import openmap.Record;
import openmap.ShapeFile;

import org.locationtech.jts.geom.*;


//******************************************************************************
//**  NaturalEarth
//******************************************************************************
/**
 *  Used to parse and load shapefiles data from NaturalEarth
 *
 ******************************************************************************/

public class NaturalEarth {


    private static Source source;

    private static void init(){
        if (source==null)
        try{
            String name = "NaturalEarth";
            source = Source.get("name=",name);
            if (source==null){
                source = new Source();
                source.setName(name);
                source.save();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to parse and load country boundaries data into a database.
   *  @param file Shapefile (*.shp)
   *  @param countries Instance of the Countries class. Used in conjunction
   *  with CountryNames to find an ISO 2 country code for a country name in
   *  cases where NaturalEarth doesn't have a ISO 2 code.
   *  @param countryNames Instance of the CountryNames class. Used in
   *  conjunction with Countries yo find ISO 2 country codes (see above).
   *  @param database Database connection info
   */
    public static void load(File file, Countries countries, CountryNames countryNames,
        Database database) throws Exception {

        init();

        if (file.getExtension().equals("zip")){
            file = unzip(file);
        }


        ShapeFile shp = new ShapeFile(file.toFile());
        System.out.println("Found " + shp.getRecordCount() + " records in the shapefile");
        Counter counter = new Counter(shp.getRecordCount());

        boolean isValidated = false;

        Iterator<Record> it = shp.getRecords();
        while (it.hasNext()){
            Record record = it.next();
            counter.updateCount();

            if (!isValidated){
                HashSet<String> colNames = new HashSet<>();
                for (Field field : record.getFields()){
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.endsWith("*")) fieldName = fieldName.substring(0, fieldName.length()-1);
                    colNames.add(fieldName);
                }
                if (!colNames.contains("name") || !colNames.contains("iso_a2") || !colNames.contains("geom") ){
                    throw new Exception();
                }
                isValidated = true;
            }


            String name = record.getValue("name").toString();
            String formalName = record.getValue("formal_en").toString();
            if (formalName.equals(name)) formalName = null;
            String cc = record.getValue("iso_a2").toString();
            if (cc==null || cc.length()>2) cc = "-99";
            if (cc.equals("-99")) cc = record.getValue("iso_a2_eh").toString();
            if (cc.equals("-99")){

              //Fuzzy match country name to country code
                Map<String, Integer> countryCodes = new HashMap<>();
                countryNames.extractCountries(name, countryCodes);
                if (!countryCodes.isEmpty()){
                    String fips = countryCodes.keySet().iterator().next();
                    cc = countries.getISOCode(fips);
                }
            }
            if (cc.equals("-99")){
                System.out.println("Skipping " + name + "...");
                continue;
            }
            Geometry geom = record.getValue("geom").toGeometry();
            String type = "boundary";
            String subtype = "country";
            Place place = Place.get("country_code=", cc, "type=", type, "subtype=", subtype);


            if (place!=null){
                //TODO: check overlap for XX?
                //System.out.println("Skipping " + cc + "...");
                continue;
            }


          //Save country
            place = new Place();
            place.setCountryCode(cc);
            place.setGeom(geom);
            place.setSource(source);
            place.setType(type);
            place.setSubtype(subtype);

            JSONObject json = new JSONObject();
            json.set("source", file.getName());
            place.setInfo(json);


            try{
                place.save();

                Name placeName = new Name();
                placeName.setName(name);
                placeName.setUname(name.toUpperCase(Locale.US));
                placeName.setLanguageCode("eng");
                placeName.setType(formalName==null ? 2 : 3); //2=formal
                placeName.setSource(source);
                placeName.setPlace(place);
                placeName.save();


                if (formalName!=null){
                    placeName = new Name();
                    placeName.setName(formalName);
                    placeName.setUname(formalName.toUpperCase(Locale.US));
                    placeName.setLanguageCode("eng");
                    placeName.setType(2); //2=formal
                    placeName.setSource(source);
                    placeName.setPlace(place);
                    placeName.save();
                }

            }
            catch(Exception e){
                System.out.println("Error saving \"" + name + "\" (" + cc + ")");
                e.printStackTrace();
            }
        }

        counter.stop();
    }


  //**************************************************************************
  //** download
  //**************************************************************************
  /** Used to download "Entire country files dataset" from the NGA website:
   *  https://www.naturalearthdata.com/downloads/10m-cultural-vectors/
   */
    public static File download(Directory downloadDir, boolean unzip) throws Exception {


        java.net.URL url = new java.net.URL("https://www.naturalearthdata.com/downloads/10m-cultural-vectors/");
        javaxt.http.Response response = new javaxt.http.Request(url).getResponse();
        if (response.getStatus()!=200) throw new Exception("Failed to connect to " + url);
        String html = response.getText();

        File file = null;
        for (String div : html.split("<div class=\"download-entry\">")){
            javaxt.html.Parser parser = new javaxt.html.Parser(div);
            for (javaxt.html.Element el : parser.getElementsByTagName("a")){
                try{
                    String text = el.getInnerText();
                    if (text.equals("Download countries")){
                        String href = el.getAttribute("href");
                        String link = javaxt.html.Parser.MapPath(href, url);
                        String path = new javaxt.utils.URL(link).getPath();
                        if (path.endsWith("/")) path = path.substring(0, path.length()-1);
                        String fileName = path.substring(path.lastIndexOf("/")+1);
                        file = new File(downloadDir + "natural_earth/" + fileName);
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
                catch(Exception e){}
            }
            if (file!=null) break;
        }



        if (file==null) throw new Exception("Failed to find file to download");
        return file;
    }


  //**************************************************************************
  //** unzip
  //**************************************************************************
    private static File unzip(File file) throws Exception {
        ZipInputStream zip = new ZipInputStream(file.getInputStream());
        ZipEntry entry;
        File shapeFile = null;
        while((entry = zip.getNextEntry())!=null){
            String fileName = entry.getName();
            String ext = fileName.substring(fileName.lastIndexOf(".")+1);
            if (ext.equals("shp") || ext.equals("shx") || ext.equals("dbf")){
                byte[] buffer = new byte[2048];
                File outputFile = new File(file.getDirectory(), fileName);
                if (!outputFile.exists()){
                    java.io.FileOutputStream output = outputFile.getOutputStream();
                    int len;
                    while ((len = zip.read(buffer)) > 0){
                        output.write(buffer, 0, len);
                        output.flush();
                    }
                    output.close();
                }
                if (ext.equals("shp")) shapeFile = outputFile;
            }
        }
        zip.close();

        return shapeFile;
    }
}