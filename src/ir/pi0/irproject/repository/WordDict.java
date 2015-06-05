package ir.pi0.irproject.repository;

import ir.pi0.irproject.utils.Util;

import java.io.*;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class WordDict implements IWordDict {

    //http://stackoverflow.com/questions/81346/most-efficient-way-to-increment-a-map-value-in-java
    //http://stackoverflow.com/questions/20356354/should-i-use-hashtable-or-hashmap-as-a-data-cache

    //Data : Word->Item
    protected final ConcurrentHashMap<String, WordDictItem> data;

    int last_word_id = 1;

    File file = null;

    public WordDict(WordDict parent) {
        this.data = parent.data;
        this.last_word_id = parent.last_word_id;
    }

    public WordDict(InputStream source) {

        data = new ConcurrentHashMap<String, WordDictItem>();
        last_word_id = 0;

        if (source == null)
            return;

        BufferedReader r =
                new BufferedReader(new InputStreamReader(source));
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
        this(file.exists() ? new FileInputStream(file) : null);
        this.file = file;
    }

    public WordDict() throws FileNotFoundException {
        this((File)null);
    }

    public void save() {
        save(this.file);
    }

    public void save(File file) {
        if (file == null)
            return;

        try {

            System.out.println("Saving dictionary");

            BufferedWriter w =
                    new BufferedWriter(new FileWriter(file));

            Enumeration<WordDictItem> elements = data.elements();
            while (elements.hasMoreElements()) {
                w.write(elements.nextElement().toString());
                w.write("\r\n");
            }

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

//        return data.computeIfAbsent(word, new Function<String, WordDictItem>() {
//            public WordDictItem apply(String s) {
//                return new WordDictItem(getNewWordID(), word);
//            }
//        });

        WordDictItem i = data.get(word);
        if (i == null)
            data.put(word, i = new WordDictItem(getNewWordID(), word));
        return i;

    }

    public WordDictItem findByWord(String word) {
        return data.get(word);
    }

    private int getNewWordID() {
        return ++last_word_id;
    }

    public void increment(final String word, int article_id, int by) {

        if(word.length()==0)
            return;

        WordDictItem item = findOrCreateByWord(word);

        item.increment(by, article_id);

        //TODO: count per article

    }


}
