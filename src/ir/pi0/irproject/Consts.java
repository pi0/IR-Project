package ir.pi0.irproject;

public class Consts {

    public final static String DB_TAG = "<مقاله>";

    public final static int BEST_BUFFER_SIZE = 1024 ;

    public final static int QUEUE_MAX = 150;

    public static final int MIN_SORT_MEM = 1024 * 1024 * 16;

    public static final int PROCESSOR_WORKERS = 2;

    public static final String STOPWORDS_FILE = "data/persian_stopwords.txt";
    public static final String VERBS_FILE  = "data/persian_verbs.txt";

    public static final String WORDS_FILE= "data/persian_words.txt";
    public static final String BENCHMARK_BASE = "data/processed/benchmark_";

    public static String waiting_tag=" [ #WAITING FOR QUEUE# ]";
}
