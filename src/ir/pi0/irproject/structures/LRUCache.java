package ir.pi0.irproject.structures;

import java.util.Map;
import java.util.function.Consumer;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final Consumer<V> afterRemovalFunc;
    private int cacheSize;

    public LRUCache(int cacheSize,Consumer<V> afterRemovalFunc) {
        super(16, (float) 0.75, true);
        this.cacheSize = cacheSize;
        this.afterRemovalFunc=afterRemovalFunc;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= cacheSize;
    }

    @Override
    void afterNodeRemoval(Node<K, V> e) {
        super.afterNodeRemoval(e);
        if(afterRemovalFunc!=null)
            afterRemovalFunc.accept(e.value);
    }
}
