package ir.pi0.irproject.repository;

public interface IWordDict {

    WordDictItem findOrCreateByWord(String word);

    WordDictItem findByWord(String word);

    void increment(String word, int article_id, int by);

    void addItem(WordDictItem i);
}
