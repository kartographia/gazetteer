package com.kartographia.gazetteer.source;
import com.kartographia.gazetteer.utils.Counter;
import com.kartographia.gazetteer.utils.DbUtils;
import com.kartographia.gazetteer.Place;
import com.kartographia.gazetteer.Edit;
import javaxt.json.*;
import javaxt.sql.*;
import java.sql.SQLException;
import java.util.*;

//******************************************************************************
//**  US Census
//******************************************************************************
/**
 *  Used to rank cities and update population information using data from the
 *  US Census Bureau.
 *
 ******************************************************************************/

public class USCensus {

    private static String delimiter = "\t";


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to update population and rank of US cities using 2010 Census data.
   * @param file
   *  http://www2.census.gov/geo/docs/maps-data/data/gazetteer/Gaz_places_national.zip
   *  http://www.census.gov/geo/maps-data/data/gazetteer2010.html
   */
    public static void load(javaxt.io.File file, Database database) throws Exception {

      //Get record count
        Counter counter = new Counter(file, true);


      //Create prepared statement to find places
        java.sql.PreparedStatement placeQuery;
        javaxt.sql.Connection conn = database.getConnection();
        placeQuery = conn.getConnection().prepareStatement( //lon, lat, a1, uname
            "select place.id, ST_Distance(ST_Centroid(geom), ST_GeomFromText('POINT(? ?)', 4326), true) as distance " +
            "from gazetteer.place as place right join gazetteer.place_name as place_name on place.id = place_name.place_id " +
            "where country_code='US' and admin1=? and uname=UPPER(?)"
        );


      //Log edit
        Edit edit = new Edit();
        edit.setStartDate(DbUtils.getDate(database));
        edit.setSummary("Update population and rank of US cities using 2010 Census data");
        edit.save();


      //Parse file
        java.io.BufferedReader br = file.getBufferedReader("UTF-8");
        br.readLine(); //Skip header
        String row;
        while ((row = br.readLine()) != null){
            counter.updateCount();


          //Update column values
            String[] col = row.split(delimiter, -1);
            for (int i=0; i<col.length; i++){
                col[i] = col[i].trim();
                if (col[i].length()==0) col[i] = null;
            }


          //Get key fields
            String a1 = col[0];
            String name = col[3];
            long pop = clng(col[6]);
            double lat = cdbl(col[12]);
            double lon = cdbl(col[13]);


          //Update names
            String[] arr = extractCityName(name);
            String cityName = arr[0];
            String cityType = arr[1];
            cityName = updateCityName(cityName);




          //Set rank
            int rank = 5;
            if (pop>=150000){
                rank = 3;
            }
            else if (pop<150000 && pop>20000){
                rank = 4;
            }
            else{
                rank = 5;
            }




          //Find city in the database
            TreeMap<Double, Long> cities = findCities(cityName, a1, lat, lon, placeQuery);
            if (cities.isEmpty()){
                if (cityName.contains("-")){
                    for (String str : cityName.split("-")){
                        str = updateCityName(str);

                        TreeMap<Double, Long> c2 = findCities(str, a1, lat, lon, placeQuery);
                        Iterator<Double> it = c2.keySet().iterator();
                        while (it.hasNext()){
                            double distance = it.next();
                            long cityID = c2.get(distance);
                            cities.put(distance, cityID);
                        }
                    }
                }
                else{
                    if (cityName.contains("(") && cityName.endsWith(")")){
                        String a = cityName.substring(0, cityName.lastIndexOf("(")).trim();
                        String b = cityName.substring(cityName.lastIndexOf("("));
                        b = b.substring(1, b.length()-1);

                        for (String str : new String[]{a,b}){
                            str = updateCityName(str);

                            TreeMap<Double, Long> c2 = findCities(str, a1, lat, lon, placeQuery);
                            Iterator<Double> it = c2.keySet().iterator();
                            while (it.hasNext()){
                                double distance = it.next();
                                long cityID = c2.get(distance);
                                cities.put(distance, cityID);
                            }
                        }

                    }
                }
            }

            if (cities.isEmpty()){
                //addCity(cityName, a1);
                //System.out.println(cityName + ", " + a1 + " (" + pop + ")");
            }
            else{

                Iterator<Double> it = cities.keySet().iterator();
                double distance = it.next();
                long cityID = cities.get(distance);
                double f = 15d;
                if (rank<=3) f = 35d;
                if (rank==4) f = 20d;

                if (distance>(f*1609.34d)){
                    System.out.println(cityName + ", " + a1 + " (" + pop + ") <------------------- " + Math.round(distance/1609.34d) + "mi " + (rank<=3? "????" : ""));
                }
                else{


                    updateCity(cityID, pop, rank);



                    if (it.hasNext()){
                        double d2 = it.next();
                        long c2 = cities.get(d2);


                        if ((d2-distance)<(5*1609.34)){

                            System.out.println(cityName + ", " + a1 + " (" + pop + ")");
                            System.out.println(" - " + cityID + " (" + distance + ")");
                            System.out.println(" - " + c2 + " (" + d2 + ")");


                            updateCity(cityID, pop, rank);

                        }
                    }
                }

            }
        }

      //Clean-up
        placeQuery.close();
        conn.close();
        br.close();


      //Update edit log
        edit.setEndDate(DbUtils.getDate(database));
        edit.save();
    }


