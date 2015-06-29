package ir.pi0.irproject;

public class Consts {

    public final static String DB_TAG = "<مقاله>";

    public static final int PROCESSOR_WORKERS;

    public static int SUB_CLUSTERS =2;

    static {
        int p = Runtime.getRuntime().availableProcessors();
        if (p > 3)
            p -= 2; /*IO Workers*/
        PROCESSOR_WORKERS = p;
    }

    public final static int BEST_BUFFER_SIZE = 1024;

    public final static int QUEUE_MAX = PROCESSOR_WORKERS * 2/* Reading is slower than processing! */;

    public static final int MIN_SORT_MEM = 1024 * 1024 * 32;

    public static final String STOPWORDS_FILE = "data/persian_stopwords.txt";
    public static final String VERBS_FILE = "data/persian_verbs.txt";

    public static final String WORDS_FILE = "data/persian_words.txt";
    public static final String BENCHMARK_BASE = "data/processed/benchmark_";

//    public static String waiting_tag = " [ #WAITING FOR QUEUE# ]";

    public static final int PREDICTED_INITIAL_WORDS_COUNT = 500000;

    public static final int WORD_POSTINGS_LRU_SIZE = PREDICTED_INITIAL_WORDS_COUNT/500;

    public static final int POSTINGS_L1_Index_SIZE = PREDICTED_INITIAL_WORDS_COUNT/100;
    //492442 unique words for our standard db


}
