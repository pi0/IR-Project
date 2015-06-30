package ir.pi0.irproject.repository;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;

import java.io.*;

public class Cluster {

    boolean active=true;
    WordDict dict;
    int id = 0;
    private SubCluster[] sub_clusters;
    TIntDoubleHashMap doc;
    int size = 0;
    boolean subclustersLoaded = false;

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
        return WordDict.articleCompare(article, doc);
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

    public void loadFromFile(boolean loadSubClusters) {
        File file = new File(dict.clusters_dir, String.valueOf(id));
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));

            this.doc = Util.StringToTIntDoubleHashMap(r.readLine());
            if (loadSubClusters) {
                size=0;
                for (int i = 0; i < Consts.SUB_CLUSTERS; i++) {
                    TIntDoubleHashMap _d = Util.StringToTIntDoubleHashMap(r.readLine());
                    TIntList _a = Util.StringToTIntList(r.readLine());
                    SubCluster sc = new SubCluster(_d, _a);
                    sub_clusters[i] = sc;
                    size+=_a.size();
                }
                subclustersLoaded = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SubCluster[] getSubClusters() {
        if(!subclustersLoaded)
            loadFromFile(true);
        return sub_clusters;
    }

    public int getSize() {
        return size;
    }

    static void updateDoc(final TIntDoubleHashMap doc, TIntDoubleHashMap newData) {

        TIntDoubleIterator i = newData.iterator();
        for (int j=newData.size(); j-->0; ) {
            i.advance();
            double v= i.value();
            doc.adjustOrPutValue(i.key(), v, v);//Simply add
        }

    }

    public void discardSubClusters() {
        subclustersLoaded=false;
        this.sub_clusters=new SubCluster[sub_clusters.length];
    }
}
