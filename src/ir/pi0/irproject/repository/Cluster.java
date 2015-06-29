package ir.pi0.irproject.repository;

import gnu.trove.list.TIntList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;

import java.io.*;

public class Cluster {

    WordDict dict;
    int id = 0;
    final SubCluster[] sub_clusters;
    TIntDoubleHashMap doc;
    int size = 0;

    public Cluster(WordDict dict, int id) {
        this.dict = dict;
        this.id = id;

        this.sub_clusters = new SubCluster[Consts.SUB_CLUSTERS];
        for (int i = 0; i < Consts.SUB_CLUSTERS; i++) {
            this.sub_clusters[i] = new SubCluster();
        }

        doc = new TIntDoubleHashMap();
    }

    public void add(int article_id, TIntDoubleHashMap cached) {
        if (cached == null)
            cached = dict.load_article(article_id, false).getValue();


        SubCluster best = sub_clusters[0];
        double best_score = -1;
        for (SubCluster subCluster : sub_clusters) {
            if (subCluster.size() == 0) {
                best = subCluster;
                break;
            }
            double score = subCluster.compareToArticle(cached);
            if (score > best_score) {
                best = subCluster;
                best_score = score;
            }
        }
        best.add(article_id, cached);

        updateDoc(doc, cached);
        size++;
    }

    public double compareToArticle(TIntDoubleHashMap article) {
        return dict.articleCompare(doc, article);
    }

    public void saveToFile() {
        try {
            File dst = new File(dict.clusters_dir, String.valueOf(id));
            final BufferedWriter w = new BufferedWriter(new FileWriter(dst));

            w.write(Util.TIntDoubleHashMapToString(doc));
            w.write("\n");

            for (SubCluster sc : sub_clusters) {
                w.write(Util.TIntDoubleHashMapToString(sc.doc));
                w.write("\n");
                w.write(Util.TIntListToString(sc.articles));
                w.write("\n");
            }

            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFromFile() {
        File file = new File(dict.clusters_dir, String.valueOf(id));
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));

            this.doc=Util.StringToTIntDoubleHashMap(r.readLine());

            for (int i = 0; i < Consts.SUB_CLUSTERS; i++) {
                TIntList _a=Util.StringToTIntList(r.readLine());
                TIntDoubleHashMap _d=Util.StringToTIntDoubleHashMap(r.readLine());
                SubCluster sc=new SubCluster(_d,_a);
                sub_clusters[i]=sc;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public int getSize() {
        return size;
    }

    static void updateDoc(final TIntDoubleHashMap doc, TIntDoubleHashMap newData) {
        newData.forEachEntry(new TIntDoubleProcedure() {
            @Override
            public boolean execute(int i, double v) {
                doc.adjustOrPutValue(i, v, v);//Simply add
                return true;
            }
        });
    }

}
