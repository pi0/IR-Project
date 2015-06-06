package ir.pi0.irproject.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    private final Pattern pattern = Pattern.compile(" +");

    private final Normalizer normalizer = new Normalizer();


    public List<String> tokenize(String text) {

        text=normalizer.normalize(text);

        return Arrays.asList(pattern.split(
                text
        ));
    }

}
