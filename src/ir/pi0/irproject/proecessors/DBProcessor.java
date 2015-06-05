package ir.pi0.irproject.proecessors;

import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;
import ir.pi0.irproject.io.DBReader;
import ir.pi0.irproject.io.DBWriter;
import ir.pi0.irproject.structures.Queue;
import ir.pi0.irproject.utils.Tokenizer;

import java.util.List;

public class DBProcessor {

    DBReader reader;
    DBWriter writer;
    String tag;

    Thread[] background_workers;
    Thread background_writer;

    final Queue<DBProcessorJob> articles;

    boolean lock = false;

    int queue_max;

    Tokenizer tokenizer;

    private List<IProcessor> processors;

    public DBProcessor(List<IProcessor> processors, String path, String outPath, int in_buffer_size, int out_buffer_size, int queue_max) {
        this.processors = processors;

        reader = new DBReader(path);
        reader.open(in_buffer_size);

        if (outPath != null) {
            writer = new DBWriter(outPath);
            writer.open(out_buffer_size);
        } else {
            writer = null;
        }

        this.queue_max = queue_max;
        this.tag = Consts.DB_TAG;

        this.tokenizer = new Tokenizer();

        articles = new Queue<>();
    }

    public DBProcessor(List<IProcessor> processors, String path, String outPath) {
        this(processors, path, outPath, Consts.BEST_BUFFER_SIZE, Consts.BEST_BUFFER_SIZE, Consts.QUEUE_MAX);
    }

    public DBProcessor(List<IProcessor> processors, String path) {
        this(processors, path, null);
    }


    public boolean process() {
        if (lock)
            return false;
        lock = true;


        try {

            long startTime = System.currentTimeMillis();

            int tag_counter = 0;

            int read = 0, total = reader.available();
            double p, l_p = -1;


            background_workers = new Thread[4];
            for (int i = 0; i < background_workers.length; i++) {
                background_workers[i] = new Thread(new Runnable() {
                    public void run() {
                        while (lock || articles.size() > 0) {
                            synchronized (articles) {
                                try {
                                    articles.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            processArticle();
                        }
                    }
                });
                background_workers[i].start();
            }


            background_writer = new Thread(new Runnable() {
                public void run() {
                    while (lock || articles.size() > 0) {
                        writeArticle();
                    }
                }
            });
            background_writer.start();

            String article;
            int article_id = 0;

            while ((article = reader.readTag()) != null) {
                tag_counter++;
                read += article.getBytes().length;

                //Progress
                p = read * 1.0 / total;
                if (p - l_p > .002) {
                    long t = System.currentTimeMillis() - startTime;
                    Util.printProgress(p, t, false);
                    System.out.printf("{ %d Tags}", tag_counter);
                    l_p = p;
                }

                long heapFreeSize = Runtime.getRuntime().freeMemory();
                if (articles.size() > queue_max || heapFreeSize < Consts.MIN_SORT_MEM) {
                    while (articles.size() > 0) {
                        Util.printProgress(p, System.currentTimeMillis() - startTime, false);
                        System.out.printf(" #WAITING FOR QUEUE# [ Jobs : %d ]", articles.size());
                        Thread.sleep(50);
                        Util.clearLine();
                    }
//                    System.gc();
                }

                articles.enqueue(new DBProcessorJob(article, ++article_id));
                synchronized (articles) {
                    articles.notify();
                }

            }
            Util.clearLine();


            lock = false;
            System.out.println("Finishing all Queue jobs ...");
            background_workers[0].join();


            background_writer.join();
            if (writer != null)
                writer.close();


            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.printf("Took : %s\n", Util.getDurationBreakdown(elapsedTime, true));

            return true;
        } catch (Exception e) {
            System.err.println("Operation failed : " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            lock = false;
        }
    }


    private DBProcessorJob getJob() {
        try {
            for (DBProcessorJob j : articles)
                if (j.offer()) {
                    return j;
                }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void processArticle() {
        if (articles.isEmpty())
            return;

        DBProcessorJob j = getJob();
        if (j == null)
            return;

        List<String> words = tokenizer.tokenize(j.article);
        if (words == null)
            return;

        for (IProcessor p : processors)
            try {
                p.processArticle(words, j.article_id);
            } catch (Exception e) {
                e.printStackTrace();
            }

        j.done(words);
    }

    int l_w = 0;


    private synchronized void writeArticle() {
        if (articles.isEmpty())
            return;

        DBProcessorJob j = articles.top();
        if (j == null)
            return;

        //Do it until job is done
        while (true) {
            if (j.done) {

                if (writer != null)
                    writer.write(j.processed);

                articles.poll();
                synchronized (articles) {
                    articles.notifyAll();
                }

//                int d = j.article_id - l_w;
//                if (d != 1)
//                    System.err.println("Warning some article skipped: " + l_w + " => " + j.article_id);
//                l_w = j.article_id;

//                System.out.println("Job done and write: "+j.article_id);

                return;
            }
            try {
//                System.out.println("Waiting for "+j.article_id+" to done");
                synchronized (j.lock) {
                    j.lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
