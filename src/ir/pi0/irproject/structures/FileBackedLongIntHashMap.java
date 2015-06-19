package ir.pi0.irproject.structures;

import gnu.trove.map.hash.TIntObjectHashMap;
import ir.pi0.irproject.Consts;
import ir.pi0.irproject.utils.Util;

import java.io.File;

public class FileBackedLongIntHashMap {

    File file;
    int initial_capacity;

    final TIntObjectHashMap<FileBackedLongIntHashMapItem> items
            = new TIntObjectHashMap<>(Consts.PREDICTED_INITIAL_WORDS_COUNT);

    public FileBackedLongIntHashMap(int initial_capacity, File file, boolean clear) {
        this.initial_capacity = initial_capacity;
        this.file = file;

        if (clear && file.exists())
            try {
                Util.deleteRecursive(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        file.mkdirs();
    }

    public int size() {
        return -1;
    }

    private FileBackedLongIntHashMapItem getOrCreateItem(long _key) {

        int key = Util.splitLongToInt(_key)[0];

        FileBackedLongIntHashMapItem i;

        synchronized (items) {
            i = items.get(key);
            if (i == null) {
                items.put(key, i = new FileBackedLongIntHashMapItem(key, this.file));
            }
        }

        return i;
    }


    public void adjustOrPutValue(Long key, int by) {
        FileBackedLongIntHashMapItem i = getOrCreateItem(key);
        i.update_entry(key, by);

    }

    public void flush() {
        //TODO
    }
}
