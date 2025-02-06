package io.github.rothes.esu.bukkit.utils;

public class JvmArrayUtils {

    private JvmArrayUtils() {}

    public static boolean[][][] newBoolArray(int x, int y, int z) {
        return new boolean[x][y][z];
    }

}
