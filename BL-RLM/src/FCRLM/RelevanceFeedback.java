package FCRLM;

import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.DOC_ID;
import static common.CommonVariables.PARA_ID;
import common.EnglishAnalyzerWithSmartStopword;
import common.TRECQuery;
import common.TRECQueryParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelIF;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class RelevanceFeedback {

    Properties      prop;
    String          indexPath;
    String          queryPath;               // path of the query file
    File            queryFile;               // the query file
    String          rankFilePath;            // pre-stored doc rank list
    File            rankFile;                // pre-rank file
    String          stopFilePath;
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          resPath;                 // path of the res file
    FileWriter      resFileWriter;
    FileWriter      expendedQueryFile;
    int             numHits;                 // number of document to retrieveWithExpansionTermsFromFile
    String          runName;                 // name of the run
    List<TRECQuery> queries;
    File            indexFile;               // place where the index is stored
    Analyzer        analyzer;
    String          fieldToSearch;           // the field in the index to be searched
    String          fieldForFeedback;        // field, to be used for feedback
    TRECQueryParser trecQueryparser;
    int             simFuncChoice;
    float           param1, param2;
    //RM3             rm3;                   // reference to RM3 to use Relevance Feedback Model
    FactoredRLM     frlm;    
    float           mixingLambda;            // mixing weight, used for doc-col weight distribution
    int             numFeedbackTerms; // number of feedback terms at the first step
    int             numFeedbackDocs;         // number of feedback documents
    float           QMIX;                    // query mix to weight between P(w|R) and P(w|Q)
    Map<String, TopDocs> topDocsMap;
    

    public RelevanceFeedback(Properties prop) throws IOException, Exception {

        this.prop = prop;
        /* property file loaded */

        /* setting the analyzer with English Analyzer with Smart stopword list */
        EnglishAnalyzerWithSmartStopword engAnalyzer;
        stopFilePath = prop.getProperty("stopFilePath");
        if (null == stopFilePath)
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword();
        else
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        /* analyzer set: analyzer */

        /* index path setting */
        indexPath = prop.getProperty("indexPath");
        System.out.println("indexPath set to: " + indexPath);
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            System.exit(1);
        }
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_BOW);
        fieldForFeedback = prop.getProperty("fieldForFeedback", FIELD_BOW);

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));

        /* setting indexReader and indexSearcher */
        indexReader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()));
        indexSearcher = new IndexSearcher(indexReader);
        setSimilarityFunction(simFuncChoice, param1, param2);
        /* indexReader and searcher set */

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        trecQueryparser = new TRECQueryParser(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed the query */

        /* numFeedbackTerms = number of top terms to select in two steps */
        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
        /* numFeedbackDocs = number of top documents to select */
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));

        /* setting mixing Lambda */
        if(param1>0.99)
            mixingLambda = 0.8f;
        else
            mixingLambda = param1;

        numHits = Integer.parseInt(prop.getProperty("numHits","1000"));
        QMIX = Float.parseFloat(prop.getProperty("rm3.queryMix"));
        
        frlm = new FactoredRLM(this);
        
        /* write expanded queries */
        expendedQueryFile = new FileWriter(prop.getProperty("resPath") + "expanded.query");
        System.out.println("Expanded queries will be stored in: " + prop.getProperty("resPath") + "expanded.query");

        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */
        
        /* pre-rank file path */
