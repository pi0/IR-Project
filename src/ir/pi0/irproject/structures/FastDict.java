package ir.pi0.irproject.structures;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FastDict<T extends Comparable<T>> {

    private final T[] items;

    public FastDict(List<T> array,Class<T> tClass){
        Collections.sort(array);

        items= (T[]) Array.newInstance(tClass,array.size());
        array.toArray(items);

    }

    public FastDict(T[] array,Class<T> tClass){
        this(Arrays.asList(array),tClass);
    }

    public boolean contains(T c) {
        /*
        http://www.programcreek.com/2014/04/check-if-array-contains-a-value-java/
         */
        return java.util.Arrays.binarySearch(items, c)>=0;
    }

}
