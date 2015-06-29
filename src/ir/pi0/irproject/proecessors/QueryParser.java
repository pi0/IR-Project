package ir.pi0.irproject.proecessors;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ir.pi0.irproject.lemmatizer.Lemmatizer;
import ir.pi0.irproject.repository.WordDict;
import ir.pi0.irproject.utils.Tokenizer;

import java.util.List;

public class QueryParser {

    WordDict dict;
    IProcessor[] processors;
    Tokenizer tokenizer = new Tokenizer();

    public QueryParser(WordDict dict, IProcessor[] processors) {
        this.dict = dict;
        this.processors = processors;
    }

    public QueryParser(WordDict dict) {
        this(dict, new IProcessor[]{
                StopWordRemover.getInstance(),
                new Lemmatizer(),
                StopWordRemover.getInstance(),
                new Sorter(),
                new Indexer(dict),
        });
    }

    private List<String> tokenizeAndProcess(String query) {
        List<String> t = tokenizer.tokenize(query);
        for (IProcessor processor : processors)
            processor.processArticle(t, -1);
        return t;
    }

    private TIntIntHashMap get_word_repeats(List<String> tokens) {
        TIntIntHashMap m = new TIntIntHashMap();
        for (String word : tokens) {
            if (word.length() == 0)
                continue;
            Integer word_id = dict.getWordID(word);
            if (word_id == null)
                continue;
            int word_repeats = dict.getWordRepeats(word_id);
            m.put(word_id, word_repeats);
        }
        return m;
    }

    public TIntDoubleHashMap get_weights(String token) {
        List<String> tokens = tokenizeAndProcess(token);
        TIntIntHashMap repeats = get_word_repeats(tokens);
        TIntDoubleHashMap weights = dict.calculate_weight(repeats);
        return weights;
    }

}
