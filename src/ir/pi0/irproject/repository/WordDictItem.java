package ir.pi0.irproject.repository;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class WordDictItem {

    /*http://java.dzone.com/articles/hashset-vs-treeset-vs*/

    final int id;
    final String word;

    final HashSet<Integer> articles = new HashSet<>();

    public AtomicInteger repeats = new AtomicInteger(0);


    WordDictItem(int id, String word) {
        this.id = id;
        this.word = word;
        this.articles.add(id);//TODO: concurrency ?
    }

    public static WordDictItem fromData(String data) {

        String[] split = data.split(",");

        int id = Integer.parseInt(split[0]);
        int repeats = Integer.parseInt(split[2]);

        WordDictItem instance = new WordDictItem(id, split[1]);

        instance.repeats = new AtomicInteger(repeats);

        if (split.length > 3)
            for (int i = 3; i < split.length; i++)
                instance.articles.add(Integer.parseInt(split[i]));

        return instance;
    }

    public void increment(int by, int article_id) {
        repeats.addAndGet(by);
    }

    @Override
    public String toString() {

        //id,word,repeats,[a0,...,an,]

        StringBuilder b = new StringBuilder();

        b.append(id).append(',')
                .append(word).append(',')
                .append(repeats).append(',');

        for (int a_id : articles)
            b.append(a_id).append(',');

        return b.toString();
    }

    public int articlesCount() {
        return articles.size();
    }
}
