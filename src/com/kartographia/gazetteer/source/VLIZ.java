package com.kartographia.gazetteer.source;
import com.kartographia.gazetteer.utils.Counter;
import com.kartographia.gazetteer.Name;
import com.kartographia.gazetteer.Source;
import com.kartographia.gazetteer.Place;
import com.vividsolutions.jts.geom.*;
import javaxt.json.JSONObject;
import javaxt.sql.Database;
import java.util.*;
import openmap.*;

//******************************************************************************
//**  VLIZ Data Loader
//******************************************************************************
/**
 *  Used to parse and load oceanographic and maritime data from The Flanders
 *  Marine Institute (VLIZ) published via marineregions.org
 *
 ******************************************************************************/


public class VLIZ {


    private static Source source;
    static {
        try{
            String name = "VLIZ";
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
  /** Used load a shapefile containing marine boundaries (boundaries of
   *  oceans, seas, etc). Each record in the shapefile should contain a name
   *  a polygon/multi-polygon representing the physical boundaries of a marine
   *  feature (e.g. Atlantic Ocean).
   *  @param file World_Seas_IHO_v3.shp
   */
    public static void load(javaxt.io.File file, Database database) throws Exception {


        ShapeFile shp = new ShapeFile(file.toFile());
        System.out.println("Found " + shp.getRecordCount() + " records in the shapefile");
        Counter counter = new Counter(shp.getRecordCount());

        boolean isValidated = false;

        Iterator<openmap.Record> it = shp.getRecords();
        while (it.hasNext()){
            openmap.Record record = it.next();
            counter.updateCount();

            if (!isValidated){
                HashSet<String> colNames = new HashSet<>();
                for (Field field : record.getFields()){
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.endsWith("*")) fieldName = fieldName.substring(0, fieldName.length()-1);
                    colNames.add(fieldName);
                }
                if (!colNames.contains("name") || !colNames.contains("geom") ){
                    throw new Exception();
                }
                isValidated = true;
            }


            String name = record.getValue("name").toString();
            Geometry geom = record.getValue("geom").toGeometry();


          //Save country
            Place place = new Place();
            place.setCountryCode("OC"); //<-- Not an offical country code!
            place.setGeom(geom);
            place.setSource(source);
            place.setType("boundary");
            place.setSubtype("marine");

            JSONObject json = new JSONObject();
            json.set("source", file.getName());
            place.setInfo(json);
            place.save();

            Name placeName = new Name();
            placeName.setName(name);
            placeName.setLanguageCode("eng");
            placeName.setType(2); //2=formal
            placeName.setSource(source);


            placeName.setPlace(place);
            placeName.save();
        }

    }

}