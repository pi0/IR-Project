package ir.pi0.irproject.lemmatizer;

import ir.pi0.irproject.Consts;
import ir.pi0.irproject.proecessors.IProcessor;
import ir.pi0.irproject.structures.FastDictFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Lemmatizer implements IProcessor {

    FastDictFile persian_words = new FastDictFile(Consts.WORDS_FILE);

    HashMap<String, String> verbs = new HashMap<String, String>();

    Stemmer stemmer = new Stemmer();

    public Lemmatizer() {

        try {

            List<String> lines =
                    java.nio.file.Files.readAllLines(Paths.get(Consts.VERBS_FILE));
            for (String line : lines) {
                String[] s = line.split("#");
                if (s.length != 2)
                    continue;
                String[] parts = line.split("#");
                for (String c : Conjugations(parts[0], parts[1])) {
                    verbs.put(c, parts[0]);
                }

            }
            System.out.println("Loaded " + verbs.size() + " verbs");


        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    protected List<String> Conjugations(String past, String present) {
        /*Data from JHazm project*/

        HashSet<String> conjugates = new HashSet<String>();

        String[] endsList = new String[]{"م", "ی", "", "یم", "ید", "ند"};
        ArrayList<String> ends = new ArrayList<String>(Arrays.asList(endsList));

        for (String end : ends) {
            String conj = past + end;
            String nconj;

            // pastSimples
            conj = GetRefinement(conj);
            conjugates.add(conj);
            nconj = GetRefinement(GetNot(conj));
            conjugates.add(nconj);


            // pastImperfects
            conj = "می‌" + conj;
            conj = GetRefinement(conj);
            conjugates.add(conj);
            nconj = GetRefinement(GetNot(conj));
            conjugates.add(nconj);
        }

        endsList = new String[]{"ه‌ام", "ه‌ای", "ه", "ه‌ایم", "ه‌اید", "ه‌اند"};
        ends = new ArrayList<String>(Arrays.asList(endsList));

        // pastNarratives
        for (String end : ends) {
            String conj = past + end;
            conjugates.add(GetRefinement(conj));
            conjugates.add(GetRefinement(GetNot(conj)));
        }

        conjugates.add(GetRefinement("ب" + present));
        conjugates.add(GetRefinement("ن" + present));

        if (present.endsWith("ا") || Arrays.asList(new String[]{"آ", "گو"}).contains(present))
            present = present + "ی";

        endsList = new String[]{"م", "ی", "د", "یم", "ید", "ند"};
        ends = new ArrayList<String>(Arrays.asList(endsList));

        List<String> presentSimples = new ArrayList<String>();
        for (String end : ends) {
            String conj = present + end;
            presentSimples.add(conj);

            conjugates.add(GetRefinement(conj));
            conjugates.add(GetRefinement(GetNot(conj)));
        }

        for (String item : presentSimples) {
            String conj;

            // presentImperfects
            conj = "می‌" + item;
            conjugates.add(GetRefinement(conj));
            conjugates.add(GetRefinement(GetNot(conj)));

            // presentSubjunctives
            conj = "ب" + item;
            conjugates.add(GetRefinement(conj));

            // presentNotSubjunctives
            conj = "ن" + item;
            conjugates.add(GetRefinement(conj));
        }

        return new ArrayList<String>(conjugates);
    }

    private String GetRefinement(String text) {
        return text.replace("بآ", "بیا").replace("نآ", "نیا");
    }

    private String GetNot(String text) {
        return "ن" + text;
    }

    public String lemmetize(String w) {

        if (persian_words.contains(w))
            return null;

        if (verbs.containsKey(w)) return verbs.get(w);

        String stem = stemmer.stem(w);
        if (persian_words.contains(stem)) return stem;

        return null;
    }

    public void processArticle(List<String> words, int article_id) {

        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);

            if (w.length() == 0)
                continue;

            String l = lemmetize(w);
            if (l != null) {
                words.set(i, l);
            }

        }
    }
}
