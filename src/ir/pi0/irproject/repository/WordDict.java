package ir.pi0.irproject.repository;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;


public class WordDict {

    public final static String csvHeader_word_repeat = "word_id,word,repeats";

    protected final TObjectIntHashMap<String> word_id_map
            = new TObjectIntHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntIntMap word_repeats =
            new TIntIntHashMap(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    protected final TIntObjectHashMap<TIntIntHashMap> article_words
            = new TIntObjectHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    private File word_repeat_file = null;
    private File article_word_file = null;

    AtomicInteger last_word_id = new AtomicInteger(0);

    public WordDict(File word_repeat, File article_word) {

        this.word_repeat_file = word_repeat;
        this.article_word_file = article_word;

        System.gc();
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        //-------------------------
        if (word_repeat.exists()) {
            System.out.println("Reading Words database ...");

            BufferedReader r =
                    null;
            try {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(word_repeat)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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

        System.gc();
        long heapFreeSize2 = Runtime.getRuntime().freeMemory();
        System.out.format("Heap usage: %s \n",
                Util.humanReadableByteCount((int) (heapFreeSize - heapFreeSize2), false));
    }


    public void save() {
        if (word_repeat_file == null || article_word_file == null)
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
                    try {
                        w.write(String.valueOf(word_id) + ',' + word + ',' + repeats + "\n");
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
                    flush_article(value);
                    return true;
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
            m = load_article(article_id);//Magically load it from disk
        return m;
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


    public void flush_article(int article_id) {
        TIntIntHashMap article = getArticle(article_id);
        if (article == null)
            return;

        File out = new File(article_word_file, String.valueOf(article_id));
        try {

            final BufferedWriter w = new BufferedWriter(new FileWriter(out));

            article.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int key, int val) {
                    try {
                        w.write(key + "," + val + "\n");
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            });

            w.close();

            article_words.remove(article_id);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public TIntIntHashMap load_article(int article_id) {
        TIntIntHashMap article = new TIntIntHashMap();
        File file = new File(article_word_file, String.valueOf(article_id));

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

        synchronized (article_words) {
            article_words.put(article_id, article);
        }

        return article;

    }
}