package ir.pi0.irproject.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    private final Pattern pattern = Pattern.compile(" +");

    public List<String> tokenize(String text) {

        text=Normalizer.normalize(text);

        String[] s= pattern.split(text);
        return Arrays.asList(s);
    }

}
