package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.repository.WordDict;

import java.util.List;

public class Indexer extends WordDict implements IProcessor {

    public Indexer(WordDict parent) {
        super(parent);
    }

    public void processArticle(List<String> words, int article_id) {


        String lastword="";
        int last_repeat= 0;

        for (String word : words) {
            if(word.length()==0)
                continue;

            if (lastword.equals(word)) {
                last_repeat++;
            } else {
                increment(lastword, article_id, last_repeat);
                lastword = word;
                last_repeat=1;
            }
        }

        increment(lastword, article_id, last_repeat);

    }
}
