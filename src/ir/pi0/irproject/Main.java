package ir.pi0.irproject;

import ir.pi0.irproject.io.DBReader;
import ir.pi0.irproject.proecessors.*;
import ir.pi0.irproject.proecessors.lemmatizer.Lemmatizer;
import ir.pi0.irproject.repository.WordDict;
import ir.pi0.irproject.repository.WordDictItem;
import ir.pi0.irproject.utils.Util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class Main {

    void benchmark(String path) throws Exception {

        DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(new File("benchmark.txt")));

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

    void normalize(String path,String out) {

        System.out.println("Normalizing data base");

        IProcessor[] p = {
                new Normalizer(),
                new StopWordRemover("PersianStopWords.txt"),
                new Lemmatizer(),
                new Sorter(),
        };

        DBProcessor processor =
                new DBProcessor(Arrays.asList(p), path, out);
        processor.process();

        System.out.println("Saved to: "+out);
    }



    void index(String path,String out) throws Exception {

        System.out.println("Indexing database");

        WordDict wordDict = new WordDict();

        IProcessor[] p = {
                new Indexer(wordDict)
        };

        DBProcessor processor =
                new DBProcessor(Arrays.asList(p), path, null);
        processor.process();

        wordDict.save(new File(out));

        System.out.println("Saved to: "+out);

    }



    public static void main(String[] args) throws Exception {

        Main main = new Main();

        if (args.length < 1) {
            printUsage();
            return;
        }

        switch (args[0].charAt(0)) {
            case 'b':
                main.benchmark(args[1]);
                break;
            case 'i':
                main.index(args[1],args[2]);
                break;
            case 'n':
                main.normalize(args[1],args[2]);
                break;
            default:
                printUsage();
                break;
        }

    }

    public static void printUsage() {
        System.out.println("Usage : ./run.sh b|i|n [input file] [output file]");
    }
}
