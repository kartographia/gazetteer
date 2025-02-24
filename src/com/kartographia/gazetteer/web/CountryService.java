package com.kartographia.gazetteer.web;
import com.kartographia.gazetteer.Place;

import javaxt.express.ServiceRequest;
import javaxt.express.ServiceResponse;
import javaxt.http.servlet.HttpServletRequest;
import javaxt.http.servlet.ServletException;

import javaxt.json.*;
import javaxt.sql.*;
import javaxt.express.utils.StringUtils;

public class CountryService {


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public CountryService(){

    }


  //**************************************************************************
  //** getServiceResponse
  //**************************************************************************
    public ServiceResponse getServiceResponse(String service, HttpServletRequest request, Database database)
        throws ServletException {

        if (service.equals("countries")){
            return getCountries(database);
        }
        else{ //service="country"
            return getCountry(new ServiceRequest(service, request), database);
        }
    }


  //**************************************************************************
  //** getCountries
  //**************************************************************************
  /** Returns a list of country names and country codes
   */
    private ServiceResponse getCountries(Database database){
        String sql =

      //Build with statement
        "with vars as (select name, country_code, min(name.type) as type " +
        "from gazetter.place " +
        "join gazetter.name on place.id=name.place_id " +
        "where place.type='boundary' and place.subtype='country' " +
        "group by country_code, name order by country_code, type, name) " +

      //Select from with statement
        "select f.name, f.country_code, f.type " +
        "from ( " +
        "   select country_code, min(type) as mintype " +
        "   from vars group by country_code " +
        ") as x inner join vars as f on f.country_code = x.country_code and f.type = x.mintype order by f.name";


        try (Connection conn = database.getConnection()) {
            JSONArray arr = new JSONArray();
            for (javaxt.sql.Record record : conn.getRecords(sql)){
                JSONObject json = new JSONObject();
                for (Field field : record.getFields()){
                    String fieldName = StringUtils.underscoreToCamelCase(field.getName());
                    if (fieldName.equals("type")) continue;
                    json.set(fieldName, field.getValue());
                }
                arr.add(json);
            }
            return new ServiceResponse(arr);
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
    }


  //**************************************************************************
  //** getCountry
  //**************************************************************************
    private ServiceResponse getCountry(ServiceRequest request, Database database){
        try{


            String filter = request.getPath(0).toString();
            String cc = request.getPath(1).toString();
            if (filter==null){
                cc = filter;
                filter = "";
            }
            if (cc==null) throw new Exception("Country code is required");



            if (filter.equals("names")){
                return getCountryNames(cc, database);
            }
            else if (filter.equals("extents")){
                return getExtents(cc, database);
            }
            else{
                Place place = Place.get("country_code=", cc);
                if (place==null) return new ServiceResponse(404);
                else{
                    return new ServiceResponse(place.toJson());
                }
            }
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
    }


  //**************************************************************************
  //** getNames
  //**************************************************************************
    private ServiceResponse getCountryNames(String cc, Database database){

        String sql = "select gazetter.name.* " +
        "from gazetter.place " +
        "join gazetter.name on place.id=name.place_id " +
        "where place.country_code='" + cc.toUpperCase() +
            "' and place.type='boundary' and place.subtype='country'";

        try (Connection conn = database.getConnection()) {
            JSONArray arr = new JSONArray();
            for (javaxt.sql.Record record : conn.getRecords(sql)){
                JSONObject json = new JSONObject();
                for (Field field : record.getFields()){
                    String fieldName = StringUtils.underscoreToCamelCase(field.getName());
                    if (fieldName.equals("uname")) continue;
                    json.set(fieldName, field.getValue());
                }
                arr.add(json);
            }

            return new ServiceResponse(arr);
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
    }


  //**************************************************************************
  //** getExtents
  //**************************************************************************
    private ServiceResponse getExtents(String cc, Database database){

        String sql =

      //Get geometry for the selected country. Note that we are simplifying the
      //geometry for performance reasons
        "with vars as (\n" +
            "select ST_Simplify(geom, 0.01) as geom from gazetter.place " +
               "where place.country_code='" + cc.toUpperCase() + "' " +
                "and place.type='boundary' and place.subtype='country'" +
        ")\n" +


      //Create a union of the extents of individual polygons that make up a
      //country. Individual extents have a buffer of 0.1 degrees to capture
      //smaller islands
        "SELECT st_astext(ST_Union(ARRAY(\n" +

        "select st_expand(extent, 0.2) from\n" +


      //Get the extents of the individual polygons that make up a country
        "(\n" +
            "SELECT n, (st_extent(ST_GeometryN(geom, n))) as extent\n" +
            "FROM vars\n" +
            "	CROSS JOIN generate_series(1,(select ST_NumGeometries(geom) from vars)) n\n" +
            "WHERE n <= ST_NumGeometries(geom) group by n order by n\n" +
        ") as extents\n" +

        ")))";

        try (Connection conn = database.getConnection()) {
            JSONArray arr = new JSONArray();

            for (javaxt.sql.Record record : conn.getRecords(sql)){
                String geom = record.get(0).toString();
                if (geom!=null) arr.add(geom);
            }


            if (arr.isEmpty()){
                sql = sql.replace("ST_Simplify(geom, 0.01)", "geom");
                for (javaxt.sql.Record record : conn.getRecords(sql)){
                    String geom = record.get(0).toString();
                    if (geom!=null) arr.add(geom);
                }
            }


            return new ServiceResponse(arr);
        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
    }

}