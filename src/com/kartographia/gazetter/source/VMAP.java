package com.kartographia.gazetter.source;


public class VMAP {

    public VMAP(javaxt.sql.Connection conn) throws java.sql.SQLException{
        intesectPolygons(conn);

    }



    private void intesectPolygons(javaxt.sql.Connection conn) throws java.sql.SQLException {
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
