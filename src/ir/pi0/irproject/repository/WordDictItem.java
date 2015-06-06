package ir.pi0.irproject.repository;

import gnu.trove.impl.sync.TSynchronizedIntSet;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WordDictItem {

    /*http://java.dzone.com/articles/hashset-vs-treeset-vs*/

    final int id;
    final String word;

    final TSynchronizedIntSet articles = new TSynchronizedIntSet(new TIntHashSet());


    public AtomicInteger repeats = new AtomicInteger(0);


    WordDictItem(int id, final String word) {
        this.id = id;
        this.word = word;
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
//        articles.add(article_id);
    }

    @Override
    public String toString() {

        //id,word,repeats,[a0,...,an,]

        final StringBuilder b = new StringBuilder();

        b.append(id).append(',')
                .append(word).append(',')
                .append(repeats).append(',');

        articles.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                b.append(i).append(',');
                return true;
            }
        });

        return b.toString();
    }

    public int articlesCount() {
        return articles.size();
    }
}
