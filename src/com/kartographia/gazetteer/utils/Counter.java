package com.kartographia.gazetteer.utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import javaxt.express.utils.StatusLogger;
import static javaxt.express.utils.StringUtils.*;

public class Counter {

    private AtomicLong recordCounter;
    private StatusLogger statusLogger;


  //Instantiate the counter with a known record count
    public Counter(long ttl){
        init(ttl);
    }

    public Counter(javaxt.io.File file, boolean skipHeader) throws Exception {
        this(file.getBufferedReader("UTF-8"), skipHeader);
    }

  //Instantiate the counter using a tsv/csv file
    public Counter(java.io.BufferedReader br, boolean skipHeader) throws Exception {
        System.out.print("Analyzing File...");
        long t = System.currentTimeMillis();
        long ttl = countRows(br, skipHeader);
        String elapsedTime = getElapsedTime(t);
        System.out.println(" Done!");
        System.out.println("Found " + format(ttl) + " records in " + elapsedTime);
        init(ttl);
    }

    private long countRows(java.io.BufferedReader br, boolean skipHeader) throws Exception{
        long ttl = 0L;
        if (skipHeader) br.readLine();
        while (br.readLine()!=null){
            ttl++;
        }
        br.close();
        return ttl;
    }

    public long getTotal(){
        return statusLogger.getTotalRecords();
    }

    private void init(long ttl){
        recordCounter = new AtomicLong(0);
        statusLogger = new StatusLogger(recordCounter);
        statusLogger.setTotalRecords(ttl);
    }

    public void updateCount(){
        recordCounter.incrementAndGet();
    }

    public void stop(){
        statusLogger.shutdown();
    }
}