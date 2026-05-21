/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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
