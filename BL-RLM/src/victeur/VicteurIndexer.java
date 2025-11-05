package victeur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.parser.ParseException;


public class VicteurIndexer {
    String               collectionPath;
    String               indexPath;
    String               stopWordPath;
    EnglishAnalyzer      analyzer;
    static IndexWriter   writer;
    static int           docCount;
    List<String>         stopWordList;
    
    
    public VicteurIndexer(String collectionPath, String indexPath) throws IOException {

        this.collectionPath = collectionPath;
        this.indexPath = indexPath;
        stopWordPath = "/home/suchana/smart-stopwords";
        
        //for using default stopwordlist
//        analyzer = new EnglishAnalyzer();                                       //org.apache.lucene.analysis.en.EnglishAnalyzer; this uses default stopword list
        //for using external stopword list
        stopWordList = getStopwordList(stopWordPath);                         
        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopWordList)); // org.apache.lucene.analysis.core.StopFilter
        
        Directory dir;                                                          // org.apache.lucene.store.Directory

        // FSDirectory.open(file-path-of-the-directory)
        dir = FSDirectory.open((new File(this.indexPath)).toPath());          // org.apache.lucene.store.FSDirectory

        IndexWriterConfig iwc;                                                // org.apache.lucene.index.IndexWriterConfig
        iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);                   // other options: APPEND, CREATE_OR_APPEND

        writer = new IndexWriter(dir, iwc);
        docCount = 0;
    }
    
    private List<String> getStopwordList(String stopwordPath) {
        
        List<String> stopwords = new ArrayList<>();
        String line;

        try {
            System.out.println("Stopword Path: "+ stopwordPath);
            FileReader fr = new FileReader(stopwordPath);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null)
                stopwords.add(line.trim());
            br.close();
            fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n" + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n" + "Stopword file not found in: "+stopwordPath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n" + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n" + "IOException occurs");
            System.exit(1);
        }
        return stopwords;
    }

    private void createIndex(String collectionPath) throws FileNotFoundException, IOException, NullPointerException, ParseException {
        
        System.out.println("Indexing started...");
        File colFile = new File(collectionPath);
        if(colFile.isDirectory())
            collectionDirectory(colFile);
        else
            indexFile(colFile);
   }

    private void collectionDirectory(File colDir) throws FileNotFoundException, IOException, NullPointerException, ParseException {
        
        File[] files = colDir.listFiles();
        for (File file : files) {
            System.out.println("Indexing file : " + file);
            if (file.isDirectory()) {
                System.out.println("It has subdirectories...\n");
                collectionDirectory(file);  // calling this function recursively to access all the subfolders in the directory
            }
            else
                indexFile(file);
        }
    }
    
    private void indexFile(File colFile) throws FileNotFoundException, IOException, ParseException {
        
        BufferedReader reader;
        String[] docDetails = null;
        String analyzedText = null;
        Document doc;
        
        try{
            reader = new BufferedReader(new FileReader(colFile));
            String line = reader.readLine();
            while (line != null) {
		System.out.println("The whole document ********* : " + line);
                
                doc = new Document();
                docDetails = line.split("\t");
                System.out.println("Document text ++++++++++++ :" + docDetails[3]);
                analyzedText = analyzeText(analyzer, cleanText(docDetails[3]), "content").toString();
                System.out.println("Analyzed text ######### : " + analyzedText);
                line = reader.readLine();
                doc.add(new Field("docId", docDetails[0], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
                doc.add(new Field("paraId", docDetails[1], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
                doc.add(new Field("year", docDetails[2], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
                doc.add(new Field("content", docDetails[3], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
                writer.addDocument(doc);
                System.out.println("Indexed doc no. : " + ++docCount + "\n");
            }
            reader.close();
        }catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
    
    private String cleanText(String rawArticle){
        
        String pattern = "\\<(.*?)\\>";
        //rawArticle = rawArticle.replaceAll(pattern, "").replaceAll("[^a-zA-Z0-9\\.\\,]", " ").trim().replaceAll(" +", " ");
        rawArticle = rawArticle.replaceAll(pattern, "").trim().replaceAll(" +", " ");
//        System.out.println("++++++++ : " + rawArticle);
        
        return rawArticle;
    }  
    
    public static StringBuffer analyzeText(Analyzer analyzer, String text, String fieldName) throws IOException {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    }
    
    public static void main(String[] args) throws IOException, FileNotFoundException, NullPointerException, ParseException {

        String collectionPath, indexPath;
//        if(args.length!=2) {
//            System.out.println("Usage: java backgroundLinking.WaPoIndexByParagraph <collection-path> <index-path>");
//            exit(0);
//        }
        args = new String[2];
        args[0] = "/store/collection/BL_fic_nonfic.csv";
        args[1] = "/store/index/BL_F-NF_index/";
        
        collectionPath = args[0];
        indexPath = args[1];
        VicteurIndexer vicindex = new VicteurIndexer(collectionPath, indexPath);

        vicindex.createIndex(vicindex.collectionPath);
        writer.close();
        System.out.println("Complete indexing... : Total indexed documents : " + docCount);
    }
}