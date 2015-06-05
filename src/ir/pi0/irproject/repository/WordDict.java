package ir.pi0.irproject.repository;

import gnu.trove.impl.sync.TSynchronizedIntSet;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import ir.pi0.irproject.structures.LRUCache;

import java.io.*;


public class WordDict implements IWordDict {

    //http://stackoverflow.com/questions/81346/most-efficient-way-to-increment-a-map-value-in-java
    //http://stackoverflow.com/questions/20356354/should-i-use-hashtable-or-hashmap-as-a-data-cache

    //Word->Item
    protected final THashMap<String, WordDictItem> data;

    protected final LRUCache<String, WordDictItem> data_cache
            = new LRUCache<>(32);

    //Article_ID -> [article words with repeat]
    protected final TIntObjectHashMap<TSynchronizedIntSet>
            article_data
            = new TIntObjectHashMap<>();

    int last_word_id = 1;

    File file = null;

    public WordDict(WordDict parent) {
        this.data = parent.data;
        this.last_word_id = parent.last_word_id;
    }

    public WordDict(InputStream source) {

        data = new THashMap<>();

        last_word_id = 0;

        if (source == null)
            return;

        System.out.println("Reading dictionary database ...");


        BufferedReader r =
                new BufferedReader(new InputStreamReader(source), 1024 * 1024 * 16);
        String l;

        try {
            while ((l = r.readLine()) != null) {
                WordDictItem i = WordDictItem.fromData(l);
                addItem(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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

//    public THashMap<Integer, ArticleDictItem> findOrCreateArticle(int article_id) {
//        THashMap<Integer, ArticleDictItem> article_words = article_data.get(article_id);
//        if (article_words == null)
//            article_data.put(article_id, article_words = new THashMap<>());
//        return article_words;
//    }
//
//    public ArticleDictItem findOrCreateWordInArticle(int article_id, final int word_id) {
//
//        THashMap<Integer, ArticleDictItem> article_words = findOrCreateArticle(article_id);
//
//        ArticleDictItem i = article_words.get(word_id);
//        if (i == null)
//            article_words.put(word_id, i = new ArticleDictItem(word_id));
//
//        return i;
//    }


    public WordDictItem findByWord(String word) {
        return data.get(word);//TODO: LRUCache
    }

    private int getNewWordID() {
        return ++last_word_id;
    }

    public void increment(final String word, int article_id, int by) {

        if (word.length() == 0)
            return;

        WordDictItem word_item = findOrCreateByWord(word);

        word_item.increment(by, article_id);

//        ArticleDictItem article =
//                findOrCreateWordInArticle(article_id,word_item.id);
//        article.increment(by);

    }


}
