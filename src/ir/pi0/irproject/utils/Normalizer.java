package ir.pi0.irproject.utils;

import ir.pi0.irproject.structures.FastDict;

import java.util.HashMap;
import java.util.Map;

public class Normalizer {

    Character[][] _tans_map = {
            {'ك', 'ک'},
            {'ي', 'ی'},
            {'ة', 'ه'},
            {'إ', 'ا'},

    };

    Map<Character, Character> trans_map = new HashMap<>();

    String remove_chars_str = "0123456789۰۱۲۳۴۵۶۷۸۹®\r" +
            "ء" +
            "ً" +
            "=`@#^&?•.+*$_[]{}()!%-/:<>',،;\\\"\n\t" +
            "؛ـ٪«÷»؟" +
            " "/*Half space*/ +
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    FastDict<Character> remove_chars;

    public Normalizer() {

        for (Character[] map : _tans_map) trans_map.put(map[0], map[1]);

        remove_chars = new FastDict<>(
                Util.toCharacterArray(remove_chars_str), Character.class);
    }

    public String normalize(String input) {
        StringBuilder output = new StringBuilder();

        boolean spaced = false;

        for (char c : input.toCharArray()) {

            if (c == ' ' || remove_chars.contains(c)) {

                if(c=='ـ')
                    continue;

                if (spaced)
                    continue;
                else {
                    spaced = true;
                    output.append(' ');
                    continue;
                }
            }

            spaced = false;
            output.append(trans_map.containsKey(c) ? trans_map.get(c) : c);
        }
        return output.toString();
    }


}
