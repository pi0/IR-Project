package ir.pi0.irproject.utils;

import ir.pi0.irproject.lemmatizer.Lemmatizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HighLighter {

    String start;
    String end;

    Lemmatizer lemmatizer = Lemmatizer.getInstance();
    Tokenizer t=new Tokenizer();

    public HighLighter(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String highlight(String article, String query) {
        StringBuilder sb = new StringBuilder();

        List<String> q_split = t.tokenize(query);
        Set<String> q = new HashSet<>(q_split.size());
        for (String s : q_split)
            q.add(lemmatizer.lemmetize(s));

        List<String> a_split = t.tokenize(article);

        for (String s : a_split) {

            boolean m = q.contains(lemmatizer.lemmetize(s));
            if (m)
                sb.append(start);
            sb.append(s);
            if (m)
                sb.append(end);
            sb.append(" ");
        }

        return sb.toString();
    }


}
