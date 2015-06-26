package ir.pi0.irproject.repository;

import java.io.*;

public class WordPosting {

    private Writer writer;

    public WordPosting(File file) {
        try {
            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void append(int id) {
        try {
            writer.write(id + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
