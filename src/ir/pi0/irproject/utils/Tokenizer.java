package ir.pi0.irproject.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    private final Pattern pattern;

    public Tokenizer() {
//        this.pattern = Pattern.compile("[\\s'\",،;\\.\\*()!%-/:\r\n<>0-9]+");
        this.pattern = Pattern.compile("[\\s\n]+");//Faster
    }

    public List<String> tokenize(String text) {
//        return new ArrayList<String>(Arrays.asList(pattern.split(text)));
        return Arrays.asList(pattern.split(text));//Faster ? :D
    }

}
