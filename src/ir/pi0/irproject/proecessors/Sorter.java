package ir.pi0.irproject.proecessors;

import java.util.Collections;
import java.util.List;

public class Sorter implements IProcessor {

    public void processArticle(List<String> words,int article_id) {

        Collections.sort(words);

    }
}
