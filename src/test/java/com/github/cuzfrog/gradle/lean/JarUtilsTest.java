package com.github.cuzfrog.gradle.lean;

import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vafer.jdependency.Clazz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

final class JarUtilsTest {

    private final Path tmpDir = TestFileSystem.createConcreteTempDir();

    private final JarMan mockJarMan = mock(JarMan.class);
    private final JarUtils jarUtils = new JarUtils(mockJarMan);
    private final Path archive = genTestJar(tmpDir.resolve("my-test.jar"));

    private final ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Set<Clazz>> clazzesCaptor = ArgumentCaptor.forClass(Set.class);

    @AfterEach
    void clear() {
        TestFileSystem.deleteDir(tmpDir);
    }

    @Test
    void internalMinimizeJars() throws Exception {
        final Path libSourceDir = Paths.get(Resources.getResource("testJars/lib").toURI());
        final Path libDir = Files.createDirectory(tmpDir.resolve("test-lib"));
        final List<Path> libJars = new ArrayList<>();
        for (final Path sourceJar : (Iterable<Path>) Files.list(libSourceDir)::iterator) {
            final Path targetLibJar = libDir.resolve(sourceJar.getFileName());
            Files.copy(sourceJar, targetLibJar);
            libJars.add(targetLibJar);
        }

        jarUtils.internalMinimizeJars(archive, libDir);

        verify(mockJarMan, times(2)).removeEntry(pathCaptor.capture(), clazzesCaptor.capture());
        assertThat(pathCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(libJars);
        assertThat(clazzesCaptor.getValue()).isNotEmpty();
    }

    private static Path genTestJar(final Path jarPath) {
        ZipFsUtils.onZipFileSystem(jarPath, zipfs -> {
            try {
                final Path targetDir = zipfs.getPath("com/github/cuzfrog/gradle/lean");
                Files.createDirectories(targetDir);
                final Path source = Paths.get(Resources.getResource("testJars/TestFileSystem.class").toURI());
                Files.copy(source, targetDir.resolve("TestFileSystem.class"));
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }, true);
        return jarPath;
    }
}