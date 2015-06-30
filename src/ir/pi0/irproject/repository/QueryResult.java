package ir.pi0.irproject.repository;

public class QueryResult implements Comparable<QueryResult> {


    public Double score;

    //L1
    public Cluster cluster;

    //L2
    public SubCluster subCluster;

    //L3
    public int article_id;

    public QueryResult(double score, Cluster cluster) {
        this.score = score;
        this.cluster = cluster;
    }

    public QueryResult(double score, SubCluster subCluster) {
        this.score = score;
        this.subCluster = subCluster;
    }

    public QueryResult(double score, int article_id) {
        this.score = score;
        this.article_id=article_id;
    }


    @Override
    public int compareTo(QueryResult o) {
        return score.compareTo(o.score);
    }
}
