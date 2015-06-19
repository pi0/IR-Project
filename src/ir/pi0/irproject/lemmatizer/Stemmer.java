package ir.pi0.irproject.lemmatizer;

public class Stemmer {

    //Data was adapted from Hazm project

    private final String[] starts = new String[]{
            "از", "در", "با", "بر", "به", "و", "یا", "برای", "را", "ها", "باید", "تنها", "هست", "هستند", "یها","نیز","یکدیگر"
    };

    private final String[] ends = new String[]{
            "ات", "ان", "ترین", "تر", "م", "ت", "ش", "یی", "ی", "ها", "ٔ", "‌ا", "در", "بر", "از", "و", "برای", "که"
    };

    public String stem(String word) {
        for (String start : this.starts) {
            if (word.startsWith(start)) {
                word = trim(word.substring(start.length()));
                return word;
            }
        }
        return null;
    }

    public String stemEx(String word) {
        for (String end : this.ends) {
            if (word.endsWith(end)) {
                word = trim(word.substring(0, word.length() - end.length()));
                return word;
            }
        }
        return null;
    }

    public static String trim(String s) {
        return s./*replace(' ',' ').*/trim();
    }


}
