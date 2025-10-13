package io.github.rothes.esu.core.util.artifact;

import com.google.common.io.BaseEncoding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    private HashUtils() {}

    public static String calculateSha1(File file) {
        try {
            var messageDigest = MessageDigest.getInstance("SHA-1");
            try (var input = new FileInputStream(file)) {
                var buffer = new byte[8192];
                while (true) {
                    int read = input.read(buffer);
                    if (read == -1) break;
                    messageDigest.update(buffer, 0, read);
                }
            }
            var hash = messageDigest.digest();

            return BaseEncoding.base16().lowerCase().encode(hash);

        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
