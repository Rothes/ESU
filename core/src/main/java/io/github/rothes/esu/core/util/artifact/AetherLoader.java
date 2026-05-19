/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.util.artifact;

import io.github.rothes.esu.core.EsuBootstrap;
import io.github.rothes.esu.core.util.artifact.injector.UnsafeURLInjector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

public class AetherLoader {

    private AetherLoader() {}

    public static void loadAether() {
        try {
            Class.forName("org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory");
        } catch (ClassNotFoundException ignored) {
            // Spigot 1.16.5 and older, No Aether library bounded
            resolveAetherLib();
        }
    }

    private static void resolveAetherLib() {
        try {
            var resolve = EsuBootstrap.Companion.getInstance().baseConfigPath().resolve(".cache/aether-library.jar").toFile();
            if (!resolve.exists() || !HashUtils.calculateSha1(resolve).equals("f2bbafed1dd38ffdbaac1daf17ca706efbec74ef")) {
                try {
                    downloadAetherLib("ghfast.top/https://github.com", resolve);
                } catch (IOException ignored) {
                    EsuBootstrap.Companion.getInstance().info("Connection error, fallback to another route");
                    downloadAetherLib("github.com", resolve);
                }
            }
            UnsafeURLInjector.INSTANCE.addURL(resolve.toURI().toURL());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void downloadAetherLib(String domain, File resolve) throws IOException {
        var url = URI.create("https://" + domain + "/Rothes/ESU/raw/refs/heads/raw/aether-library.jar").toURL();
        EsuBootstrap.Companion.getInstance().info(String.format("Downloading %s to %s", url, resolve.toString()));
        resolve.getParentFile().mkdirs();
        resolve.createNewFile();
        try (
                var in = url.openStream();
                var out = new FileOutputStream(resolve)
        ) {
            var buffer = new byte[8192];
            while (true) {
                int read = in.read(buffer);
                if (read == -1) break;
                out.write(buffer, 0, read);
            }
        }
    }

}
