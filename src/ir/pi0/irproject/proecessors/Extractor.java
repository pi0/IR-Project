package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.utils.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Extractor implements IProcessor {

    File outputDir;

    public Extractor(File outputDir){
        this.outputDir=outputDir;
        if(outputDir.exists())
            Util.deleteRecursive(outputDir);
        outputDir.mkdirs();
    }

    public void processArticle(List<String> words,int article_id) {
        File out=new File(outputDir,String.valueOf(article_id));
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(out));
            for(String w:words){
                if(w.length()==0)
                    continue;
                bw.write(w);
                bw.write(" ");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