//        rankFilePath = "/store/adaptive_feedback/monot5_base_rerank/bm25.monot5-base.dl.1000.run.sorted";
//        rankFile = new File(rankFilePath);
    }
    

    /**
     * Sets indexSearcher.setSimilarity() with parameter(s)
     * @param choice similarity function selection flag
     * @param param1 similarity function parameter 1
     * @param param2 similarity function parameter 2
     */
    private void setSimilarityFunction(int choice, float param1, float param2) {

            switch(choice) {
            case 0:
                indexSearcher.setSimilarity(new DefaultSimilarity());
                System.out.println("Similarity function set to DefaultSimilarity");
                break;
            case 1:
                indexSearcher.setSimilarity(new BM25Similarity(param1, param2));
                System.out.println("Similarity function set to BM25Similarity"
                    + " with parameters: " + param1 + " " + param2);
                break;
            case 2:
                indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                System.out.println("Similarity function set to LMJelinekMercerSimilarity"
                    + " with parameter: " + param1);
                break;
            case 3:
                indexSearcher.setSimilarity(new LMDirichletSimilarity(param1));
                System.out.println("Similarity function set to LMDirichletSimilarity"
                    + " with parameter: " + param1);
                break;
            case 4:
                indexSearcher.setSimilarity(new DFRSimilarity(new BasicModelIF(), new AfterEffectB(), new NormalizationH2()));
                System.out.println("Similarity function set to DFRSimilarity with default parameters");
                break;
        }
    } // ends setSimilarityFunction()
    

    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName_ResFileName() {

        Similarity s = indexSearcher.getSimilarity(true);
        runName = s.toString()+"-D"+numFeedbackDocs+"-T"+numFeedbackTerms;
        runName += "-queryMix-"+QMIX;
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");
        if(null == prop.getProperty("resPath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath+queryFile.getName()+"-"+runName + ".res";
    } // ends setRunName_ResFileName()

    /**
     * Parses the query from the file and makes a List<TRECQuery> 
     *  containing all the queries (RAW query read)
     * @return A list with the all the queries
     * @throws Exception 
     */
    private List<TRECQuery> constructQueries() throws Exception {
        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    } // ends constructQueries()
    
    public Map<String, TopDocs> loadResFile(File resFile) {
        Map<String, TopDocs> topDocsMap = new HashMap<>();
        try {
            String prev_qid = null, qid = null;
            RetrievedResults rr = null;
            
            BufferedReader br = new BufferedReader(new FileReader(resFile));
            String line = br.readLine();
            while(line != null){ 
                System.out.println("Line : " + line);
                String[] tokens = line.split("\\s+");
                qid = tokens[0];
                if (prev_qid!=null && !prev_qid.equals(qid)) {
                    topDocsMap.put(prev_qid, convert(rr));
                    rr = new RetrievedResults(qid);
                }
                else if (prev_qid == null) {
                    rr = new RetrievedResults(qid);
                }

                int offset = getDocOffsetFromId(tokens[2]);
                System.out.println("OFFSET : " + offset);
                int rank = Integer.parseInt(tokens[3]);
                double score = Float.parseFloat(tokens[4]);

                rr.addTuple(String.valueOf(offset), rank, score);
                prev_qid = qid;
                line = br.readLine(); 
            }
            if (qid!=null)
                topDocsMap.put(qid, convert(rr));
            }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return topDocsMap;
    } 
    
    public int getDocOffsetFromId(String docId) throws Exception {
        TopScoreDocCollector collector;
        TopDocs topDocs;
        collector = TopScoreDocCollector.create(numHits);
        Query query = new TermQuery(new Term(DOC_ID, docId));
        System.out.println("Initial query: " + query.toString());
        indexSearcher.search(query, collector);
        topDocs = collector.topDocs();
        System.out.println("docs retrieved : " + topDocs.totalHits);
        return topDocs.scoreDocs[0].doc;
    }

    private TopDocs convert(RetrievedResults rr) {
        int nret = rr.getNumRet();
        ScoreDoc[] sd = new ScoreDoc[nret];

        int i = 0;
        for (ResultTuple resultTuple: rr.getTuples()) {
            sd[i++] = new ScoreDoc(
                Integer.parseInt(resultTuple.getDocName()),
                (float)(resultTuple.getScore())
            );
        }
        return new TopDocs(numHits, sd, 100);
    }

    public void retrieveAll() throws Exception {

        ScoreDoc[] hits;
        TopDocs topDocsPRD1, topDocsPRD2, topDocsFinal;
        TopScoreDocCollector collector;
        HashMap<String, WordProbability> hashmap_PwGivenR, hashmap_PwGivenR_causal;

        for (TRECQuery query : queries) {
            
            
            collector = TopScoreDocCollector.create(numHits);
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);

            System.out.println("\n" + query.qid +": Initial query: " + luceneQuery.toString());

            /* PRF - initial retrieval performed */
            indexSearcher.search(luceneQuery, collector);
            topDocsPRD1 = collector.topDocs();
            System.out.println("docs retrieved : " + topDocsPRD1.totalHits);
            hits = topDocsPRD1.scoreDocs;
                if(hits == null)
                System.out.println("Nothing found");

//            int hits_length = hits.length;
            /* PRF */

            StringBuffer resBuffer, queryBuffer;
//            
            frlm.setFeedbackStats(topDocsPRD1, luceneQuery.toString(fieldToSearch).split(" "), this);
            
            /**
             * HashMap of P(w|R) for 'numFeedbackTerms' terms with top P(w|R) among each w in R,
             * keyed by the term with P(w|R) as the value.
             * T1 = normalized RM1(D1)--<sorted> 
             * T1'= normalized top n terms of T1--<with highest weights>
             * EQ1 = RM3(T1',Q,alpha) and retrieve
             */
            
//            hashmap_PwGivenR = frlm.RM1(query, topDocsPRD1);

            hashmap_PwGivenR = frlm.RM3(query, topDocsPRD1, luceneQuery.toString(fieldToSearch).split(" "));
            System.out.println("Total expanded terms : " + hashmap_PwGivenR.size());
//            /* EQ1 = RM3(T1',Q, alpha) */
//            
            BooleanQuery booleanQuery;

            booleanQuery = frlm.getExpandedQuery(hashmap_PwGivenR, query);
//            System.out.println("Re-retrieval after 1st level estimation with EQ1 :" + booleanQuery);
            System.out.println("Expansion terms : " + booleanQuery.toString(fieldToSearch));
            String [] boolQuery = booleanQuery.toString(fieldToSearch).replaceAll("\\^", "\t").split(" ");
            Arrays.sort(boolQuery); 
//            for (String expTerm : boolQuery)
//                System.out.println(expTerm);
            collector = TopScoreDocCollector.create(numHits);
            indexSearcher.search(booleanQuery, collector);      //retrieve with EQ1
            
            /* D2 = top k docs of search (EQ1,C) */
            topDocsPRD2 = collector.topDocs(); 
            hits = topDocsPRD2.scoreDocs;
                if(hits == null)
                System.out.println("Nothing found");

            int hits_length = hits.length;

            resFileWriter = new FileWriter(resPath, true);

            /* res file in TREC format with doc text (7 columns) */
            resBuffer = new StringBuffer();
            for (int i = 0; i < hits_length; ++i) {
                int docId = hits[i].doc;
                Document d = indexSearcher.doc(docId);
//                resBuffer.append(query.qid).append("\tQ0\t").
//                append(d.get(DOC_ID)).append("\t").
//                append(d.get(FIELD_ID)).append("\t").
//                append((i+1)).append("\t").
//                append(hits[i].score).append("\t").
//                append(runName).append("\n");

                resBuffer.append(query.qid).append("\tQ0\t").
//                append(d.get(DOC_ID)).append("\t").
                append(d.get(PARA_ID)).append("\t").
                append((i+1)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
            
            /* write expanded queries */
            
            expendedQueryFile = new FileWriter(prop.getProperty("resPath") + "expanded.query", true);
            
            queryBuffer = new StringBuffer();
            queryBuffer.append(query.qid).append("\t");
            for (String qTerm : hashmap_PwGivenR.keySet() ) {
                queryBuffer.append(qTerm).append(" ");
            }
            queryBuffer.append("\n");
            expendedQueryFile.write(queryBuffer.toString());
            expendedQueryFile.close();            
        } // ends for each query
    } // ends retrieveAll
    
    public void retrieveFromRes() throws Exception { 
        StringBuffer resBuffer, queryBuffer;
        HashMap<String, WordProbability> hashmap_PwGivenR;
        TopScoreDocCollector collector;
        TopDocs topD, topD2;
        ScoreDoc[] hits;
        
        topDocsMap = new HashMap<>();
        topDocsMap = loadResFile(rankFile);
        System.out.println("#### : " + topDocsMap.size());
        for (TRECQuery query : queries) {
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);
            System.out.println("\n" + query.qid +": Initial query: " + luceneQuery.toString());
            topD = topDocsMap.get(query.qid);
            System.out.println("VALUE : " + topD.scoreDocs.length);
            
            collector = TopScoreDocCollector.create(numHits);
            frlm.setFeedbackStats(topD, luceneQuery.toString(fieldToSearch).split(" "), this);
            hashmap_PwGivenR = frlm.RM3(query, topD, luceneQuery.toString(fieldToSearch).split(" "));
            System.out.println("Total expanded terms : " + hashmap_PwGivenR.size());
            BooleanQuery booleanQuery;
            booleanQuery = frlm.getExpandedQuery(hashmap_PwGivenR, query);
            System.out.println("Re-retrieval after 1st level estimation with EQ1 :" + booleanQuery);
            System.out.println(booleanQuery.toString(fieldToSearch));
            collector = TopScoreDocCollector.create(numHits);
            indexSearcher.search(booleanQuery, collector);      //retrieve with EQ1
            
            /* D2 = top k docs of search (EQ1,C) */
            topD2 = collector.topDocs(); 
            hits = topD2.scoreDocs;
                if(hits == null)
                System.out.println("Nothing found");
            int hits_length = hits.length;
            
            resFileWriter = new FileWriter(resPath, true);

            /* res file in TREC format with doc text (7 columns) */
            resBuffer = new StringBuffer();
            for (int i = 0; i < hits_length; ++i) {
                int docId = hits[i].doc;
                Document d = indexSearcher.doc(docId);
                resBuffer.append(query.qid).append("\tQ0\t").
                append(d.get(DOC_ID)).append("\t").
                append((i+1)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
        }
    } // ends retrieveFromRes
    

    public static void main(String[] args) throws IOException, Exception {

        String usage = "java RelevanceFeedback <properties-file>\n"
                + "Properties file must contain the following fields:\n"
                + "1. stopFilePath: path of the stopword file\n"
                + "2. indexPath: Path of the index\n"
                + "3. queryPath: path of the query file (in proper xml format)\n"
                + "4. resPath: path of the directory to store res file\n"
                + "5. numFeedbackDocs: number of feedback documents to use\n"
                + "6. numFeedbackTerms: number of feedback terms to use for feedback\n"
                + "7. rm3.queryMix (0.0-1.0): query mix to weight between P(w|R) and P(w|Q)\n"
                + "8. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n";               
                
        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "fcrlm-0.4-query_test.xml.D-10.topical-10.causal-10.properties";
            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        RelevanceFeedback rbcm = new RelevanceFeedback(prop);
        
        /* to work with index directly */
        rbcm.retrieveAll();
        
        /* to work with a pre-stored .res file */
//        rbcm.retrieveFromRes();
    } // ends main()
}
