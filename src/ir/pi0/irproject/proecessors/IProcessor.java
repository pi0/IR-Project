package ir.pi0.irproject.proecessors;

import java.util.List;

public interface IProcessor {
    void processArticle(List<String> words,int article_id);
}
