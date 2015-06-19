package ir.pi0.irproject.utils;

import ir.pi0.irproject.structures.FastDict;

import java.util.HashMap;
import java.util.Map;

public class Normalizer {

    static Character[][] _tans_map = {
            {'ك', 'ک'},
            {'ي', 'ی'},
            {'ة', 'ه'},
            {'إ', 'ا'},
    };

    static Map<Character, Character> trans_map = new HashMap<>();

//            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    //    static String remove_chars_str = "0123456789۰۱۲۳۴۵۶۷۸۹®\r\n\t\"" +
//            "…”’؛ـ٪«÷»؟=`@#^&?•.+*$_[]{}()!%-/:<>',،;¯¨©|~–—\u007F\\¼½è" +
//            "abcdefghijklmnopqrstuvwxyz" +
//    static FastDict<Character> remove_chars;

    static String valid_chars_str =
            "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهیآاًهٔةيكإ";
    static FastDict<Character> valid_chars;

    static {

        for (Character[] map : _tans_map) trans_map.put(map[0], map[1]);

//        remove_chars = new FastDict<>(
//                Util.toCharacterArray(remove_chars_str), Character.class);

        valid_chars = new FastDict<>(
                Util.toCharacterArray(valid_chars_str), Character.class);

    }


    public static String normalize(String input) {
        StringBuilder output = new StringBuilder();

        boolean spaced = false;

        for (char c : input.toCharArray()) {
//            if (c == ' ' || c == ' '/*&nbsp(160)*/ || c == '\u200C'/*ZERO WIDTH NON-JOINER*/
//                    || remove_chars.contains(c)) {
            if (!valid_chars.contains(c)) {

                if (c == 'ـ')
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
        return output.toString().trim();

    }


}
