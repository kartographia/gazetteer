package com.kartographia.gazetteer.utils;

//Java imports
import java.util.*;
import java.nio.file.Paths;

//JavaXT imports
import javaxt.json.JSONObject;
import static javaxt.utils.Console.console;

//Lucene imports
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.document.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;



//******************************************************************************
//**  LuceneIndex
//******************************************************************************
/**
 *   Used to create and manage an searchable file index. The file index is
 *   persisted in a directory on the file system.
 *
 ******************************************************************************/

public class LuceneIndex {

    private Directory dir;
    private DirectoryReader directoryReader;
    private Object wmonitor = new Object();
    private Object smonitor = new Object();
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private PerFieldAnalyzerWrapper perFieldAnalyzerWrapper;

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_INFO = "info";


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public LuceneIndex(javaxt.io.Directory path) throws Exception {
        dir = FSDirectory.open(Paths.get(path.toString()));

        StandardAnalyzer standardAnalyzer = new StandardAnalyzer(noStopWords());

        Map<String,Analyzer> analyzerPerFieldMap = new HashMap<>();
        analyzerPerFieldMap.put(FIELD_NAME, standardAnalyzer);
        perFieldAnalyzerWrapper = new PerFieldAnalyzerWrapper(standardAnalyzer, analyzerPerFieldMap);
    }


  //**************************************************************************
  //** getSize
  //**************************************************************************
  /** Return the number of items in the index
   */
    public int getSize() {
        if (indexExists()) {
            return getIndexSearcher().getIndexReader().numDocs();
        }
        else{
            return 0;
        }
    }


  //**************************************************************************
  //** addRecord
  //**************************************************************************
    public synchronized void addRecord(javaxt.utils.Record record) throws Exception {
        IndexWriter writer = getWriter();
        addRecord(record, writer);
        writer.commit();
    }


  //**************************************************************************
  //** addRecord
  //**************************************************************************
    public synchronized void addRecord(javaxt.utils.Record record, IndexWriter writer) throws Exception {
        Integer id = record.get("id").toInteger();
        String name = record.get("name").toString();
        if (hasID(id) || name==null) return;



        String t = name.toLowerCase();
        if (t.startsWith("st ")) name = "Saint " + name.substring(3);
        if (t.startsWith("st. ")) name = "Saint " + name.substring(4);
        name = updateSearchTerm(name);


        Document doc = new Document();
        doc.add(new TextField(FIELD_ID, id+"", Field.Store.YES));
        doc.add(new TextField(FIELD_NAME, name, Field.Store.YES));
        doc.add(new TextField(FIELD_INFO, new JSONObject(record).toString(), Field.Store.YES));


        writer.addDocument(doc);
    }

    private String updateSearchTerm(String term){
        if (term.contains(",")) term = term.replace(",", " ");
        if (term.contains(".")) term = term.replace(".", "");
        while (term.contains("  ")) term = term.replace("  ", " ").trim();
        return term;
    }


  //**************************************************************************
  //** getRecords
  //**************************************************************************
    public ArrayList<javaxt.utils.Record> getRecords(List<String> searchTerms, Integer limit) throws Exception {

        IndexSearcher searcher = getIndexSearcher();
        if (searcher==null) throw new Exception();
        if (limit==null || limit<1) limit = 10;


      //Compile query
        Query query;
        if (searchTerms.size()==1){
            String term = QueryParser.escape(searchTerms.get(0)).toLowerCase();
            query = new FuzzyQuery(new Term(FIELD_NAME, term));
        }
        else{

            BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();

            for (int i=0; i<searchTerms.size(); i++) {
                String term = searchTerms.get(i);
                if (i==0){
                    String t = term.toLowerCase();
                    if (t.equals("st") || t.equals("st.")) term = "Saint";
                }

                term = updateSearchTerm(term);



                //console.log("term: " + term);

                term = QueryParser.escape(term);

                //console.log("escaped term: " + term);
                term = term.toLowerCase();

                //if (term.equals("st") || term.equals("st."))


                //WildcardQuery nameQuery = new WildcardQuery(new Term(FIELD_NAME, term));
                //term = WildcardQuery.WILDCARD_STRING + term + WildcardQuery.WILDCARD_STRING;
                //console.log("final term: " + term);

                FuzzyQuery nameQuery = new FuzzyQuery(new Term(FIELD_NAME, term));
                bqBuilder.add(new BooleanClause(nameQuery, BooleanClause.Occur.SHOULD));
            }
            query = bqBuilder.build();

        }



      //Execute search
        TopDocs hits = searcher.search(query, limit);
        //console.log("Hits: " + hits.totalHits.value);


      //Group results by score
        TreeMap<Float, ArrayList<javaxt.utils.Record>> searchResults = new TreeMap<>();
        for (ScoreDoc scoreDoc : hits.scoreDocs) {

            float score = scoreDoc.score;
            Document doc = searcher.doc(scoreDoc.doc);
            JSONObject info = new JSONObject(doc.get(FIELD_INFO));
            info.set("score", score);

            ArrayList<javaxt.utils.Record> records = searchResults.get(score);
            if (records==null){
                records = new ArrayList<>();
                searchResults.put(score, records);
                records.add(info);
            }
            else{

              //Not sure if this is necessary but the following logic ensures
              //that the documents are unique
                boolean addRecord = true;
                int id = info.get("id").toInteger();
                for (javaxt.utils.Record record : records){
                    int i = record.get("id").toInteger();
                    if (i==id){
                        addRecord = false;
                        float s = record.get("score").toFloat();
                        if (s<score){
                            record.set("score", score);
                        }
                        break;
                    }
                }
                if (addRecord) records.add(info);
            }
        }


      //Generate response
        ArrayList<javaxt.utils.Record> results = new ArrayList<>();
        Iterator<Float> it = searchResults.descendingKeySet().iterator();
        Float topScore = null;
        while (it.hasNext()){
            Float score = it.next();
            ArrayList<javaxt.utils.Record> records = searchResults.get(score);
            if (topScore==null) topScore = score;


          //Sort records within each score group
            Collections.sort(records, (javaxt.utils.Record a, javaxt.utils.Record b) -> {


              //Sort by population
                Integer population = a.get("population").toInteger();
                Integer population2 = b.get("population").toInteger();
                if (population!=null || population2!=null){

                  //If one of the places is important enough to have a
                  //population, then we will prioritize it
                    if (population==null) population = 0;
                    if (population2==null) population2 = 0;
                    return population2.compareTo(population);
                }


              //Sort adminLevel so smaller values appear first
                Integer adminLevel = a.get("adminLevel").toInteger();
                if (adminLevel==null) adminLevel = 20;

                Integer adminLevel2 = b.get("adminLevel").toInteger();
                if (adminLevel2==null) adminLevel2 = 20;

                int x = adminLevel.compareTo(adminLevel2);
                if (x!=0) return x;


              //Sort by area
                Double area = a.get("area").toDouble();
                if (area==null) area = 0.0;

                Double area2 = b.get("area").toDouble();
                if (area2==null) area2 = 0.0;

                return area2.compareTo(area);
            });

            for (javaxt.utils.Record record : records){
                record.set("score", score/topScore);
                results.add(record);
            }
        }

        return results;
    }


