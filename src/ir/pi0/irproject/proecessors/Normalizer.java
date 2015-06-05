package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.structures.FastDict;
import ir.pi0.irproject.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Normalizer implements IProcessor {

    String[] tans_str = {" كي", " کی"};
    Map<Character, Character> trans_map = new HashMap<Character, Character>();


    String remove_chars_str = "',،;؛٪.*()!%-/:<>0123456789۰۱۲۳۴۵۶۷۸۹\"\r";
    FastDict<Character> remove_chars;

    public Normalizer() {

        for (int i = 0; i < tans_str[0].length(); i++)
            trans_map.put(tans_str[0].charAt(i), tans_str[1].charAt(i));

        remove_chars = new FastDict<Character>(
                Util.toCharacterArray(remove_chars_str), Character.class);

    }

    public String normalize(String input) {
        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray())
            if (!remove_chars.contains(c))
                output.append(trans_map.containsKey(c) ? trans_map.get(c) : c);
        return output.toString();
    }

    public void processArticle(List<String> words,int article_id) {
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);

            w = normalize(w);

            words.set(i, w);
        }
    }

}
