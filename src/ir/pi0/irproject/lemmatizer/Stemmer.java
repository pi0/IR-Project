package ir.pi0.irproject.lemmatizer;

public class Stemmer {

    //Data was adapted from Hazm project

    private final String[] ends = new String[] {
            "ات", "ان", "ترین", "تر", "م", "ت", "ش", "یی", "ی", "ها", "ٔ", "‌ا", //
    };

    private final String[] starts = new String[] {
            "از","در","با","بر","به","و","یا","برای"    };

    public String stem_1(String word) {
        for (String end : this.ends) {
            if (word.endsWith(end)) {
                word = word.substring(0, word.length() - end.length()).trim();
            }
        }
        return word;
    }

    public String stem_2(String word) {
        for (String start : this.starts) {
            if (word.startsWith(start)) {
                word = word.substring(start.length()).trim();
            }
        }
        return word;
    }

}
