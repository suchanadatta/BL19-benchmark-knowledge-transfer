package common;

/**
 *
 * @author dwaipayan
 */

public class CommonVariables {
    /**
     * The unique document id of each of the documents.
     */
//    static final public String FIELD_ID = "docid";
//    static final public String FIELD_ID = "id"; // for msmarco index
    static final public String DOC_ID = "docId"; // for BL index
    static final public String PARA_ID = "paraId"; // for BL
    
    /**
     * The analyzed content of each of the documents.
     */
    static final public String FIELD_BOW = "content"; // trec + BL index
//    static final public String FIELD_BOW = "words"; // for msmarco index
    /**
     * Analyzed full content (including tag informations): Mainly used for WT10G initial retrieval.
     */
    static final public String FIELD_FULL_BOW = "full-content";

    /**
     * The meta content, that is removed from the the full-content to get the cleaned-content.
     */
    static final public String FIELD_META = "meta-content";
    
    /**
     * The news category of WashingtonPost corpus
     */
    //static final public String FIELD_CAT = "category";
}
