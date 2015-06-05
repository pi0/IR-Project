package ir.pi0.irproject.proecessors;

import java.util.List;

public class DBProcessorJob {

    int article_id;
    String article;
    List<String> processed = null;
    boolean offered=false;
    boolean done=false;

    final Character lock=new Character('A');

    DBProcessorJob(String article,int article_id) {
        this.article = article;
        this.article_id=article_id;
    }

    public synchronized boolean offer() {
        boolean o = !(done || offered);
        if(o)
            offered=true;
        return o;
    }

    public synchronized void done(List<String> res){
        this.processed=res;
//        this.article="";
        done=true;
        synchronized (lock){
            lock.notify();
        }
    }


}
