package ir.pi0.irproject.repository;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.proecessors.QueryParser;
import ir.pi0.irproject.structures.LRUCache;
import ir.pi0.irproject.utils.Util;
import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WordDict {

    public final static String csvHeader_word_repeat = "word_id,word,repeats,in_articles";

    protected final TObjectIntHashMap<String> word_id_map
            = new TObjectIntHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntIntMap word_repeats
            = new TIntIntHashMap(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntIntMap word_repeats_in_article
            = new TIntIntHashMap(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntObjectHashMap<TIntIntHashMap> article_words
            = new TIntObjectHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntObjectHashMap<TIntDoubleHashMap> article_word_weights
            = new TIntObjectHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final LRUCache<Integer, WordPosting> word_postings;

    public final File word_repeat_file;
    public final File article_words_dir;
    public final File word_postings_dir;
    public final File clusters_dir;

    AtomicInteger last_word_id = new AtomicInteger(0);

    boolean sync_postings;

    QueryParser queryParser = new QueryParser(this);

    List<Cluster> clusters = new ArrayList<>();

    public WordDict(File db, boolean purge_old_data, boolean sync_postings) {
        this(db, purge_old_data, sync_postings, true);
    }

    public WordDict(File db, boolean purge_old_data, boolean sync_postings, boolean load_data) {

        this.sync_postings = sync_postings;
        this.word_repeat_file = db;


        if (purge_old_data && db.exists())
            db.delete();

        File data_root_dir = new File(db.getParent(), db.getName() + ".data");
        if (!data_root_dir.exists())
            data_root_dir.mkdirs();
        else if (purge_old_data)
            Util.deleteRecursive(data_root_dir);

        this.article_words_dir = new File(data_root_dir, "article_words");
        if (!article_words_dir.exists())
            article_words_dir.mkdirs();

        this.clusters_dir = new File(data_root_dir, "clusters");
        if (!clusters_dir.exists())
            clusters_dir.mkdirs();

        this.word_postings_dir = new File(data_root_dir, "word_postings");
        if (sync_postings) {
            if (!word_postings_dir.exists())
                word_postings_dir.mkdirs();
        }

        //Create posting root dirs
        if (purge_old_data && sync_postings)
            for (int i = 0; i < Consts.POSTINGS_L1_Index_SIZE; i++)
                new File(word_postings_dir, String.valueOf(i)).mkdir();


        word_postings = new LRUCache<>(Consts.WORD_POSTINGS_LRU_SIZE, new Consumer<WordPosting>() {
            @Override
            public void accept(WordPosting wordPosting) {
                wordPosting.flush();
            }
        });

        //(Debug)
        System.gc();
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        //-------------------------
        if (!purge_old_data && load_data) {
            System.out.println("Reading Words database ...");

            BufferedReader r;
            try {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(db)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            String l;
            try {
                l = r.readLine();
                if (!csvHeader_word_repeat.equals(l))
                    System.err.println("Warning: Invalid csv header");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                while ((l = r.readLine()) != null) {
                    String[] split = l.split(",");
                    int id = Integer.parseInt(split[0]);
                    int repeats = Integer.parseInt(split[2]);
                    addItem(split[1], id, repeats);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            System.out.println("Reading clusters");

            this.clusters.clear();

            for (File f : clusters_dir.listFiles()) {
                int id = Integer.parseInt(f.getName());
                Cluster c = new Cluster(this, id);
                c.loadFromFile(false);//Don't load subclusters by default for memory efficiency
                clusters.add(c);
            }
        }
        //-------------------------

        //(Debug)
        System.gc();
        long heapFreeSize2 = Runtime.getRuntime().freeMemory();
        System.out.format("Heap usage: %s \n",
                Util.humanReadableByteCount((int) Math.abs(heapFreeSize - heapFreeSize2), false));
    }


    public void save() {
        if (article_words_dir == null)
            return;

        try {

            //-------------------------

            System.out.println("Saving Words");

            final BufferedWriter w =
                    new BufferedWriter(new FileWriter(word_repeat_file, false/*don't append*/));
            w.write(csvHeader_word_repeat);
            w.write("\r\n");

            TObjectIntIterator i = word_id_map.iterator();
            for (int j = word_id_map.size(); j-- > 0; ) {
                i.advance();
                Integer word_id = word_id_map.get(i.value());
                Integer repeats = word_repeats.get(word_id);
                Integer repeats_arc = word_repeats_in_article.get(word_id);
                try {
                    w.write(String.valueOf(word_id) + ',' + i.key() + ',' + repeats + "," + repeats_arc + "\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //-------------------------

            System.out.println("Flushing remaining Articles");
            synchronized (article_words) {
                TIntObjectIterator<TIntIntHashMap> i2 = article_words.iterator();
                for (; i2.hasNext(); ) {
                    flush_article(i2.key(), true, true);
                }
            }

            System.out.println("Flushing all article postings");

            word_postings.forEach(new BiConsumer<Integer, WordPosting>() {
                @Override
                public void accept(Integer integer, WordPosting wordPosting) {
                    wordPosting.flush();
                }
            });

            //-------------------------

            System.out.println("Save done");

            w.flush();
            w.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TIntDoubleHashMap calculate_weight(TIntIntHashMap article) {

        final TIntDoubleHashMap ws = new TIntDoubleHashMap();

        final double logN = Util.log2(word_repeats.size());

        //Find maximum repeats in this article
        int max_repeats = 0;
        for (int r : article.values())
            if (r > max_repeats)
                max_repeats = r;

        //pre-calculate
        final double InvNMax = 1.0 / max_repeats;

        //for each word, calculate Wij
        TIntIntIterator i = article.iterator();
        for (int j = article.size(); j-- > 0; ) {
            i.advance();
            int key = i.key();
            double logRepeatInArticles = Util.log2(word_repeats_in_article.get(key));
            double w = i.value() * InvNMax * (logN - logRepeatInArticles);
            ws.put(key, w);
        }


        return ws;
    }

    public void calculate_weights() {

        System.out.println("Calculate weights");

        final int[] articles = list_articles();

        //(debug)
        long startTime = System.currentTimeMillis();
        long heapFreeSize_min = Runtime.getRuntime().totalMemory();
        double p, last_p = -1;

        for (int i = 0; i < articles.length; i++) {
            final int article_id = articles[i];

            //(Debug)
            p = (i * 1.0) / articles.length;
            if (p - last_p > .001) {
                Util.clearLine();
                Util.printProgress(p, System.currentTimeMillis() - startTime, false, true, "Progress");
                last_p = p;
            }
            long heapFreeSize = Runtime.getRuntime().freeMemory();
            if (heapFreeSize < heapFreeSize_min)
                heapFreeSize_min = heapFreeSize;


            //Load article
            TIntIntHashMap article = load_article(article_id, false).getKey();

            //Open article file for rewriting
            //Now the article file is empty !!!
            final Writer writer;
            try {
                writer = new BufferedWriter(new FileWriter(
                        new File(article_words_dir, String.valueOf(article_id)), false));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //Calculate doc
            final TIntDoubleHashMap weights = calculate_weight(article);

            TIntIntIterator it = article.iterator();
            for (int j = article.size(); j-- > 0; ) {
                it.advance();
                int word_id = it.key();
                double w = weights.get(word_id);
                try {
                    writer.append(String.format("%d,%d,%f\n", word_id, it.value(), w));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        //(debug)
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        Util.clearLine();
        System.out.println();
        System.out.printf("Max heap usage during process : %s\n",
                Util.humanReadableByteCount(Runtime.getRuntime().totalMemory() - heapFreeSize_min));
        System.out.printf("Took : %s\n", Util.getDurationBreakdown(elapsedTime, true));

    }


    public void cluster_articles() {
        int[] articles = this.list_articles();

        //Pick sqrt(n) random articles as leaders

        int clusters_count = (int) (Math.sqrt((double) articles.length));

        Cluster[] clusters = new Cluster[clusters_count];

        int inc = articles.length / clusters_count;

        //Initialize clusters
        for (int i = 0; i < clusters_count; i++) {
            clusters[i] = new Cluster(this, i + 1);
            clusters[i].add(inc * i, null);
        }

        //Now distribute all articles among leaders

        //(debug)
        double p, last_p = -1;
        long startTime = System.currentTimeMillis();
        long heapTotal = Runtime.getRuntime().totalMemory();
        long heapFreeSize_min = heapTotal;


        for (int i = 0; i < articles.length; i++) {

            if (i % inc == 0)
                continue;//Skip leaders !!

            final int article_id = articles[i];
            TIntDoubleHashMap article = load_article(article_id, false).getValue();

            //(Debug)
            long heapFreeSize = Runtime.getRuntime().freeMemory();
            if (heapFreeSize < heapFreeSize_min)
                heapFreeSize_min = heapFreeSize;
            p = (i * 1.0) / articles.length;
            if (p - last_p > .001) {
                Util.clearLine();
                Util.printProgress(p, System.currentTimeMillis() - startTime, false, true, "Clustering");
                Util.printProgress(1 - (heapFreeSize * 1.0 / heapTotal), 0, false, false, "Heap usage");
                last_p = p;
            }

            //Compare it to all leaders
            double best_match = -1;
            Cluster best_match_c = clusters[0];
            for (Cluster cluster : clusters) {
                if (!cluster.active)
                    continue;
                if (cluster.getSize() > inc) {
                    cluster.saveToFile();
                    cluster.discardSubClusters();
                    cluster.active = false;
                    System.out.println("\nCluster " + cluster.id + " full & saved");
                    System.gc();
                    continue;
                }

                double score = cluster.compareToArticle(article);

                if (score > best_match) {
                    best_match = score;
                    best_match_c = cluster;
                }

            }
            best_match_c.add(article_id, article);
        }

        //Now save all clusters
        System.out.println("\nRemoving all old cluster files");
        Util.deleteRecursive(clusters_dir);
        clusters_dir.mkdirs();

        System.out.println("Saving all clusters");
        for (Cluster c : clusters)
            if (c.active)
                c.saveToFile();
        System.out.println("Save done");

    }


    public static double articleCompare(TIntDoubleHashMap doc1, TIntDoubleHashMap doc2) {

        if (doc1.size() > doc2.size()) {
            TIntDoubleHashMap tmp = doc1;
            doc1 = doc2;
            doc2 = tmp;
        }

        double score = 0;

        double no_entity = doc1.getNoEntryValue();

        for (int key : doc1.keys()) {
            double val2 = doc2.get(key);
            if (val2 == no_entity)
                continue;
            double val1 = doc1.get(key);
            score += val1 * val2;
        }

        return score;
    }

    public List<Integer> query(String query, int limit) {

        System.out.println("Do Query");


        //(debug)
        long startTime = System.currentTimeMillis();
        long heapFreeSize_min = Runtime.getRuntime().totalMemory();
        double p, last_p = -1;

        final TIntDoubleHashMap query_doc = queryParser.get_weights(query);

        //-------------------------
        //L1 -- No limit
        TreeSet<QueryResult> results_l1 = new TreeSet<>();/*TreeSet is sorted*/
        for (Cluster c : clusters) {
            results_l1.add(new QueryResult(c.compareToArticle(query_doc), c));
        }

        //L2
        TreeSet<QueryResult> results_l2 = new TreeSet<>();
        int sz = 0;
        while (results_l1.size() > 0 && sz < limit) {
            QueryResult r = results_l1.pollLast();
            sz += r.cluster.size;
            System.out.println("Debug: Scanning cluster " + r.cluster.id);
            for (SubCluster sc : r.cluster.getSubClusters()) {
                results_l2.add(new QueryResult(sc.compareToArticle(query_doc), sc));
            }
            r.cluster.discardSubClusters();//Don't waste memory
        }

        //L3
        final TreeSet<QueryResult> results_l3 = new TreeSet<>();
        while (results_l2.size() > 0 && results_l3.size() < limit) {
            QueryResult r2 = results_l2.pollLast();

            TIntIterator i = r2.subCluster.articles.iterator();
            for (int j = r2.subCluster.articles.size(); j-- > 0; ) {
                int article_id = i.next();
                TIntDoubleHashMap article = load_article(article_id, false).getValue();
                results_l3.add(new QueryResult(articleCompare(query_doc, article), article_id));
            }

        }

        //Final
        List<Integer> results = new ArrayList<>(limit);
        while (results_l3.size() > 0 && results.size() < limit) {
            QueryResult result = results_l3.pollLast();
            results.add(result.article_id);
        }

        //-------------------------

        //(debug)
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println();
        System.out.printf("Max heap usage during process : %s\n",
                Util.humanReadableByteCount(Runtime.getRuntime().totalMemory() - heapFreeSize_min));
        System.out.printf("Took : %s\n", Util.getDurationBreakdown(elapsedTime, true));

        return results;
    }


    public void addItem(String word, Integer id, Integer repeats) {
        if (id > last_word_id.get())
            last_word_id.set(id + 1);
        word_id_map.put(word, id);
        word_repeats.put(id, repeats);
    }


    public WordPosting getWordPosting(int word_id) {
        WordPosting p;
        synchronized (word_postings) {
            p = word_postings.get(word_id);
        }

        if (p == null) {
            String filename = (word_id % Consts.POSTINGS_L1_Index_SIZE) + "/"
                    + (word_id / Consts.POSTINGS_L1_Index_SIZE);
            p = new WordPosting(new File(word_postings_dir, filename));
            synchronized (word_postings) {
                word_postings.put(word_id, p);
            }
        }

        return p;
    }

    public Integer getWordID(String word) {
        int id = word_id_map.get(word);
        if (id != word_id_map.getNoEntryValue())
            return id;
        return null;
    }

    public Integer getWordRepeats(int id) {
        return word_repeats.get(id);
    }

    public Integer getWordRepeats(String word) {
        int word_id = word_id_map.get(word);
        if (word_id == word_id_map.getNoEntryValue())
            return null;
        return word_repeats.get(word_id);
    }

    public void increment(String word, int article_id, int by) {
        if (word.length() == 0)
            return;

        //Get word_id
        Integer word_id;
        synchronized (word_id_map) {
            word_id = word_id_map.get(word);
            if (word_id == word_id_map.getNoEntryValue())
                word_id_map.put(word, word_id = last_word_id.incrementAndGet());
        }

        //Adjust total repeats
        synchronized (word_repeats) {
            word_repeats.adjustOrPutValue(word_id, by, by);
        }

        //Find article
        TIntIntHashMap article = getArticleAndCache(article_id);
        article.adjustOrPutValue(word_id, by, by);


    }


    public void flush_article(final int article_id, final boolean update_postings, boolean save) {
        TIntIntHashMap article = getArticleAndCache(article_id);
        final TIntDoubleHashMap article_weights = article_word_weights.get(article_id);

        if (article == null)
            return;

        File article_words_file = new File(article_words_dir, String.valueOf(article_id));

        try {

            //Save article words
            if (save) {
                final BufferedWriter w = new BufferedWriter(new FileWriter(article_words_file));
                synchronized (article) {
                    TIntIntIterator it = article.iterator();
                    for (int j = article.size(); j-- > 0; ) {
                        it.advance();
                        int word_id = it.key();
                        int word_repeats=it.value();

                        try {

                            if (article_weights.containsKey(word_id))
                                w.write(word_id + "," + word_repeats + "," + article_weights.get(word_id) + "\n");
                            else
                                w.write(word_id + "," + word_repeats + "\n");

                            if (update_postings && sync_postings) {
                                //Also update postings
                                getWordPosting(word_id).append(article_id);
                            }

                            word_repeats_in_article.adjustOrPutValue(word_id, 1, 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                w.close();
            }

            this.article_words.remove(article_id);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Pair<TIntIntHashMap, TIntDoubleHashMap> load_article(int article_id, boolean cache) {
        TIntIntHashMap article = new TIntIntHashMap();
        TIntDoubleHashMap article_weight = new TIntDoubleHashMap();

        File file = new File(article_words_dir, String.valueOf(article_id));

        if (file.exists()) {
            try {
                final BufferedReader r = new BufferedReader(new FileReader(file));

                String l;
                while ((l = r.readLine()) != null) {
                    String[] ls = l.split(",");
                    int word_id = Integer.parseInt(ls[0]);
                    article.put(word_id, Integer.parseInt(ls[1]));
                    if (ls.length > 2)
                        article_weight.put(word_id, Double.parseDouble(ls[2]));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (cache)
            synchronized (article_words) {
                article_words.put(article_id, article);
                article_word_weights.put(article_id, article_weight);
            }

        return new Pair<>(article, article_weight);
    }


    public TIntIntHashMap getArticleAndCache(int article_id) {
        TIntIntHashMap m;
        synchronized (article_words) {
            m = article_words.get(article_id);
        }
        if (m == null)
            m = load_article(article_id, true).getKey();//Magically load it from disk
        return m;
    }


    public TIntDoubleHashMap getArticleWeightAndCache(int article_id) {
        TIntDoubleHashMap m;
        synchronized (article_words) {
            m = article_word_weights.get(article_id);
        }
        if (m == null)
            m = load_article(article_id, true).getValue();//Magically load it from disk
        return m;
    }


    public int[] list_articles() {
        File[] files = article_words_dir.listFiles();
        if (files == null)
            return new int[0];

        int[] ids = new int[files.length];

        for (int i = 0; i < files.length; i++) {
            ids[i] = Integer.parseInt(files[i].getName());
        }

        return ids;
    }

}