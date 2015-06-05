package ir.pi0.irproject.io;

import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;

import java.io.IOException;
import java.util.regex.Pattern;

public class DBReader extends FileReader {

    String tag;

    public DBReader(String path) {
        super(path);
        this.tag = Consts.DB_TAG;
    }

    @Override
    public boolean open(int buffer_size) {
        boolean r = super.open(buffer_size);
        bReader.setNl('>');
        readTag();//Skip first empty tag
        return r;
    }

    public long benchmark() {

        try {

            long startTime = System.currentTimeMillis();

            double total = inputStream.available();
            double l_p = -1, p, read = 0, c_read;
//            char buff[] = new char[buffer_size];

            String article;

            while ((article = readTag()) != null) {

//                read += c_read * 2/*UTF-8*/;
//                http://stackoverflow.com/questions/5078314/isnt-the-size-of-character-in-java-2-bytes

                read += article.getBytes().length;

                p = read * 1.0 / total;
                if (p - l_p > .009) {
                    Util.printProgress(p, System.currentTimeMillis() - startTime, false);
                    l_p = p;
                }
            }
            Util.clearLine();

            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.printf("IO BUFF: %s  Took : %s\n",
                    Util.humanReadableByteCount(buffer_size, false),
                    Util.getDurationBreakdown(elapsedTime, true));

            return elapsedTime;

        } catch (Exception e) {
            System.err.println("Benchmark failed : " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public WordIndex[] index(String[] words) {

        int tag_counter = 0;

        WordIndex[] index = new WordIndex[words.length];

        for (int i = 0; i < words.length; i++)
            index[i] = new WordIndex(words[i]);


        try {
            long startTime = System.currentTimeMillis();

            String article;
            int read = 0, total = inputStream.available();
            double p, l_p = -1;


            while ((article = readTag()) != null) {

                read += article.getBytes().length;

                tag_counter++;

                //Progress
                p = read * 1.0 / total;
                if (p - l_p > .002) {
                    Util.printProgress(p, System.currentTimeMillis() - startTime, false);
                    System.out.printf(" { %d Tags }", tag_counter);
                    l_p = p;
                }

                //Index words
                for (WordIndex i : index) {

                    int r = Util.countMatches(article, i.pattern);
                    if (r == 0)
                        continue;
                    //Else if index word found ...
                    i.count_pertags++;
                    if (r > i.max_intag)
                        i.max_intag = r;

                    i.count += r;
                }

            }
            Util.clearLine();


            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;


            System.out.printf("Read Tags : %d\n", tag_counter);
            System.out.printf("Took : %s\n", Util.getDurationBreakdown(elapsedTime, true));

            return index;

        } catch (Exception e) {
            System.err.println("Indexing failed : " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    public String readTag() {
        StringBuilder t = new StringBuilder();
        String r = null;
        String tag_head = tag.substring(0, tag.length() - 1);
        try {
            while ((r = bReader.readToTag()) != null) {
                if (r.endsWith(tag_head))
                    return t.append(r.substring(0, r.length() - tag_head.length()))
                            .toString();
                else
                    t.append(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public class WordIndex {
        String word;
        int count = 0;
        int count_pertags = 0;
        int max_intag = 0;

        Pattern pattern;

        WordIndex(String word) {
            this.word = word;
            pattern = Pattern.compile(word);
        }


        @Override
        public String toString() {
            return String.format("%s\tCount:[%d]\tObserved in tags:[%d]\tMax count in tags:[%d]",
                    word, count, count_pertags, max_intag);
        }


    }


}
