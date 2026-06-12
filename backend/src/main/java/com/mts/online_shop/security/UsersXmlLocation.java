package com.mts.online_shop.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Единый путь к runtime-файлу users.xml (WildFly data dir или ./data).
 */
public final class UsersXmlLocation {

    public static final String XML_FILE_NAME = "users.xml";
    public static final String SYSTEM_PROPERTY = "mts.users.xml.path";

    private UsersXmlLocation() {
    }

    public static Path resolve() {
        String configured = System.getProperty(SYSTEM_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        String jbossDataDir = System.getProperty("jboss.server.data.dir");
        if (jbossDataDir != null && !jbossDataDir.isBlank()) {
            return Paths.get(jbossDataDir, "mts-online-shop", XML_FILE_NAME);
        }
        return Paths.get(System.getProperty("user.dir"), "data", XML_FILE_NAME);
    }

    public static void publish(Path path) {
        System.setProperty(SYSTEM_PROPERTY, path.toAbsolutePath().toString());
    }

    public static void migrateLegacyIfNeeded(Path targetPath) throws java.io.IOException {
        if (Files.exists(targetPath)) {
            return;
        }
        Path legacyPath = Paths.get(System.getProperty("user.dir"), "data", XML_FILE_NAME);
        if (Files.exists(legacyPath)) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(legacyPath, targetPath);
        }
    }
}
