package com.kartographia.gazetteer.utils;
import static com.kartographia.gazetteer.utils.StringUtils.*;
import java.util.concurrent.atomic.AtomicLong;
  /** Used to render status updates to the console.
   */
public class Counter {
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
        System.out.println("Found " + format(ttl) + " records in " + elapsedTime);
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

    public long getTotal(){
        return ttl;
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