  //**************************************************************************
  //** updateCity
  //**************************************************************************
  /** */
    private static void updateCity(long cityID, long pop, int rank){
        try{
            Place place = new Place(cityID);
            place.setRank(rank);
            JSONObject json = place.getInfo();
            if (json==null) json = new JSONObject();
            json.set("population", pop);
            place.setInfo(json);
            place.save();
        }
        catch(Exception e){
        }
    }


  //**************************************************************************
  //** findCities
  //**************************************************************************
    private static TreeMap<Double, Long> findCities(String name, String a1, double lat, double lon, java.sql.PreparedStatement placeQuery) throws SQLException {

        TreeMap<Double, Long> cities = new TreeMap<>();

//        String sql = "select place.id, ST_Distance(ST_Centroid(geom), ST_GeomFromText('POINT(" + lon + " " + lat + ")', 4326), true) as distance " +
//        "from place right join place_name on place.id = place_name.place_id " +
//        "where country_code='US' and admin1='" + a1 + "'" +
//        " and uname=UPPER('" + name.replace("'", "''") + "')"
//        ;


        placeQuery.setDouble(1, lon);
        placeQuery.setDouble(2, lat);
        placeQuery.setString(3, a1);
        placeQuery.setString(4, name);
        java.sql.ResultSet r2 = placeQuery.executeQuery();
        while (r2.next()) {
            long cityID = r2.getLong(1);
            double distance = r2.getDouble(2);
            cities.put(distance, cityID);
        }
        r2.close();


        return cities;
    }



  //**************************************************************************
  //** extractCityName
  //**************************************************************************
  /** US Census appends city names with a classification such as "village",
   *  "town", "city", etc. This method extracts the name and classification.
   */
    private static String[] extractCityName(String name){

        String city = null;
        String type = null;

        String[] arr = name.split(" ");
        for (int i=arr.length-1; i>=0; i--){


            if (!arr[i-1].toLowerCase().equals(arr[i-1]) || arr[i].equals("CDP")){
                int x = i;
                city = "";
                for (i=0; i<x; i++){
                    if (i>0) city += " ";
                    city += arr[i];
                }

                type = "";
                for (i=x; i<arr.length; i++){
                    type += arr[i];
                    if (i<arr.length-1) type += " ";
                }

                break;
            }

        }

        return new String[]{city, type};
    }


