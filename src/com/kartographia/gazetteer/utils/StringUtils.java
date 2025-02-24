package com.kartographia.gazetteer.utils;
import java.util.Locale;

public class StringUtils {


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
    public static String format(long l){
        return java.text.NumberFormat.getNumberInstance(Locale.US).format(l);
    }


}