  //**************************************************************************
  //** getIndexSearcher
  //**************************************************************************
    private IndexSearcher getIndexSearcher() {
        synchronized (smonitor) {
            if (indexSearcher == null) {
                try {
                    directoryReader = DirectoryReader.open(dir);
                    indexSearcher = new IndexSearcher(directoryReader);
                }
                catch (Exception e) {
                    //console.log("ERROR: " + e);
                }
            }
            else {
                try {
                    DirectoryReader directoryReaderTemp = DirectoryReader.openIfChanged(directoryReader);
                    if(directoryReaderTemp != null) {
                        IndexSearcher indexSearcherTemp = new IndexSearcher(directoryReaderTemp);
                        try {
                            if(directoryReader != null) {
                                directoryReader.close();
                            }
                        } catch(Exception e) {
                            console.log("ERROR: " + e);
                        }
                        directoryReader = directoryReaderTemp;
                        indexSearcher = indexSearcherTemp;
                    }
                }
                catch (Exception e) {
                    //console.log("ERROR: " + e);
                }
            }
        }
        return indexSearcher;
    }


  //**************************************************************************
  //** getWriter
  //**************************************************************************
    public IndexWriter getWriter(){
        synchronized (wmonitor) {
            if (indexWriter == null || !indexWriter.isOpen()) {
                try {
                    IndexWriterConfig iwc = new IndexWriterConfig(perFieldAnalyzerWrapper);
                    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    indexWriter = new IndexWriter(dir, iwc);
                }
                catch (Exception e) {
                    //console.log("ERROR: " + e);
                }
            }
        }
        return indexWriter;
    }





//  //**************************************************************************
//  //** removeFile
//  //**************************************************************************
//  /** Used to remove a file from the index
//   */
//    public boolean removeFile(javaxt.io.File file) throws Exception {
//        return remove(new Term(FIELD_PATH, file.toString() ));
//    }
//
//
//  //**************************************************************************
//  //** removeDocument
//  //**************************************************************************
//  /** Used to remove a document from the index
//   */
//    public boolean removeDocument(long documentId) throws Exception {
//        return remove(new Term( FIELD_ID, documentId+"" ));
//    }
//
//
//  //**************************************************************************
//  //** remove
//  //**************************************************************************
//  /** Used to remove an entry from the index using a given search term
//   */
//    private boolean remove(Term term) throws Exception {
//        BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
//        bqBuilder.add(new TermQuery(term), Occur.MUST);
//        IndexWriter writer = getWriter();
//        writer.deleteDocuments(bqBuilder.build());
//        long status = writer.commit();
//        if(status == -1) return false;
//        return true;
//    }


  //**************************************************************************
  //** hasID
  //**************************************************************************
  /** Returns true if an item is in the index
   */
    public boolean hasID(long id) {
        if (indexExists()) {
            IndexSearcher searcher = getIndexSearcher();

            if (searcher != null) {
                try {
                    TopDocs results = searcher.search(new TermQuery(new Term(FIELD_ID, id+"")), 1);
                    if (results.totalHits.value > 0) {
                        return true;
                    }
                }
                catch (Exception e) {}
            }
        }
        return false;
    }

    private boolean indexExists() {
        try {
            return DirectoryReader.indexExists(dir);
        }
        catch (Exception e) {
            return false;
        }
    }


    private CharArraySet noStopWords() {
        return new CharArraySet(new ArrayList<>(), false);
    }
}