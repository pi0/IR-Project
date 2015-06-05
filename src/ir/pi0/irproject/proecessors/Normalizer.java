package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.structures.FastDict;
import ir.pi0.irproject.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Normalizer implements IProcessor {

    Character[][] _tans_map = {
            {'ك', 'ک'},
            {'ي', 'ی'},
    };

    String tans_to_space_map = "?\\\\\\\"\\.*()!%-/:<>',،;" +
            "؛٪«»؟";

    Map<Character, Character> trans_map = new HashMap<>();

    //    String remove_chars_str = "',،;؛٪.*()!%-/:<>«»0123456789۰۱۲۳۴۵۶۷۸۹؟?\\\"\\r" + ' '/*Half space*/;
    String remove_chars_str = "0123456789۰۱۲۳۴۵۶۷۸۹\r" + ' '/*Half space*/;

    FastDict<Character> remove_chars;

    public Normalizer() {

        for (Character[] map : _tans_map) trans_map.put(map[0], map[1]);

        for(char c:tans_to_space_map.toCharArray())
            trans_map.put(c,' ');

        remove_chars = new FastDict<>(
                Util.toCharacterArray(remove_chars_str), Character.class);

    }

    public String normalize(String input) {
        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray())
            if (!remove_chars.contains(c))
                output.append(trans_map.containsKey(c) ? trans_map.get(c) : c);
        return output.toString();
    }

    public void processArticle(List<String> words, int article_id) {
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);

            w = normalize(w);

            words.set(i, w);
        }
    }

}
