package com.kartographia.gazetter.source;
import com.kartographia.gazetter.*;

import javaxt.sql.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.sql.SQLException;


public class Utils {

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


  //**************************************************************************
  //** Counter
  //**************************************************************************
  /** Used to render status updates to the console.
   */
    public static class Counter {
        private String statusText = "Status: 0  %  ETC: ---------- --:-- --";
        private int percentComplete = 0;
        private long ttl;
        private AtomicLong counter;
        private long startTime;

      //Instantiate the counter with a known record count
        public Counter(long ttl){
            this.ttl = ttl;
            init();
        }

        public Counter(javaxt.io.File file, boolean skipHeader) throws Exception {
            this(file.getBufferedReader("UTF-8"), skipHeader);
        }

      //Instantiate the counter using a tsv/csv file
        public Counter(java.io.BufferedReader br, boolean skipHeader) throws Exception {
            System.out.print("Analyzing File...");
            long t = System.currentTimeMillis();
            countRows(br, skipHeader);
            String elapsedTime = getElapsedTime(t);
            System.out.println(" Done!");
            System.out.println("Found " + getNumber(ttl) + " records in " + elapsedTime);
            init();
        }

        private void countRows(java.io.BufferedReader br, boolean skipHeader) throws Exception{
            ttl = 0L;
            if (skipHeader) br.readLine();
            while (br.readLine()!=null){
                ttl++;
            }
            br.close();
        }

        private void init(){
            javaxt.utils.Date startDate = new javaxt.utils.Date();
            startDate.setTimeZone("America/New York");
            System.out.println("Starting ingest at " + startDate.toString("yyyy-MM-dd HH:mm a"));
            startTime = System.currentTimeMillis();
            counter = new AtomicLong(0);
        }

        public void updateCount(){
            if (counter.get()==0) System.out.print(statusText);
            long x = counter.incrementAndGet();
            double p = ((double) x/ (double) ttl);
            int currPercent = (int) Math.round(p*100);
            if (currPercent > percentComplete){
                percentComplete = currPercent;


                long currTime = System.currentTimeMillis();
                int elapsedTime = (int) Math.round(((currTime-startTime)/1000)/60); //minutes
                int totalTime = (int) Math.round((double)elapsedTime/p); //minutes
                int timeRemaining = totalTime - elapsedTime;

                javaxt.utils.Date etc = new javaxt.utils.Date();
                etc.add(timeRemaining, "minutes");


                if (percentComplete==100) etc = new javaxt.utils.Date();
                etc.setTimeZone("America/New York");

                String _etc = etc.toString("yyyy-MM-dd HH:mm a");
                if (elapsedTime==0) _etc = "---------- --:-- --";


                for (int i=0; i<statusText.length(); i++){
                    System.out.print("\b");
                }
                String str = statusText.replace("0  %", pad(percentComplete)+"%");
                str = str.replace("---------- --:-- --", _etc);

                System.out.print(str);
            }
        }
    }


  //**************************************************************************
  //** getElapsedTime
  //**************************************************************************
  /** Computes elapsed time between a given startTime and now. Returns a
   *  human-readable string representing the elapsed time.
   */
    public static String getElapsedTime(long startTime){
        long t = System.currentTimeMillis()-startTime;
        if (t<1000) return t + "ms";
        long s = Math.round(t/1000);
        if (s<60) return s + "s";
        long m = Math.round(s/60);
        return m + "m";
    }

  //**************************************************************************
  //** getNumber
  //**************************************************************************
  /** Used to format a number with commas.
   */
    public static String getNumber(long l){
        return java.text.NumberFormat.getNumberInstance(Locale.US).format(l);
    }


  //**************************************************************************
  //** pad
  //**************************************************************************
  /** Used to pad a number with white spaces. Used when printing percent
   *  complete to the standard output stream.
   */
    private static String pad(int i){
        String s = ""+i;
        if(s.length()==1){
          s += "  ";
        }
        else if(s.length()==2){
          s += " ";
        }
        return s;
    }
}