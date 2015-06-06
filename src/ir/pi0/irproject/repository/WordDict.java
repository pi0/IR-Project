package ir.pi0.irproject.repository;

import gnu.trove.TCollections;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import ir.pi0.irproject.structures.LRUCache;
import ir.pi0.irproject.utils.Util;

import java.io.*;


public class WordDict {

    //Word->Item
    protected final THashMap<String, WordDictItem> data;

    protected final LRUCache<String, WordDictItem> data_cache = new LRUCache<>(64);

    //Article_ID -> [article words with repeat]
    protected final TIntObjectHashMap<TIntIntMap> article_data;

    int last_word_id = 1;

    File file = null;

    public WordDict(InputStream source) {

        data = new THashMap<>();

        article_data = new TIntObjectHashMap<>();

        last_word_id = 0;

        if (source == null)
            return;

        System.out.println("Reading dictionary database ...");


        BufferedReader r =
                new BufferedReader(new InputStreamReader(source), 1024 * 1024 * 16);
        String l = null;

        long heapFreeSize = Runtime.getRuntime().freeMemory();

        try {
            l = r.readLine();

            if (!WordDictItem.csvHeader.equals(l))
                System.err.println("Warning: Invalid index csv header");

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while ((l = r.readLine()) != null) {
                WordDictItem i = WordDictItem.fromData(l);
                addItem(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long heapFreeSize2 = Runtime.getRuntime().freeMemory();
        System.out.format("Heap usage: %s \n",
                Util.humanReadableByteCount(
                        (int) (heapFreeSize2 - heapFreeSize), false));
    }

    public WordDict(File file) throws FileNotFoundException {
        this(file != null && file.exists() ? new FileInputStream(file) : null);
        this.file = file;
    }

    public WordDict() throws FileNotFoundException {
        this((File) null);
    }

    public void save() {
        save(this.file);
    }

    public void save(File file) {
        if (file == null)
            return;

        try {

            System.out.println("Saving dictionary");

            final BufferedWriter w =
                    new BufferedWriter(new FileWriter(file));

            w.write(WordDictItem.csvHeader);
            w.write("\r\n");

            data.forEachValue(new TObjectProcedure<WordDictItem>() {
                @Override
                public boolean execute(WordDictItem wordDictItem) {
                    try {
                        w.write(wordDictItem.toString());
                        w.write("\r\n");
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            });

            System.out.println("Save done");

            w.flush();
            w.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------

    public void addItem(WordDictItem i) {
        if (i.id > last_word_id)
            last_word_id = i.id + 1;
        data.put(i.word, i);
    }


    public WordDictItem findByWord(String word) {
        return data.get(word);//TODO: LRUCache
    }

    public WordDictItem findOrCreateByWord(final String word) {

        WordDictItem i;

        i = data_cache.get(word);
        if (i != null)
            return i;

        i = data.get(word);
        if (i == null)
            synchronized (data) {
                data.put(word, i = new WordDictItem(getNewWordID(), word));
            }

        data_cache.put(word, i);

        return i;

    }


    private int getNewWordID() {
        return ++last_word_id;
    }

    public void increment(final String word, int article_id, int by) {

        if (word.length() == 0)
            return;

        WordDictItem word_item = findOrCreateByWord(word);

        word_item.increment(by, article_id);


        TIntIntMap article;

        synchronized (article_data) {
            article = article_data.get(article_id);
            if (article == null)
                article_data.put(article_id, article = TCollections.synchronizedMap(new TIntIntHashMap()));
        }

        article.adjustOrPutValue(word_item.id, by, by);

    }


}