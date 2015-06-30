package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.repository.WordDict;

import java.util.List;

public class Indexer implements IProcessor {

    WordDict d;
    boolean sync_postings;

    public Indexer(WordDict parent) {
        this(parent, false);
    }

    public Indexer(WordDict parent, boolean sync_postings) {
        d = parent;
        this.sync_postings = sync_postings;
    }

    public void processArticle(List<String> words, int article_id) {

        String last_word = "";
        int last_repeat = 0;

        for (String word : words) {
            if (word.length() == 0)
                continue;

            if (last_word.equals(word)) {
                last_repeat++;
            } else {
                d.increment(last_word, article_id, last_repeat);
                last_word = word;
                last_repeat = 1;
            }
        }

        d.increment(last_word, article_id, last_repeat);

        d.flush_article(article_id, sync_postings, true);

    }
}
