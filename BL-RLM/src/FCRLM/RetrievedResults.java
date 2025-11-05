package FCRLM;

import java.util.ArrayList;
import java.util.List;


public class RetrievedResults {
    
    String qid;
    List<ResultTuple> rtuples;
    int numRelRet;
    float avgP;

    public RetrievedResults(String qid) {
        this.qid = qid;
        this.rtuples = new ArrayList<>(100);
        avgP = -1;
        numRelRet = -1;
    }
    
    public void addTuple(String docName, int rank, double score) {
        rtuples.add(new ResultTuple(docName, rank, score));
    }
    
    public int getNumRet() { return rtuples.size(); }
    
    public List<ResultTuple> getTuples() { return this.rtuples; }
}


