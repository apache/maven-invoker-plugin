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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugins.invoker.AbstractInvokerMojo.ToolchainPrivateManager;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SelectorUtils}.
 *
 * @author Benjamin Bentmann
 */
class SelectorUtilsTest {

    @Test
    void parseList() {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();

        SelectorUtils.parseList(null, includes, excludes);

        SelectorUtils.parseList(" 1.5, !1.4, 1.6+ ", includes, excludes);
        assertThat(includes).containsExactly("1.5", "1.6+");
        assertThat(excludes).containsExactly("1.4");
    }

    @Test
    void parseVersion() {
        assertThat(SelectorUtils.parseVersion("1.6.0_12")).containsExactly(1, 6, 0, 12);
        assertThat(SelectorUtils.parseVersion("1.6.0_12+")).containsExactly(1, 6, 0, 12);
        assertThat(SelectorUtils.parseVersion("1.6.0_12-")).containsExactly(1, 6, 0, 12);
    }

    @Test
    void compareVersions() {
        assertThat(SelectorUtils.compareVersions(Arrays.asList(1, 6), Arrays.asList(1, 6)))
                .isZero();

        assertThat(SelectorUtils.compareVersions(Arrays.asList(1, 5), Arrays.asList(1, 6)))
                .isNegative();
        assertThat(SelectorUtils.compareVersions(Arrays.asList(1, 6), Arrays.asList(1, 5)))
                .isPositive();

        assertThat(SelectorUtils.compareVersions(Collections.singletonList(1), Arrays.asList(1, 6)))
                .isNegative();
        assertThat(SelectorUtils.compareVersions(Arrays.asList(1, 6), Collections.singletonList(1)))
                .isPositive();
    }

    @Test
    void isMatchingJre() {

        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 4, 2, 8), "1.5")).isFalse();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5), "1.5")).isTrue();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5, 9), "1.5")).isTrue();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 6), "1.5")).isFalse();

        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 4, 2, 8), "1.5+"))
                .isFalse();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5), "1.5+")).isTrue();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5, 9), "1.5+")).isTrue();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 6), "1.5+")).isTrue();

        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 4, 2, 8), "1.5-"))
                .isTrue();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5), "1.5-")).isFalse();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 5, 9), "1.5-")).isFalse();
        assertThat(SelectorUtils.isJreVersion(Arrays.asList(1, 6), "1.5-")).isFalse();

        assertThat(SelectorUtils.isJreVersion((String) null, "1.5")).isTrue();
        assertThat(SelectorUtils.isJreVersion("", "1.5")).isTrue();
    }

    @Test
    void isMatchingToolchain() throws Exception {
        InvokerToolchain openJdk9 = new InvokerToolchain("jdk");
        openJdk9.addProvides("version", "9");
        openJdk9.addProvides("vendor", "openJDK");

        InvokerToolchain maven360 = new InvokerToolchain("maven");
        openJdk9.addProvides("version", "3.6.0");

        ToolchainPrivateManager toolchainPrivateManager = mock(ToolchainPrivateManager.class);
        ToolchainPrivate jdkMatching = mock(ToolchainPrivate.class);
        when(jdkMatching.matchesRequirements(anyMap())).thenReturn(true);
        when(jdkMatching.getType()).thenReturn("jdk");

        ToolchainPrivate jdkMismatch = mock(ToolchainPrivate.class);
        when(jdkMismatch.getType()).thenReturn("jdk");

        when(toolchainPrivateManager.getToolchainPrivates("jdk")).thenReturn(new ToolchainPrivate[] {jdkMatching});
        assertThat(SelectorUtils.isToolchain(toolchainPrivateManager, Collections.singleton(openJdk9)))
                .isTrue();

        when(toolchainPrivateManager.getToolchainPrivates("jdk")).thenReturn(new ToolchainPrivate[] {jdkMismatch});
        assertThat(SelectorUtils.isToolchain(toolchainPrivateManager, Collections.singleton(openJdk9)))
                .isFalse();

        when(toolchainPrivateManager.getToolchainPrivates("jdk"))
                .thenReturn(new ToolchainPrivate[] {jdkMatching, jdkMismatch, jdkMatching});
        assertThat(SelectorUtils.isToolchain(toolchainPrivateManager, Collections.singleton(openJdk9)))
                .isTrue();

        when(toolchainPrivateManager.getToolchainPrivates("jdk")).thenReturn(new ToolchainPrivate[0]);
        assertThat(SelectorUtils.isToolchain(toolchainPrivateManager, Collections.singleton(openJdk9)))
                .isFalse();

        when(toolchainPrivateManager.getToolchainPrivates("jdk")).thenReturn(new ToolchainPrivate[] {jdkMatching});
        when(toolchainPrivateManager.getToolchainPrivates("maven")).thenReturn(new ToolchainPrivate[0]);
        assertThat(SelectorUtils.isToolchain(toolchainPrivateManager, Arrays.asList(openJdk9, maven360)))
                .isFalse();
    }

    @Test
    void mavenVersionForNotExistingMavenHomeThrowException() {
        File mavenHome = new File("not-existing-path");

        assertThatCode(() -> SelectorUtils.getMavenVersion(mavenHome))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Maven home installation directory: not-existing-path");
    }

    @Test
    void mavenVersionFromMavenHome() throws Exception {
        File mavenHome = new File(System.getProperty("maven.home"));

        String mavenVersion = SelectorUtils.getMavenVersion(mavenHome);

        assertThat(mavenVersion).isNotBlank();
    }
}
