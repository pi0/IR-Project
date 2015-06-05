package ir.pi0.irproject.lemmatizer;

public class Stemmer {

    //Data was adapted from Hazm project

    private final String[] ends = new String[] {
            "ات", "ان", "ترین", "تر", "م", "ت", "ش", "یی", "ی", "ها", "ٔ", "‌ا", //
    };

    public String stem(String word) {
        for (String end : this.ends) {
            if (word.endsWith(end)) {
                word = word.substring(0, word.length() - end.length()).trim();
                //TODO: Strip whitespaces
            }
        }

        if (word.endsWith("ۀ"))
            word = word.substring(0, word.length() - 1) + "ه";

        return word;
    }

}
