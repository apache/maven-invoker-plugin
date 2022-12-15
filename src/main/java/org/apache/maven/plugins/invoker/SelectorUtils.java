/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.invoker;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.plugins.invoker.AbstractInvokerMojo.ToolchainPrivateManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides utility methods for selecting build jobs based on environmental conditions.
 *
 * @author Benjamin Bentmann
 */
class SelectorUtils {

    static void parseList(String list, Collection<String> includes, Collection<String> excludes) {
        String[] tokens = (list != null) ? StringUtils.split(list, ",") : new String[0];

        for (String token1 : tokens) {
            String token = token1.trim();

            if (token.startsWith("!")) {
                excludes.add(token.substring(1));
            } else {
                includes.add(token);
            }
        }
    }

    static boolean isOsFamily(String osSpec) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        parseList(osSpec, includes, excludes);

        return isOsFamily(includes, true) && !isOsFamily(excludes, false);
    }

    static boolean isOsFamily(List<String> families, boolean defaultMatch) {
        if (families != null && !families.isEmpty()) {
            for (String family : families) {
                if (Os.isFamily(family)) {
                    return true;
                }
            }

            return false;
        } else {
            return defaultMatch;
        }
    }

    /**
     * Retrieves the current Maven version.
     *
     * @return The current Maven version.
     */
    static String getMavenVersion() {
        try {
            // This relies on the fact that MavenProject is the in core classloader
            // and that the core classloader is for the maven-core artifact
            // and that should have a pom.properties file
            // if this ever changes, we will have to revisit this code.
            Properties properties = new Properties();
            // CHECKSTYLE_OFF: LineLength
            properties.load(MavenProject.class
                    .getClassLoader()
                    .getResourceAsStream("META-INF/maven/org.apache.maven/maven-core/pom.properties"));
            // CHECKSTYLE_ON: LineLength
            return StringUtils.trim(properties.getProperty("version"));
        } catch (Exception e) {
            return null;
        }
    }

    static String getMavenVersion(File mavenHome) throws IOException {
        File mavenLib = new File(mavenHome, "lib");
        File[] jarFiles = mavenLib.listFiles((dir, name) -> name.endsWith(".jar"));

        if (jarFiles == null) {
            throw new IllegalArgumentException("Invalid Maven home installation directory: " + mavenHome);
        }

        for (File file : jarFiles) {
            try {
                URL url = new URL("jar:" + file.toURI().toURL().toExternalForm()
                        + "!/META-INF/maven/org.apache.maven/maven-core/pom.properties");

                try (InputStream in = url.openStream()) {
                    Properties properties = new Properties();
                    properties.load(in);
                    String version = StringUtils.trim(properties.getProperty("version"));
                    if (version != null) {
                        return version;
                    }
                }
            } catch (FileNotFoundException | MalformedURLException e) {
                // ignore
            }
        }
        return null;
    }

    static boolean isMavenVersion(String mavenSpec) {
        return isMavenVersion(mavenSpec, getMavenVersion());
    }

    static boolean isMavenVersion(String mavenSpec, String actualVersion) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        parseList(mavenSpec, includes, excludes);

        List<Integer> mavenVersionList = parseVersion(actualVersion);

        return isJreVersion(mavenVersionList, includes, true) && !isJreVersion(mavenVersionList, excludes, false);
    }

    static String getJreVersion() {
        return System.getProperty("java.version", "");
    }

    static String getJreVersion(File javaHome) {
        // @todo detect actual version
        return null;
    }

    static boolean isJreVersion(String jreSpec) {
        return isJreVersion(jreSpec, getJreVersion());
    }

    static boolean isJreVersion(String jreSpec, String actualJreVersion) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        parseList(jreSpec, includes, excludes);

        List<Integer> jreVersion = parseVersion(actualJreVersion);

        return isJreVersion(jreVersion, includes, true) && !isJreVersion(jreVersion, excludes, false);
    }

    static boolean isJreVersion(List<Integer> jreVersion, List<String> versionPatterns, boolean defaultMatch) {
        if (versionPatterns != null && !versionPatterns.isEmpty()) {
            for (String versionPattern : versionPatterns) {
                if (isJreVersion(jreVersion, versionPattern)) {
                    return true;
                }
            }

            return false;
        } else {
            return defaultMatch;
        }
    }

    static boolean isJreVersion(List<Integer> jreVersion, String versionPattern) {
        List<Integer> checkVersion = parseVersion(versionPattern);

        if (versionPattern.endsWith("+")) {
            // 1.5+ <=> [1.5,)
            return compareVersions(jreVersion, checkVersion) >= 0;
        } else if (versionPattern.endsWith("-")) {
            // 1.5- <=> (,1.5)
            return compareVersions(jreVersion, checkVersion) < 0;
        } else {
            // 1.5 <=> [1.5,1.6)
            return checkVersion.size() <= jreVersion.size()
                    && checkVersion.equals(jreVersion.subList(0, checkVersion.size()));
        }
    }

    static List<Integer> parseVersion(String version) {
        version = version.replaceAll("[^0-9]", ".");

        String[] tokens = StringUtils.split(version, ".");

        List<Integer> numbers = Arrays.stream(tokens).map(Integer::valueOf).collect(Collectors.toList());

        return numbers;
    }

    static int compareVersions(List<Integer> version1, List<Integer> version2) {
        for (Iterator<Integer> it1 = version1.iterator(), it2 = version2.iterator(); ; ) {
            if (!it1.hasNext()) {
                return it2.hasNext() ? -1 : 0;
            }
            if (!it2.hasNext()) {
                return it1.hasNext() ? 1 : 0;
            }

            Integer num1 = it1.next();
            Integer num2 = it2.next();

            int rel = num1.compareTo(num2);
            if (rel != 0) {
                return rel;
            }
        }
    }

    /**
     * @param toolchainPrivateManager
     * @param invokerToolchains
     * @return {@code true} if all invokerToolchains are available, otherwise {@code false}
     */
    static boolean isToolchain(
            ToolchainPrivateManager toolchainPrivateManager, Collection<InvokerToolchain> invokerToolchains) {
        for (InvokerToolchain invokerToolchain : invokerToolchains) {
            boolean found = false;
            try {
                for (ToolchainPrivate tc : toolchainPrivateManager.getToolchainPrivates(invokerToolchain.getType())) {
                    if (!invokerToolchain.getType().equals(tc.getType())) {
                        // useful because of MNG-5716
                        continue;
                    }

                    if (tc.matchesRequirements(invokerToolchain.getProvides())) {
                        found = true;
                        continue;
                    }
                }
            } catch (MisconfiguredToolchainException e) {
                return false;
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }
}
