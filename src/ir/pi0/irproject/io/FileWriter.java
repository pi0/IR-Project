package ir.pi0.irproject.io;

import java.io.*;
import java.util.Scanner;

public class FileWriter {

    protected boolean err = false;
    protected boolean opened = false;
    protected File file;
    protected int buffer_size;
    OutputStream outputStream;
    BufferedWriter writer;


    public FileWriter(String path) {
        this.file = new File(path);
    }

    public boolean open(int buffer_size) {
        if (!opened)
            try {

                outputStream = new FileOutputStream(file);

                writer = new BufferedWriter(
                        new OutputStreamWriter(outputStream),buffer_size);

                this.buffer_size = buffer_size;
                opened = true;

            } catch (Exception e) {
                err = true;
                System.err.println("Error Opening DB for Write :" + e.getMessage());
            }
        return opened;
    }

    public void write(String str) {
        try {
            writer.write(str);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
