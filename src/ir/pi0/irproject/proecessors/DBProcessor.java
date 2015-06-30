package ir.pi0.irproject.proecessors;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.io.DBReader;
import ir.pi0.irproject.io.DBWriter;
import ir.pi0.irproject.structures.Queue;
import ir.pi0.irproject.utils.Tokenizer;
import ir.pi0.irproject.utils.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

            int article_counter = 0;

            int total = reader.available();
            double p, l_p = -1;

            TIntIntHashMap starts = new TIntIntHashMap();

            System.out.println("Starting " + Consts.PROCESSOR_WORKERS + " Parallel workers");

            background_workers = new Thread[Consts.PROCESSOR_WORKERS];
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
            int last_article_id = 0;

            long heapTotal = Runtime.getRuntime().totalMemory();
            long heapFreeSize_min = heapTotal;

            while ((article = reader.readTag()) != null) {
                article_counter++;

                //(Debug)
                long heapFreeSize = Runtime.getRuntime().freeMemory();
                if (heapFreeSize < heapFreeSize_min)
                    heapFreeSize_min = heapFreeSize;

                //Progress
                p = 1 - (reader.available() * 1.0 / total);
                if (p - l_p > .001) {
                    print_progress(startTime, p, null, article_counter, heapFreeSize * 1.0 / heapTotal);
                    l_p = p;
                }

                if (articles.size() > queue_max || heapFreeSize < Consts.MIN_SORT_MEM) {
                    while (articles.size() > 0) {
                        print_progress(startTime, p, /*Consts.waiting_tag*/ null,
                                article_counter, heapFreeSize * 1.0 / heapTotal);
                        Thread.sleep(100);
                    }
                    System.gc();
                }

                articles.enqueue(new DBProcessorJob(article, ++last_article_id));


                synchronized (articles) {
                    articles.notify();
                }

            }
            Util.clearLine();

            starts.put(last_article_id, reader.pos());


            lock = false;
            System.out.println("Finishing all Queue jobs ...");
            background_workers[0].join();
            background_writer.join();
            if (writer != null)
                writer.close();


            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.printf("Max heap usage during process : %s\n",
                    Util.humanReadableByteCount(Runtime.getRuntime().totalMemory() - heapFreeSize_min));
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

    private void print_progress(long startTime, double p, String tag,
                                int tag_counter, double heap_free) {
        long t = System.currentTimeMillis() - startTime;

        Util.clearLine();

        Util.printProgress(p, t, false, true, "Progress");

        System.out.printf(" [ %d Articles ] ", tag_counter);

        Util.printProgress(1 - heap_free, 0, false, false, "Heap usage");

        if (tag != null)
            System.out.print(tag);
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
        j.article = "";//Free data ASAP

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

                return;
            }
            try {
                synchronized (j.lock) {
                    j.lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


}
