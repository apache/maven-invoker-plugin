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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Matches the goal a forked Maven build reported as failed against an <code>invoker.failedGoal</code> specification.
 * <p>
 * A specification is a colon separated list of coordinate fragments, optionally followed by an execution id in
 * parentheses, for example <code>maven-enforcer-plugin:enforce (enforce-rules)</code>. The fragments have to occur, in
 * the given order, among the <code>groupId:artifactId:version:goal</code> segments Maven logged, so a test can name as
 * little or as much of the coordinates as it cares about. When an execution id is given, it has to match exactly.
 */
class FailedGoalMatcher {

    /**
     * The message Maven prints for a failed mojo execution, both in Maven 3 and Maven 4.
     */
    private static final Pattern FAILED_GOAL_LINE =
            Pattern.compile("Failed to execute goal (\\S+?)(?: \\(([^)]*)\\))? on project ");

    private final String spec;

    private final Goal expected;

    /**
     * Creates a matcher for the given specification.
     *
     * @param spec the value of an <code>invoker.failedGoal</code> property, must not be <code>null</code>
     * @throws IllegalArgumentException if the specification names no coordinate fragment
     */
    FailedGoalMatcher(String spec) {
        this.spec = spec;
        this.expected = Goal.parse(spec);
        if (expected.coordinates.isEmpty()) {
            throw new IllegalArgumentException("invalid failed goal specification: " + spec);
        }
    }

    /**
     * Collects the goals a build log reports as failed. A build log is not guaranteed to hold well-formed text, so
     * bytes the platform encoding cannot decode are replaced rather than reported.
     *
     * @param logFile a <code>build.log</code>, must not be <code>null</code>
     * @return the failed goals in order of appearance, each as logged by Maven, never <code>null</code>
     * @throws IOException if the log file could not be read
     */
    static List<String> findFailedGoals(File logFile) throws IOException {
        CharsetDecoder decoder = Charset.defaultCharset()
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        List<String> failedGoals = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(Files.newInputStream(logFile.toPath()), decoder))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                findFailedGoal(line).ifPresent(failedGoals::add);
            }
        }
        return failedGoals;
    }

    /**
     * Collects the goals a build log reports as failed.
     *
     * @param logLines the lines of a <code>build.log</code>, must not be <code>null</code>
     * @return the failed goals in order of appearance, each as logged by Maven, never <code>null</code>
     */
    static List<String> findFailedGoals(Collection<String> logLines) {
        List<String> failedGoals = new ArrayList<>();
        for (String line : logLines) {
            findFailedGoal(line).ifPresent(failedGoals::add);
        }
        return failedGoals;
    }

    private static Optional<String> findFailedGoal(String logLine) {
        Matcher matcher = FAILED_GOAL_LINE.matcher(logLine);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String executionId = matcher.group(2);
        return Optional.of(executionId != null ? matcher.group(1) + " (" + executionId + ")" : matcher.group(1));
    }

    /**
     * Checks whether any of the given failed goals matches this specification.
     *
     * @param failedGoals the failed goals as returned by {@link #findFailedGoals(Collection)}
     * @return <code>true</code> if at least one goal matches
     */
    boolean matchesAny(Collection<String> failedGoals) {
        return failedGoals.stream().anyMatch(this::matches);
    }

    /**
     * Checks whether the given failed goal matches this specification.
     *
     * @param failedGoal a failed goal as logged by Maven
     * @return <code>true</code> if the goal matches
     */
    boolean matches(String failedGoal) {
        Goal actual = Goal.parse(failedGoal);
        if (expected.executionId != null && !Objects.equals(expected.executionId, actual.executionId)) {
            return false;
        }
        return isSubsequence(expected.coordinates, actual.coordinates);
    }

    @Override
    public String toString() {
        return spec;
    }

    private static boolean isSubsequence(List<String> fragments, List<String> segments) {
        int segment = 0;
        for (String fragment : fragments) {
            while (segment < segments.size() && !fragment.equals(segments.get(segment))) {
                segment++;
            }
            if (segment == segments.size()) {
                return false;
            }
            segment++;
        }
        return true;
    }

    /**
     * A goal reference, split into its coordinate segments and its optional execution id.
     */
    private static final class Goal {

        private final List<String> coordinates;

        private final String executionId;

        private Goal(List<String> coordinates, String executionId) {
            this.coordinates = coordinates;
            this.executionId = executionId;
        }

        private static Goal parse(String value) {
            String coordinates = value.trim();
            String executionId = null;
            int parenthesis = coordinates.indexOf('(');
            if (parenthesis >= 0 && coordinates.endsWith(")")) {
                executionId = coordinates
                        .substring(parenthesis + 1, coordinates.length() - 1)
                        .trim();
                coordinates = coordinates.substring(0, parenthesis).trim();
            }
            return new Goal(split(coordinates), executionId);
        }

        private static List<String> split(String coordinates) {
            if (coordinates.isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(coordinates.split(":"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}
