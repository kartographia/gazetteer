package com.kartographia.gazetter.utils;
import com.kartographia.gazetter.Source;
import javaxt.sql.*;

import java.util.*;
import java.sql.SQLException;

public class DbUtils {
  //**************************************************************************
  //** getDate
  //**************************************************************************
  /** Returns the current date in the database.
   */
    public static javaxt.utils.Date getDate(Database database) throws SQLException {

        Connection conn = null;
        try{
            javaxt.utils.Date date = null;
            conn = database.getConnection();
            for (Recordset rs : conn.getRecordset("select now()")){
                date = rs.getValue(0).toDate();
            }
            conn.close();
            return date;
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }
    }


  //**************************************************************************
  //** getRecentUpdates
  //**************************************************************************
  /** Returns a list of the most recent updates by country code.
   */
    public static LinkedHashMap<String, Integer> getRecentUpdates(Source source, Database database) throws SQLException{

        String sql =
        "select country_code, max(source_date) from gazetter.place" +
        " where source_id=" + source.getID() +
        " group by country_code order by country_code";

        LinkedHashMap<String, Integer> updates = new LinkedHashMap<String, Integer>();
        Connection conn = null;
        try{
            conn = database.getConnection();
            for (Recordset rs : conn.getRecordset(sql)){
                updates.put(rs.getValue(0).toString(), rs.getValue(1).toInteger());
            }
            conn.close();
            return updates;
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }
    }

}