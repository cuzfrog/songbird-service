package com.github.cuzfrog.gradle.lean;

import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vafer.jdependency.Clazz;
import org.vafer.jdependency.Clazzpath;
import org.vafer.jdependency.ClazzpathUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class MinimizerImpl implements Minimizer {
    private static final Logger logger = LoggerFactory.getLogger(MinimizerImpl.class);

    private final JarMan jarMan;
    private final List<String> excludedClasses;
    private final List<String> excludedDependencies;

    @VisibleForTesting
    MinimizerImpl(final JarMan jarMan,
                  final Collection<String> excludedClasses,
                  final Collection<String> excludedDependencies) {
        this.jarMan = jarMan;
        this.excludedClasses = new ArrayList<>(excludedClasses);
        this.excludedDependencies = new ArrayList<>(excludedDependencies);
    }

    @Override
    public void minimize(final Path archivePath, final Path libDir) {
        try {
            final Clazzpath cp = new Clazzpath();
            final ClazzpathUnit artifact = cp.addClazzpathUnit(archivePath);

            final List<Path> dependencyJars = getLibDependencyJars(libDir, archivePath.getFileName().toString());
            for (final Path jar : dependencyJars) {
                cp.addClazzpathUnit(jar, jar.getFileName().toString());
            }

            final Set<Clazz> removable = cp.getClazzes();
            removable.removeAll(artifact.getClazzes());
            removable.removeAll(artifact.getTransitiveDependencies());

            for (final Path jar : dependencyJars) {
                logger.trace("Try to minimize jar '{}'", jar);
                jarMan.removeEntry(jar, removable);
            }
        }catch (final IOException e){
            throw new RuntimeException("Error happened while minimizing jars in dir:" + libDir, e);
        }
    }

    private static List<Path> getLibDependencyJars(final Path libDir, final String artifactName) throws IOException {
        return Files.list(libDir)
                .filter(p -> {
                    final String filename = p.getFileName().toString();
                    return filename.endsWith(".jar") && !filename.equals(artifactName);
                })
                .collect(Collectors.toList());
    }
}