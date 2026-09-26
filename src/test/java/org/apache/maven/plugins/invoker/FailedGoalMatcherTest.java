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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests matching of the goal reported as failed in a build log.
 */
class FailedGoalMatcherTest {

    private static final String ENFORCER_LINE =
            "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.6.2:enforce"
                    + " (enforce-rules) on project test: Some Enforcer rules have failed. -> [Help 1]";

    private static final String COMPILER_LINE =
            "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.14.1:compile"
                    + " (default-compile) on project test: Compilation failure -> [Help 1]";

    @Test
    void findFailedGoalsReadsCoordinatesAndExecutionId() {
        List<String> failedGoals = FailedGoalMatcher.findFailedGoals(
                Arrays.asList("[INFO] Building test 1.0", ENFORCER_LINE, "[ERROR] -> [Help 1]"));

        assertThat(failedGoals)
                .containsExactly("org.apache.maven.plugins:maven-enforcer-plugin:3.6.2:enforce (enforce-rules)");
    }

    @Test
    void findFailedGoalsKeepsEveryReportedGoal() {
        List<String> failedGoals = FailedGoalMatcher.findFailedGoals(Arrays.asList(ENFORCER_LINE, COMPILER_LINE));

        assertThat(failedGoals).hasSize(2);
    }

    @Test
    void findFailedGoalsWithoutExecutionId() {
        List<String> failedGoals = FailedGoalMatcher.findFailedGoals(Collections.singletonList(
                "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-clean-plugin:3.5.0:clean on project"
                        + " test: Failed to clean project"));

        assertThat(failedGoals).containsExactly("org.apache.maven.plugins:maven-clean-plugin:3.5.0:clean");
    }

    @Test
    void findFailedGoalsIgnoresLinesWithoutCoordinates() {
        List<String> failedGoals = FailedGoalMatcher.findFailedGoals(Arrays.asList(
                "[ERROR] Failed to execute goal on project test: Could not resolve dependencies",
                "[ERROR] BUILD FAILURE"));

        assertThat(failedGoals).isEmpty();
    }

    @Test
    void findFailedGoalsReadsABuildLog(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("build.log");
        Files.write(
                logFile,
                Arrays.asList("[INFO] Scanning for projects...", ENFORCER_LINE, "[ERROR] BUILD FAILURE"),
                Charset.defaultCharset());

        assertThat(FailedGoalMatcher.findFailedGoals(logFile.toFile()))
                .containsExactly("org.apache.maven.plugins:maven-enforcer-plugin:3.6.2:enforce (enforce-rules)");
    }

    @Test
    void matchesFullCoordinates() {
        assertThat(matcher("org.apache.maven.plugins:maven-enforcer-plugin:3.6.2:enforce")
                        .matchesAny(failedGoals(ENFORCER_LINE)))
                .isTrue();
    }

    @Test
    void matchesCoordinateSubsequences() {
        assertThat(matcher("maven-enforcer-plugin:enforce").matchesAny(failedGoals(ENFORCER_LINE)))
                .isTrue();
        assertThat(matcher("org.apache.maven.plugins:maven-enforcer-plugin:enforce")
                        .matchesAny(failedGoals(ENFORCER_LINE)))
                .isTrue();
        assertThat(matcher("enforce").matchesAny(failedGoals(ENFORCER_LINE))).isTrue();
    }

    @Test
    void doesNotMatchAnotherGoal() {
        assertThat(matcher("maven-compiler-plugin:compile").matchesAny(failedGoals(ENFORCER_LINE)))
                .isFalse();
        assertThat(matcher("maven-enforcer-plugin:enforce").matchesAny(failedGoals(COMPILER_LINE)))
                .isFalse();
    }

    @Test
    void doesNotMatchFragmentsOutOfOrder() {
        assertThat(matcher("enforce:maven-enforcer-plugin").matchesAny(failedGoals(ENFORCER_LINE)))
                .isFalse();
    }

    @Test
    void matchesExecutionIdWhenGiven() {
        assertThat(matcher("maven-enforcer-plugin:enforce (enforce-rules)").matchesAny(failedGoals(ENFORCER_LINE)))
                .isTrue();
        assertThat(matcher("maven-enforcer-plugin:enforce (other-rules)").matchesAny(failedGoals(ENFORCER_LINE)))
                .isFalse();
    }

    @Test
    void matchesOneOfSeveralFailedGoals() {
        assertThat(matcher("maven-compiler-plugin:compile").matchesAny(failedGoals(ENFORCER_LINE, COMPILER_LINE)))
                .isTrue();
    }

    @Test
    void doesNotMatchWhenLogHasNoFailedGoal() {
        assertThat(matcher("maven-enforcer-plugin:enforce").matchesAny(Collections.emptyList()))
                .isFalse();
    }

    @Test
    void rejectsSpecificationWithoutCoordinates() {
        assertThatThrownBy(() -> matcher(" (enforce-rules) ")).isInstanceOf(IllegalArgumentException.class);
    }

    private static FailedGoalMatcher matcher(String spec) {
        return new FailedGoalMatcher(spec);
    }

    private static List<String> failedGoals(String... logLines) {
        return FailedGoalMatcher.findFailedGoals(Arrays.asList(logLines));
    }
}
