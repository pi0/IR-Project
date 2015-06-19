package ir.pi0.irproject.structures;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TLongIntProcedure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileBackedLongIntHashMapItem {

    RandomAccessFile raf = null;
    final TLongIntHashMap update = new TLongIntHashMap();

    public FileBackedLongIntHashMapItem(int key, File parent) {
        File article = new File(parent, String.valueOf(key));

        if (!article.exists())
            try {
                article.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        try {
            raf = new RandomAccessFile(article, "rwd");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void update_entry(Long key, int update_inc) {
        synchronized (update) {
            update.adjustOrPutValue(key, update_inc, update_inc);
            if (update.size() > 1000) {
                System.out.println("BOO: "+key);
                update.clear();
                //flush_entries();
            }
        }

    }

    private void flush_entries() {
        try {
            System.out.println("Updating ");
            for (raf.seek(0); raf.getFilePointer() < raf.length(); ) {
                long loc_key = raf.readLong();
                if (update.containsKey(loc_key)) {
                    int val = raf.readInt() + update.get(loc_key);
                    raf.seek(raf.getFilePointer() - 4);
                    raf.writeInt(val);
                    update.remove(loc_key);
                    break;
                } else
                    raf.skipBytes(4);//skip val
            }

            //Now create new keys
            //Not found : add entry
            update.forEachEntry(new TLongIntProcedure() {
                @Override
                public boolean execute(long key, int val) {
                    try {
                        raf.setLength(raf.length() + 12);
                        raf.seek(raf.length() - 12);
                        raf.writeLong(key);
                        raf.writeInt(val);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }
            });
            update.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void close() {
        try {
            synchronized (update) {
                flush_entries();
            }
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
