package com.kartographia.gazetter.web;
import com.kartographia.gazetter.*;

//webservice includes
import javaxt.express.ServiceRequest;
import javaxt.express.ServiceResponse;
import javaxt.express.WebService;
import javaxt.http.servlet.ServletException;

//java includes
import java.util.*;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;

//javaxt includes
import javaxt.sql.*;
import javaxt.json.*;
import javaxt.utils.Console;

//jts includes
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;


//******************************************************************************
//**  MapService
//******************************************************************************
/**
 *   Web service used to generate map tiles and execute map operations
 *
 ******************************************************************************/

public class MapService extends WebService {

    private ConcurrentHashMap<String, byte[]> mapCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, JSONObject> aoiStats = new ConcurrentHashMap<>();
    private Console console = new Console();

    private static PrecisionModel precisionModel = new PrecisionModel();
    private static GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 4326);

    private static DecimalFormat df = new DecimalFormat("#.##");


  //**************************************************************************
  //** getLayers
  //**************************************************************************
    public ServiceResponse getLayers(ServiceRequest request, Database database)
        throws ServletException, IOException {


        String layerType = request.getPath(1).toString();
        if (layerType.equalsIgnoreCase("basemaps")){
            return new ServiceResponse(Config.getBaseMaps());
        }
        else{
            return new ServiceResponse(501, "Not Implemented");
        }
    }


  //**************************************************************************
  //** getTile
  //**************************************************************************
    public ServiceResponse getTile(ServiceRequest request, Database database)
        throws ServletException, IOException {

        String layer;
        double north;
        double south;
        double west;
        double east;
        int width;
        int height;
        String format;
        int srid;

        if (request.getPath(1).toString().equals("wms")){
            layer = request.getParameter("layer").toString();
            width = request.getParameter("width").toInteger();
            height = request.getParameter("height").toInteger();
            format = request.getParameter("format").toString();
            if (format==null) format = "png";
            else{
                format = format.toLowerCase();
                if (format.startsWith("image/")) format = format.substring(6);
            }
            String[] bbox = request.getParameter("bbox").toString().split(",");
            north = Double.valueOf(bbox[3]);
            south = Double.valueOf(bbox[1]);
            east = Double.valueOf(bbox[2]);
            west = Double.valueOf(bbox[0]);
            srid = 4326;
        }
        else{
            layer = request.getPath(1).toString();
            int z = request.getPath(2).toInteger();
            int x = request.getPath(3).toInteger();
            int y = request.getPath(4).toInteger();
            int size = 256;
            north = tile2lat(y, z);
            south = tile2lat(y + 1, z);
            west = tile2lon(x, z);
            east = tile2lon(x + 1, z);
            width = size;
            height = size;
            format = "png";
            srid = 3857;
        }


      //Create map tile
        MapTile mapTile;
        if (srid==4326){
            mapTile = new MapTile(west, south, east, north, width, height, 4326);
        }
        else{
            double[] sw = get3857(south, west);
            double[] ne = get3857(north, east);
            mapTile = new MapTile(sw[0], sw[1], ne[0], ne[1], width, height, 3857);
        }



      //Return response
        if (layer.equals("countries")){
            return getCountries(mapTile, format, request, database);
        }
        else{
            return new ServiceResponse(404, "Not Found");
        }
    }


  //**************************************************************************
  //** getCountries
  //**************************************************************************
    private ServiceResponse getCountries(MapTile mapTile, String format, ServiceRequest request, Database database)
        throws ServletException, IOException {

      //Check whether the client wants to use cache
        boolean useCache = true;


      //Generate key used to cache the requested tile in memory
        String key = getKey(mapTile, "countries", format);


      //Check map cache
        byte[] bytes = null;
        if (useCache){
            synchronized(mapCache){
                bytes = mapCache.get(key);
            }
        }


      //Create image as needed
        if (bytes==null){

            mapTile.setBackgroundColor(229, 237, 243);
            Color lineColor = new Color(181,182,181);
            Color fillColor = new Color(246,248,246);

            Connection conn = null;
            try{

              //Get tile boundary and expand it by 10%
                double d = Math.max(MapTile.diff(mapTile.getWest(), mapTile.getEast()), MapTile.diff(mapTile.getSouth(), mapTile.getNorth()))*0.10;
                d = Math.min(d, 1.5); //Anything greater than 1.5 degrees creates issues when you zoom out
                d = Math.max(d, 0.01); //Anything smaller than 0.01 and we start to see tile boundaries on the images
                String bbox = "ST_GeomFromText('" + mapTile.getBounds() + "', 4326)";
                String buffer = "st_expand(" + bbox + ", " + df.format(d) + ")";


              //Create select clause
                String select;
                if (mapTile.getSRID() == 4326){
                    select = "st_intersection((st_dump(geom)).geom, (select buffer from vars))";
                }
                else{
                    select = "st_transform(st_intersection((st_dump(geom)).geom, (select buffer from vars)), " + mapTile.getSRID() + ")";
                }


              //Compile sql statement
                String sql =
                    "with vars as (select " + buffer + " as buffer)\r\n" +
                    "select st_astext(" + select + ") from gazetter.place where " +
                    "type='boundary' and subtype='country' and st_intersects(geom, (select buffer from vars))";


              //Execute query and generate list of polygons
                ArrayList<Polygon> polygons = new ArrayList<>();
                conn = database.getConnection();
                for (Recordset rs : conn.getRecordset(sql)){

                    Geometry g = new WKTReader().read(rs.getValue(0).toString());
                    if (g instanceof Polygon){
                        Polygon p = (Polygon) g;
                        polygons.add(p);
                    }
                    else if (g instanceof MultiPolygon){
                        MultiPolygon mp = (MultiPolygon) g;
                        for (int i=0; i<mp.getNumGeometries(); i++){
                            Polygon p = (Polygon) mp.getGeometryN(i);
                            polygons.add(p);
                        }
                    }
                }
                conn.close();


              //Render polygons using specialized mapping function for countries
                mapTile.addCountries(polygons, lineColor, fillColor);

            }
            catch(Exception e){
                if (conn!=null) conn.close();
                return new ServiceResponse(e);
            }

          //Get image bytes
            bytes = mapTile.getImage().getByteArray(format);

          //Update the cache
            synchronized(mapCache){
                mapCache.put(key, bytes);
                mapCache.notify();
            }

        }


      //Return response
        ServiceResponse response = new ServiceResponse(bytes);
        response.setContentType("image/" + format);
        return response;
    }



  //**************************************************************************
  //** getSelect
  //**************************************************************************
    public ServiceResponse getSelect(ServiceRequest request, Database database)
        throws ServletException, IOException {

        String layer = request.getPath(1).toString();
        if (layer.equals("points")){ //e.g. map/select/points/?...
            return selectPoints(request, database);
        }
        else if (layer.equals("polygons")){ //e.g. map/select/polygons/?...
            return selectPolygons(request, database);
        }
        else{
            return new ServiceResponse(500, "Not Implemented");
        }
    }


  //**************************************************************************
  //** selectPoints
  //**************************************************************************
  /** Returns a list of points (cities, towns, etc) that are within a certain
   *  distance of a given coordinate.
   */
    private ServiceResponse selectPoints(ServiceRequest request, Database database)
        throws ServletException, IOException {


      //Parse parameters
        String geom = request.getParameter("geom").toString();
        Double lat = request.getParameter("lat").toDouble();
        Double lon = request.getParameter("lon").toDouble();
        Double buffer = request.getParameter("buffer").toDouble(); //in meters



        String wkt = null;
        boolean useGeom = false;
        if (lat==null && lon==null){
            if (geom!=null){
                wkt = geom;
                useGeom = true;
            }
        }
        else{
            wkt = "POINT(" + lon + " " + lat + ")";
        }
        if (wkt==null){
            return new ServiceResponse(500, "A coordinate or geometry is required");
        }



      //Generate sql string
        String sql = "select id, latitude, longitude, device_id, date, " +
        "st_distance(coordinate, ST_Centroid(ST_GeomFromText('" + wkt + "',4326)), true) as distance " +
        "from device_location where " +
        "ST_Intersects(coordinate, " +

        (useGeom ? (
            "ST_GeomFromText('" + wkt + "', 4326)"
        )
            : //else
        (

            "st_transform(" +
            "    st_buffer(" +
            "      st_transform(ST_GeomFromText('" + wkt + "', 4326), 900913)," +
            "      " + buffer + //radius in meters
            "    )," +
            "    4326" +
            ")"

        )) +
        ")";


        if (geom!=null && !useGeom){
            sql += " and ST_Intersects(coordinate, " +
                    "ST_GeomFromText('" + geom + "', 4326)" +
                ")";
        }


      //Append date filter
        //String dateFilter = getDateFilter(year, month, day, hour, minute, timezone);
        //if (dateFilter==null) dateFilter = request.getDateFilter();
        //if (dateFilter!=null) sql += " and " + dateFilter;

        sql += " order by distance";


      //Execute query and return response
        Connection conn = null;
        try{
            JSONArray arr = new JSONArray();
            LinkedHashMap<String, JSONObject> uniquePoints = new java.util.LinkedHashMap<>();
            conn = database.getConnection();
            for (Recordset rs : conn.getRecordset(sql)){
                Long id = rs.getValue("id").toLong();
                BigDecimal latitude = rs.getValue("latitude").toBigDecimal();
                BigDecimal longitude = rs.getValue("longitude").toBigDecimal();
                Long deviceID = rs.getValue("device_id").toLong();
                Double distance = rs.getValue("distance").toDouble();
                javaxt.utils.Date date = rs.getValue("date").toDate();

              //Generate json. Only include unique points for each device.
                String key = deviceID + "|" + latitude + "|" + longitude;
                if (!uniquePoints.containsKey(key)){
                    JSONObject json = new JSONObject();
                    json.set("id", id);
                    json.set("latitude", latitude);
                    json.set("longitude", longitude);
                    json.set("deviceID", deviceID);
                    json.set("distance", distance);
                    json.set("date", date);
                    uniquePoints.put(key, json);
                    arr.add(json);
                }
            }


            conn.close();
            return new ServiceResponse(arr);
        }
        catch(Exception e){
            if (conn!=null) conn.close();
            return new ServiceResponse(e);
        }
    }


    private ServiceResponse selectPolygons(ServiceRequest request, Database database)
        throws ServletException, IOException {


      //Parse parameters
        String geom = request.getParameter("geom").toString();
        Double lat = request.getParameter("lat").toDouble();
        Double lon = request.getParameter("lon").toDouble();
        Double buffer = request.getParameter("buffer").toDouble(); //in meters



        String wkt = null;
        boolean useGeom = false;
        if (lat==null && lon==null){
            if (geom!=null){
                wkt = geom;
                useGeom = true;
            }
        }
        else{
            wkt = "POINT(" + lon + " " + lat + ")";
        }
        if (wkt==null){
            return new ServiceResponse(500, "A coordinate or geometry is required");
        }


        String sql = "with vars as ( select " +
        (useGeom ? (
            "ST_GeomFromText('" + wkt + "', 4326)"
        )
            : //else
        (

            "st_transform(" +
            "    st_buffer(" +
            "      st_transform(ST_GeomFromText('" + wkt + "', 4326), 900913)," +
            "      " + buffer + //radius in meters
            "    )," +
            "    4326" +
            ")"

        )) + " ) \n" +

        "select n, cc, st_astext(g) as g \n" +
        "st_distance(g, ST_Centroid(ST_GeomFromText('" + wkt + "',4326)), true) as distance \n" +
        "from (\n" +
        "SELECT n, country_code as cc, ST_GeometryN(geom, n) as g\n" +
        "FROM gazetter.place\n" +
        "   CROSS JOIN generate_series(1,(100)) n \n" + //limit of 100?
        "WHERE n <= ST_NumGeometries(geom) \n" +
        "and st_intersects(geom, (select geom from vars))\n" +
        ") as q\n" +
        "where st_intersects(g, (select geom from vars))";




      //Execute query and return response
        Connection conn = null;
        try{
            JSONArray arr = new JSONArray();
            conn = database.getConnection();
            for (Recordset rs : conn.getRecordset(sql)){
                JSONObject json = new JSONObject();
                json.set("n", rs.getValue("n"));
                json.set("g", rs.getValue("g"));
                json.set("cc", rs.getValue("cc"));
                json.set("distance", rs.getValue("distance"));
                arr.add(json);
            }

            conn.close();
            return new ServiceResponse(arr);
        }
        catch(Exception e){
            if (conn!=null) conn.close();
            return new ServiceResponse(e);
        }

    }



  //**************************************************************************
  //** getKey
  //**************************************************************************
    private String getKey(MapTile mapTile, String layer, String format){
        return
        mapTile.getNorth() + "|" + mapTile.getSouth() + "|" +
        mapTile.getEast() + "|" + mapTile.getWest() + "|" +
        mapTile.getWidth() + "|" + mapTile.getHeight() + "|" +
        format + "|" + layer + "|" + mapTile.getSRID();
    }


    private double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private double[] get3857(double lat, double lon) {
        double x = MapTile.getX(lon);
        double y = MapTile.getY(lat);
        return new double[] { x, y };
    }



    private double round(double number){
        number = Math.round(number * 100);
        return number/100;
    }
}