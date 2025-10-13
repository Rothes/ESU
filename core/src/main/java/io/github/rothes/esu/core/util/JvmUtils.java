package io.github.rothes.esu.core.util;

public class JvmUtils {

    private JvmUtils() {}

    public static int compareLong(long a, long b) {
        return Long.compare(a, b);
    }

}
