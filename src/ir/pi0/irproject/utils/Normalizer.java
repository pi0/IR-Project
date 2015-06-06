package ir.pi0.irproject.utils;

import ir.pi0.irproject.structures.FastDict;

import java.util.HashMap;
import java.util.Map;

public class Normalizer {

    Character[][] _tans_map = {
            {'ك', 'ک'},
            {'ي', 'ی'},
            {'ة', 'ه'},
            {'إ','ا'},
            
    };

    String tans_to_space_map = "?•.*$_[]{}()!%-/:<>',،;\\\"\n\t" +
            "؛٪«»؟" +
            " "/*Half space*/;


    Map<Character, Character> trans_map = new HashMap<>();

    String remove_chars_str = "0123456789۰۱۲۳۴۵۶۷۸۹®\r" +
            "ء" +
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    FastDict<Character> remove_chars;

    public Normalizer() {

        for (Character[] map : _tans_map) trans_map.put(map[0], map[1]);

        for (char c : tans_to_space_map.toCharArray())
            trans_map.put(c, ' ');

        remove_chars = new FastDict<>(
                Util.toCharacterArray(remove_chars_str), Character.class);

    }

    public String normalize(String input) {
        StringBuilder output = new StringBuilder();

        boolean spaced = false;

        for (char c : input.toCharArray()) {

            if (c == ' ') {
                if (spaced)
                    continue;
                else {
                    spaced = true;
                    output.append(' ');
                    continue;
                }
            }

            spaced = false;

            if (!remove_chars.contains(c))
                output.append(trans_map.containsKey(c) ? trans_map.get(c) : c);
        }
        return output.toString();
    }


}
