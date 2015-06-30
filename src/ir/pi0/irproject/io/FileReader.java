package ir.pi0.irproject.io;

import java.io.*;

public class FileReader {

    protected boolean err = false;
    protected boolean opened = false;
    protected File file;
    public int buffer_size;
    InputStream inputStream;
    ArticleBufferedReader bReader;
    int total_size;
    public FileReader(String path) {
        this.file = new File(path);
    }

    public boolean open(int buffer_size) {
        if (!opened)
            try {

                inputStream = new FileInputStream(file);

                bReader = new ArticleBufferedReader(
                        new InputStreamReader(inputStream),buffer_size);

                total_size=available();

                this.buffer_size = buffer_size;
                opened = true;

//                System.out.println("File Opened with size of " +
//                        Util.humanReadableByteCount(available(), false));

            } catch (Exception e) {
                err = true;
                System.err.println("Error Opening DB :" + e.getMessage());
            }
        return opened;
    }

    public int available() {

        try {
            return inputStream.available();
        } catch (IOException e) {
            return -1;
        }

    }

    public int pos() {
        return total_size-available();
    }


    public ArticleBufferedReader getReader() {
        return bReader;
    }
}
