package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.Consts;
import ir.pi0.irproject.structures.FastDictFile;
import ir.pi0.irproject.utils.Normalizer;

import java.util.List;

public class StopWordRemover implements IProcessor {

    private FastDictFile stopWords;

    private static StopWordRemover instance=null;

    public static StopWordRemover getInstance() {
        if(instance==null)
            instance=new StopWordRemover(Consts.STOPWORDS_FILE);
        return instance;
    }

    private StopWordRemover(String path){
        stopWords=new FastDictFile(path, new FastDictFile.Middleware() {
            public String process(String in) {
                return Normalizer.normalize(in);
            }
        });
    }

    public void processArticle(List<String> words,int article_id) {
        for(int i=0;i<words.size();i++){
            String w=words.get(i);

            if(w.length()==0)
                continue;

            if(stopWords.contains(w))
                words.set(i,"");
        }
    }

}
