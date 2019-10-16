package libs.espressif.collection;

import java.util.List;

import libs.espressif.function.FilterFunction;

public class EspCollections {
    public static <E> int index(List<E> list, FilterFunction<E> function) {
        int index = 0;
        for (E e : list) {
            if (function.filter(e)) {
                return index;
            }

            ++index;
        }

        return -1;
    }
}
