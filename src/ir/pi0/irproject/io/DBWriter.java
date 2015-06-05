package ir.pi0.irproject.io;

import ir.pi0.irproject.Consts;

import java.util.ArrayList;
import java.util.List;

public class DBWriter extends FileWriter {

    private String tag;

    public DBWriter(String path) {
        super(path);
        this.tag = Consts.DB_TAG;
    }

    public void write(List<String> words) {

        StringBuilder out = new StringBuilder();

        out.append(tag).append("\n");

        for (String word : words) {
            if (word == null)
                continue;//Removed
            if (word.length() > 0)
                out.append(word).append(" ");
        }
        out.append("\n");

        write(out.toString());

    }

    public void close() {
        try {
            write(new ArrayList<String>());//Write empty ending tag
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
