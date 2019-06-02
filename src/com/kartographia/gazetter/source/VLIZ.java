package com.kartographia.gazetter.source;
import com.kartographia.gazetter.*;
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
        Source source = Source.get("name=","VLIZ");


        ShapeFile shp = new ShapeFile(file.toFile());
        System.out.println("Found " + shp.getRecordCount() + " records in the shapefile");
        Utils.Counter counter = new Utils.Counter(shp.getRecordCount());

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
                if (!colNames.contains("name") || !colNames.contains("geom") ){
                    throw new Exception();
                }
                isValidated = true;
            }


            String name = record.getValue("name").toString();
            Geometry geom = record.getValue("geom").toGeometry();


          //Save country
            Place place = new Place();
            place.setCountryCode("OC"); //<-- Not an offical CC!
            place.setGeom(geom);
            place.setSource(source);
            place.setType("boundary");
            place.setSubtype("marine");

            JSONObject json = new JSONObject();
            json.set("source", file.getName());
            place.setInfo(json);

            try{
                place.save();
            }
            catch(Exception e){
                //probably a duplicate
            }


            PlaceName placeName = new PlaceName();
            placeName.setName(name);
            placeName.setLanguageCode("eng");
            placeName.setType(2); //2=formal
            placeName.setPlace(place);
            try{
                placeName.save();
            }
            catch(Exception e){

            }

        }

    }

}