package com.kartographia.gazetteer.source;
import com.kartographia.gazetteer.utils.Counter;
import com.kartographia.gazetteer.*;

import javaxt.json.JSONObject;
import javaxt.sql.Database;
import java.util.*;

import openmap.*;
import org.locationtech.jts.geom.*;


//******************************************************************************
//**  VMAP Data Loader
//******************************************************************************
/**
 *  Used to parse and load VMAP data from NGA
 *
 ******************************************************************************/

public class VMAP {


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used load a shapefile containing political boundaries. Each record in
   *  the shapefile should contain the FIPS country code and a polygon/multi-
   *  polygon representing the physical borders of the country. The shapefile
   *  was generated using NGA's World Vector Shoreline Plus (WVSPLUS).
   *  @param file polboundaries_250K.shp
   */
    public static void load(javaxt.io.File file, Database database) throws Exception {
        Source source = Source.get("name=","NGA");


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
                if (!colNames.contains("name") || !colNames.contains("cc") || !colNames.contains("geom") ){
                    throw new Exception();
                }
                isValidated = true;
            }


            String name = record.getValue("name").toString();
            String cc = record.getValue("cc").toString();
            Geometry geom = record.getValue("geom").toGeometry();


          //Save country
            Place place = new Place();
            place.setCountryCode(cc);
            place.setGeom(geom);
            place.setSource(source);
            place.setType("boundary");
            place.setSubtype("country");

            JSONObject json = new JSONObject();
            json.set("source", file.getName());
            place.setInfo(json);

            Name placeName = new Name();
            placeName.setName(name);
            placeName.setLanguageCode("eng");
            placeName.setType(2); //2=formal
            placeName.setSource(source);

            try{
                place.save();
                placeName.setPlace(place);
                placeName.save();
            }
            catch(Exception e){
                System.out.println("Error saving \"" + name + "\" (" + cc + ")");
                e.printStackTrace();
            }
        }

        counter.stop();
    }



    private void intersectPolygons(javaxt.sql.Connection conn) throws java.sql.SQLException {
        javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
        String sql = "Select gid, nam from vmap_cities";
        rs.open(sql, conn);
        while (rs.hasNext()){
            int id = rs.getValue("gid").toInteger();
            String name = rs.getValue("nam").toString();
            System.out.println("\r\n" + id + ":\t" + name);


            if (name!=null && !name.equals("UNK")){
                //updateCommonName(id, name, conn);
            }

            updateRank(id, conn);


            rs.moveNext();
        }
        rs.close();
    }

    private void updateCommonName(int id, String name, javaxt.sql.Connection conn) throws java.sql.SQLException {
        javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
        String sql =
        "select * from vmap_cities, t_cities where " +
        "gid=" + id + " and cty_ucase='" + name.trim().toUpperCase() + "' and " +
        "intersects(t_cities.cty_point, vmap_cities.the_geom) " +
        "order by id";

        rs.open(sql, conn, false);
        while (rs.hasNext()){

            String commonName = rs.getValue("cty_name").toString();
            System.out.println(commonName + " vs " + name);

            rs.moveNext();
        }
        rs.close();

    }


    private void updateRank(int id, javaxt.sql.Connection conn) throws java.sql.SQLException {
        javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
        String sql =
        "select * from vmap_cities, t_cities where " +
        "gid=" + id + " and " + //cty_name=
        "ST_Intersects(t_cities.cty_point, vmap_cities.the_geom) " +
        "order by cty_rank asc, cty_source_id";

        Integer maxRank = null;
        rs.open(sql, conn);
        while (rs.hasNext()){
            String name = rs.getValue("cty_name").toString();
            String cc = rs.getValue("cty_cc").toString();
            int rank = rs.getValue("cty_rank").toInteger();
            if (maxRank==null) maxRank = rank;


            if (!cc.equals("US")){

              //If the polygon includes multiple cities,
                boolean pplx = rank>maxRank;


              //If the city is unranked, update the rank
                if (rank > 4){
                    rank = 4;
                }


                System.out.println(name + ", " + cc + " (" + pplx + ")");
                try{
                    conn.execute("UPDATE t_cities SET cty_rank=" + rank + ", cty_pplx=" + pplx + " where cty_id=" + rs.getValue("cty_id").toInteger());
                }
                catch(Exception e){
                    e.printStackTrace();
                }

                //rs.setValue("cty_rank", rank);
                //rs.setValue("cty_pplx", pplx);
                //rs.update();




            }
            rs.moveNext();
        }
        rs.close();

    }

}
