package ir.pi0.irproject.repository;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.structures.LRUCache;
import ir.pi0.irproject.utils.Util;

import java.io.*;
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

    protected final LRUCache<Integer, WordPosting> word_postings;

    private final File word_repeat_file;
    private final File article_words_dir;
    private final File word_postings_dir;

    AtomicInteger last_word_id = new AtomicInteger(0);

    boolean sync_postings;

    public WordDict(File db, boolean purge_old_data, boolean sync_postings) {

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

        this.word_postings_dir = new File(data_root_dir, "word_postings");
        if (!word_postings_dir.exists())
            word_postings_dir.mkdirs();

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
        if (!purge_old_data) {
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
            word_id_map.forEach(new TObjectProcedure<String>() {
                @Override
                public boolean execute(String word) {
                    Integer word_id = word_id_map.get(word);
                    Integer repeats = word_repeats.get(word_id);
                    Integer repeats_arc = word_repeats_in_article.get(word_id);
                    try {
                        w.write(String.valueOf(word_id) + ',' + word + ',' + repeats + "," + repeats_arc + "\n");
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            });

            //-------------------------

            System.out.println("Flushing remaining Articles");

            article_words.keySet().forEach(new TIntProcedure() {
                @Override
                public boolean execute(int value) {
                    flush_article(value, true, true);
                    return true;
                }
            });

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

    public void generate_weights() {

        System.out.println("Generating word/article weights");

        final Integer[] articles = list_articles();

        //(debug)
        long startTime = System.currentTimeMillis();
        long heapFreeSize_min = Runtime.getRuntime().totalMemory();
        double p, last_p = -1;

        //Some pre-calculations
        final double logN = Util.log2(word_repeats.size());

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
            TIntIntHashMap article = getArticle(article_id);

            //Open article file for rewriting
            final Writer writer;
            try {
                writer = new BufferedWriter(new FileWriter(
                        new File(article_words_dir, String.valueOf(article_id)), false));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //First find maximum repeats in this article
            int max_repeats = 0;
            for (int r : article.values())
                if (r > max_repeats)
                    max_repeats = r;

            //pre-calculate
            final double InvNMax = 1.0 / max_repeats;

            //for each word, calculate Wij
            article.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int word_id, int repeats) {
                    double logRepeatInArticles = Util.log2(word_repeats_in_article.get(word_id));
                    double w = repeats * InvNMax * (logN - logRepeatInArticles);

                    //Now store it
                    try {
                        writer.append(String.format("%d,%d,%d\n", word_id, repeats, (int) (w * Consts.WeightBase)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return true;
                }
            });

            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            flush_article(article_id, false, false);
        }


        //(debug)
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println();
        System.out.printf("Max heap usage during process : %s\n",
                Util.humanReadableByteCount(Runtime.getRuntime().totalMemory() - heapFreeSize_min));
        System.out.printf("Took : %s\n", Util.getDurationBreakdown(elapsedTime, true));

    }


    //----------------

    public void addItem(String word, Integer id, Integer repeats) {
        if (id > last_word_id.get())
            last_word_id.set(id + 1);
        word_id_map.put(word, id);
        word_repeats.put(id, repeats);
    }

    public TIntIntHashMap getArticle(int article_id) {
        TIntIntHashMap m;
        synchronized (article_words) {
            m = article_words.get(article_id);
        }
        if (m == null)
            m = load_article(article_id,true);//Magically load it from disk
        return m;
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
        TIntIntHashMap article = getArticle(article_id);
        article.adjustOrPutValue(word_id, by, by);


    }


    public void flush_article(final int article_id, final boolean update_postings, boolean save) {
        TIntIntHashMap article = getArticle(article_id);
        if (article == null)
            return;

        File article_words_file = new File(article_words_dir, String.valueOf(article_id));

        try {

            //Save article words
            if (save) {
                final BufferedWriter w = new BufferedWriter(new FileWriter(article_words_file));
                article.forEachEntry(new TIntIntProcedure() {
                    @Override
                    public boolean execute(int word_id, int word_repeats) {
                        try {
                            w.write(word_id + "," + word_repeats + "\n");

                            if (update_postings && sync_postings) {
                                //Also update postings
                                getWordPosting(word_id).append(article_id);
                            }

                            if (update_postings) {
                                word_repeats_in_article.adjustOrPutValue(word_id, 1, 1);
                            }

                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                });
                w.close();
            }

            this.article_words.remove(article_id);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public TIntIntHashMap load_article(int article_id, boolean cache) {
        TIntIntHashMap article = new TIntIntHashMap();
        File file = new File(article_words_dir, String.valueOf(article_id));

        if (file.exists()) {
            try {
                final BufferedReader r = new BufferedReader(new FileReader(file));

                String l;
                while ((l = r.readLine()) != null) {
                    String[] ls = l.split(",");
                    article.put(Integer.parseInt(ls[0]), Integer.parseInt(ls[1]));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (cache)
            synchronized (article_words) {
                article_words.put(article_id, article);
            }

        return article;
    }


    public Integer[] list_articles() {
        File[] files = article_words_dir.listFiles();
        if (files == null)
            return new Integer[0];

        Integer[] ids = new Integer[files.length];

        for (int i = 0; i < files.length; i++) {
            ids[i] = Integer.parseInt(files[i].getName());
        }

        return ids;
    }

}