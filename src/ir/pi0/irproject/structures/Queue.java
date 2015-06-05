package ir.pi0.irproject.structures;

import java.util.ArrayList;

public class Queue<T> extends ArrayList<T> {

    public T poll() {
        if (isEmpty())
            return null;
        T r = get(0);
        remove(0);
        return r;
    }

    public T top() {
        if (isEmpty())
            return null;
        T r = get(0);
        return r;
    }


    public boolean enqueue(T s) {
        return add(s);
    }

}
