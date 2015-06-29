package ir.pi0.irproject.repository;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class SubCluster {

    TIntDoubleHashMap doc;
    TIntList articles;

    SubCluster() {
        doc = new TIntDoubleHashMap();
        articles = new TIntArrayList();
    }

    public SubCluster(TIntDoubleHashMap doc, TIntList articles) {
        this.doc = doc;
        this.articles = articles;
    }

    public void add(int article_id, TIntDoubleHashMap cached) {
        articles.add(article_id);
        Cluster.updateDoc(doc, cached);
    }

    public int size() {
        return articles.size();
    }

    public double compareToArticle(TIntDoubleHashMap article) {
        return WordDict.articleCompare(doc, article);
    }

}
