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
        var url = URI.create("https://$domain/Rothes/ESU/raw/refs/heads/raw/aether-library.jar").toURL();
        EsuBootstrap.Companion.getInstance().info(String.format("Downloading %s from %s", url, domain));
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
