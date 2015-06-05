package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.structures.FastDictFile;

import java.util.List;

public class StopWordRemover implements IProcessor {

    private FastDictFile stopWords;

    Normalizer normalizer=new Normalizer();

    public StopWordRemover(String path){
        stopWords=new FastDictFile(path, new FastDictFile.Middleware() {
            public String process(String in) {
                return normalizer.normalize(in);
            }
        });
    }

    public void processArticle(List<String> words,int article_id) {
        for(int i=0;i<words.size();i++){
            String w=words.get(i);
            if(w.length()==0) {
                continue;
            }

            if(stopWords.contains(w))
                words.set(i,"");
//                words.remove(i); BUG BUG BUG :(

        }
    }

}
