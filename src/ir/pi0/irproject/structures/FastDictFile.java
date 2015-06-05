package ir.pi0.irproject.structures;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FastDictFile {

    private final String[] items;

    public FastDictFile(String path){
        this(path, new Middleware() {
            public String process(String in) {
                return in;
            }
        });
    }


    public FastDictFile(String path,Middleware middleware){
        List<String> words=new ArrayList<String>();

        try {
            BufferedReader r= new BufferedReader(new java.io.FileReader(path));
            String l;
            while((l=r.readLine())!=null) {
                l=middleware.process(l);
                if(l.length()>0)
                    words.add(l);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Read "+words.size()+" Items from "+path);

        Collections.sort(words);
        items=new String[words.size()];
        words.toArray(items);
    }

    public boolean contains(String word) {
        /*
        http://www.programcreek.com/2014/04/check-if-array-contains-a-value-java/
         */
        return java.util.Arrays.binarySearch(items, word)>0;
    }

    public interface Middleware{
        public String process(String in);
    }

}
