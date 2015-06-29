package ir.pi0.irproject;

import ir.pi0.irproject.io.DBReader;
import ir.pi0.irproject.lemmatizer.Lemmatizer;
import ir.pi0.irproject.proecessors.*;
import ir.pi0.irproject.repository.WordDict;
import ir.pi0.irproject.utils.Util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    void benchmark(String path) throws Exception {

        DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(new File(Consts.BENCHMARK_BASE + "benchmark.txt")));

        int test[] = {
                1024 * 64,/* 64kb */
                1024 * 128,/* 128kb */
                1024 * 512,/* 512kb */
                1024 * 1024 * 4,/* 4MiB */
                1024 * 1024 * 16,/* 16MiB */
                1024 * 1024,/* 1MiB */
                1024,/* 1kb */
                1,/* 1b  SO SLOW! */
        };


        for (int size_byte : test) {
            System.out.println("Testing with Buffer size of " +
                    Util.humanReadableByteCount(size_byte, false));
            DBReader d = new DBReader(path);
            d.open(size_byte);
            long b = d.benchmark();
            dos.writeBytes(String.format("%d %d\r\n", size_byte, b));
            dos.flush();
        }
        dos.close();

    }

    void normalize(String path, String out) {

        System.out.println("Normalizing database");

        IProcessor[] p = {
                StopWordRemover.getInstance(),
                new Lemmatizer(),
                StopWordRemover.getInstance(),
                new Sorter(),
        };

        DBProcessor processor =
                new DBProcessor(Arrays.asList(p), path, out);
        processor.process();

        System.out.println("Saved to: " + out);
    }


    void index(String path, String out) throws Exception {

        System.out.println("Indexing database");

        WordDict wordDict = new WordDict(new File(out), true, false);

        IProcessor[] p = {
                new Indexer(wordDict)
        };

        DBProcessor processor =
                new DBProcessor(Arrays.asList(p), path);
        processor.process();

        wordDict.save();

        System.out.println("Saved to: " + out);

    }


    void generate(String db) throws Exception {
        System.out.println("Generate weights");
        WordDict wordDict = new WordDict(new File(db), false, false);
        wordDict.calculate_weights();
    }

    void cluster(String db) throws Exception {
        System.out.println("Clustering");
        WordDict wordDict = new WordDict(new File(db), false, false);
        wordDict.cluster_articles();
    }


    void cli(String path) throws Exception {

        WordDict wordDict = new WordDict(new File(path), false, false);

        Scanner s = new Scanner(System.in);

        while (true) {
            System.out.print("Word to lookup ~> ");
            String l = s.nextLine();

            wordDict.query(l,100);

//            Integer i = wordDict.getWordRepeats(l);
//            if (i != null)
//                System.out.println("Repeats: " + i);
//            else
//                System.out.println("Not found !");
        }


    }

    void demo(String path, String out) {

        IProcessor stopWordRemover = StopWordRemover.getInstance();

        WordDict wordDict = new WordDict(new File(out), true, false);

        IProcessor[] p = {
                stopWordRemover,
                new Lemmatizer(),
                stopWordRemover,
                new Sorter(),
                new Indexer(wordDict),
        };

        System.out.println("StopWord -> Lemmatize -> StopWord -> Sort -> Index");

        DBProcessor processor = new DBProcessor(Arrays.asList(p), path, out);

        processor.process();

        wordDict.save();

        wordDict.calculate_weights();

    }


    public static void main(String[] args) throws Exception {
        Main main = new Main();

        if (args.length < 1) {
            printUsage();
            return;
        }

        System.out.println("IR-Project");
        System.out.format("Total heap: %s\r\n",
                Util.humanReadableByteCount(Runtime.getRuntime().maxMemory()));

        long startTime = System.currentTimeMillis();

        System.out.println("-----------------------------");

        switch (args[0].charAt(0)) {
            case 'b':
                main.benchmark(args[1]);
                break;
            case 'i':
                main.index(args[1], args[2]);
                break;
            case 'g':
                main.generate(args[1]);
                break;
            case 'n':
                main.normalize(args[1], args[2]);
                break;
            case 'c':
                main.cli(args[1]);
                break;
            case 'l':
                main.cluster(args[1]);
                break;

            case 'd':
                main.demo(args[1], args.length > 0 ? args[2] : null);
                break;
            default:
                printUsage();
                break;
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("-----------------------------");
        System.out.printf("Total runtime: %s\n", Util.getDurationBreakdown(elapsedTime, true));


    }

    public static void printUsage() {
        System.out.println("Usage : ./run.sh d|b|i|g|n|c|l [input file] [output file]");
    }
}
