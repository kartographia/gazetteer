package com.kartographia.gazetteer.utils;
import com.kartographia.gazetteer.Source;
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
        try (Connection conn = database.getConnection()) {
            return conn.getRecord("select now()").get(0).toDate();
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

        LinkedHashMap<String, Integer> updates = new LinkedHashMap<>();

        try (Connection conn = database.getConnection()) {
            for (javaxt.sql.Record record : conn.getRecords(sql)){
                updates.put(record.get(0).toString(), record.get(1).toInteger());
            }
            return updates;
        }
    }

}