  //**************************************************************************
  //** updateCityName
  //**************************************************************************
    private static String updateCityName(String name){
        if (name.contains(" ")){
            if (name.startsWith("Greater") || name.startsWith("Central")){
                name = name.substring(name.indexOf(" ")).trim();
            }
        }
        name =  name.replace("St. ", "Saint ");
        return name;
    }






//
//  //**************************************************************************
//  //** loadY2K
//  //**************************************************************************
//  /** Used to rank cities based on population data from the 2000 Census:
//   *  http://www.census.gov/geo/www/gazetteer/places2k.html#places
//   *
//   *  Columns 1-2: United States Postal Service State Abbreviation
//   *  Columns 3-4: State Federal Information Processing Standard (FIPS) code
//   *  Columns 5-9: Place FIPS Code
//   *  Columns 10-73: Name
//   *  Columns 74-82: Total Population (2000)
//   *  Columns 83-91: Total Housing Units (2000)
//   *  Columns 92-105: Land Area (square meters)
//   *  Columns 106-119: Water Area(square meters)
//   *  Columns 120-131: Land Area (square miles)
//   *  Columns 132-143: Water Area (square miles)
//   *  Columns 144-153: Latitude (decimal degrees)
//   *  Columns 154-164: Longitude (decimal degrees)
//   */
//    public static void loadY2K(javaxt.io.File file, javaxt.sql.Connection conn) throws Exception {
//
//
//        BufferedReader br = file.getBufferedReader("UTF-8");
//
//
//        String row;
//
//        int y=0;
//        while ((row = br.readLine()) != null){
//
//            City city = new City();
//
//            city.name = row.substring(9, 73).trim();
//            city.name = updateName(city.name);
//
//
//            city.lat = cdbl(row.substring(143, 153));
//            city.lon = cdbl(row.substring(153, 164));
//            city.cc = "US";
//            city.a1 = row.substring(0, 2);
//            city.source = "USGS";
//
//
//            Integer pop = cint(row.substring(73, 82).trim());
//            //System.out.println(pop);
//            if (pop>=150000){
//                city.rank = 3;
//            }
//            else if (pop<150000 && pop>20000){
//                city.rank = 4;
//            }
//            else{
//                city.rank = 5;
//            }
//
//
//            city.pop = pop;
//
//
//            //System.out.println(city);
//
//
//
//
//            if (city.rank < 5){
//                //System.out.println(city);
//
//                boolean foundMatch = false;
//
//
//                if (city.name.contains("-")){
//                    foundMatch = updateRank(city, conn);
//                    if (!foundMatch){
//                        String[] arr = city.name.split("-");
//                        for (int i=0; i<arr.length; i++){
//                            city.name = updateName(arr[i].trim());
//                            foundMatch = updateRank(city, conn);
//                            if (foundMatch) break;
//                        }
//                    }
//
//
//                }
//                else if (city.name.contains("(") && city.name.endsWith(")")){
//                    String n1 = city.name.substring(0, city.name.lastIndexOf("(")).trim();
//                    String n2 = city.name.substring(city.name.lastIndexOf("(")+1, city.name.length()-1).trim();
//                    //System.out.println(city.name + " (" + n1 + " vs " + n2 + ")");
//
//                    city.name = updateName(n1);
//                    foundMatch = updateRank(city, conn);
//
//                    if (!foundMatch){
//                        if (!n2.equalsIgnoreCase("balance")){
//                            city.name = updateName(n2);
//                            foundMatch = updateRank(city, conn);
//                        }
//                    }
//
//
//
//                }
//                else if (
//                    city.name.toLowerCase().endsWith(" town") ||
//                    city.name.toLowerCase().endsWith(" city") ||
//                    city.name.toLowerCase().endsWith(" township") ||
//                    city.name.toLowerCase().endsWith(" north") ||
//                    city.name.toLowerCase().endsWith(" south") ||
//                    city.name.toLowerCase().endsWith(" east") ||
//                    city.name.toLowerCase().endsWith(" west")){
//
//                    foundMatch = updateRank(city, conn);
//
//                    if (!foundMatch){
//                        city.name = city.name.substring(0, city.name.lastIndexOf(" ")).trim();
//                        foundMatch = updateRank(city, conn);
//                    }
//                }
//                else{
//                    foundMatch = updateRank(city, conn);
//                }
//
//                if (!foundMatch){
//                    //System.out.println(row.substring(9, 73).trim() + " (" + city.a1 + ")");
//                    System.out.println(city.name + " (" + city.a1 + ") -- " + city.rank);
//                    y++;
//                }
//
//            }
//
//
//            x++;
//
//
//            //if (x>3) break;
//        }
//
//        System.out.println("Missed " + y + " cities.");
//
//        br.close();
//
//    }
//
//    private static String updateName(String name){
//        if (name.contains(" ")){
//
//
//            if (name.endsWith("city and borough")){
//                name = name.substring(0, name.lastIndexOf("city and borough")).trim();
//            }
//            else if (name.endsWith(" city") || name.endsWith(" town") || name.endsWith(" village") ||
//                name.endsWith(" borough") || name.endsWith(" municipality") ||
//                name.endsWith(" comunidad")|| name.endsWith(" CDP") ){
//                name = name.substring(0, name.lastIndexOf(" ")).trim();
//            }
//            else if (name.endsWith("zona urbana")){
//                name = name.substring(0, name.lastIndexOf("zona urbana")).trim();
//            }
//            else if (name.endsWith("(balance)")){
//                name = name.substring(0, name.lastIndexOf("(balance)")).trim();
//            }
//
//        }
//
//        if (name.contains(" ")){
//            if (name.startsWith("Greater") || name.startsWith("Central")){
//                name = name.substring(name.indexOf(" ")).trim();
//            }
//        }
//
//
//        name =  name.replace("St. ", "Saint ");
//        return name;
//    }
//
//
//    private static boolean updateRank(City city, javaxt.sql.Connection conn) throws java.sql.SQLException {
//
//        boolean foundMatch = false;
//
//
//        String n = city.name;
//        if (n.contains("'")){
//            n = n.replace("'", "''");
//        }
//
//        String where = " where " +
//            "cty_ucase='" + n.toUpperCase() + "' and " +
//            "cty_cc='" + city.cc + "' and " +
//            "cty_a1='" + city.a1 + "'";
//
//
//
//
//        String sql = "Select count(cty_id) from t_cities" + where;
//        javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
//        rs.open(sql, conn);
//        int numMatches = rs.getValue(0).toInteger();
//        rs.close();
//
//        if (numMatches==1){
//            sql = "Select cty_id, cty_rank from t_cities" + where;
//            rs.open(sql, conn, false);
//            rs.setValue("cty_rank", city.rank);
//            rs.update();
//            rs.close();
//            foundMatch = true;
//        }
//        else{
//
//
//
//            sql = "Select cty_id, cty_name, cty_rank from t_cities " +
//            "WHERE " +
//            "cty_cc='" + city.cc + "' and " +
//            "cty_a1='" + city.a1 + "'" +
//            "ORDER BY distance(cty_point, GeomFromText('POINT(" + city.lon + " " + city.lat + ")', 4326)) " +
//            "LIMIT 15";
//
//            //StringBuffer str = new StringBuffer();
//
//
//            rs.open(sql, conn, false);
//            while (rs.hasNext()){
//                String name = rs.getValue("cty_name").toString();
//                //str.append(city.name + " vs " + name + " (" + city.a1 + ")" + "\r\n");
//
//                if (name.equals(city.name)){
//                    rs.setValue("cty_rank", city.rank);
//                    rs.update();
//                    foundMatch = true;
//                    break;
//                }
//
//                rs.moveNext();
//            }
//
//            //if (!foundMatch) System.out.println(str);
//
//            rs.close();
//
//
//        }
//
//        return foundMatch;
//
//
//    }


  //**************************************************************************
  //** clng
  //**************************************************************************
  /** Used to convert a string to an long.
   */
    private static Long clng(String str){
        try{
            return Long.parseLong(str);
        }
        catch(Exception e){
            return null;
        }
    }

  //**************************************************************************
  //** cdbl
  //**************************************************************************
  /** Used to convert a string to a double.
   */
    private static Double cdbl(String str){
        try{
            return Double.parseDouble(str);
        }
        catch(Exception e){
            return null;
        }
    }

}
