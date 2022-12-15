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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides a convenient facade around the <code>invoker.properties</code>.
 *
 * @author Benjamin Bentmann
 */
class InvokerProperties {
    private static final String SELECTOR_PREFIX = "selector.";

    private static final Pattern ENVIRONMENT_VARIABLES_PATTERN =
            Pattern.compile("invoker\\.environmentVariables\\.([A-Za-z][^.]+)(\\.([0-9]+))?");

    // default values from Mojo configuration
    private Boolean defaultDebug;
    private Boolean defaultQuiet;
    private List<String> defaultGoals;
    private List<String> defaultProfiles;
    private String defaultMavenOpts;
    private Integer defaultTimeoutInSeconds;
    private Map<String, String> defaultEnvironmentVariables;
    private File defaultMavenExecutable;
    private Boolean defaultUpdateSnapshots;

    private enum InvocationProperty {
        PROJECT("invoker.project"),
        BUILD_RESULT("invoker.buildResult"),
        GOALS("invoker.goals"),
        PROFILES("invoker.profiles"),
        MAVEN_EXECUTABLE("invoker.mavenExecutable"),
        MAVEN_OPTS("invoker.mavenOpts"),
        FAILURE_BEHAVIOR("invoker.failureBehavior"),
        NON_RECURSIVE("invoker.nonRecursive"),
        OFFLINE("invoker.offline"),
        SYSTEM_PROPERTIES_FILE("invoker.systemPropertiesFile"),
        DEBUG("invoker.debug"),
        QUIET("invoker.quiet"),
        SETTINGS_FILE("invoker.settingsFile"),
        TIMEOUT_IN_SECONDS("invoker.timeoutInSeconds"),
        UPDATE_SNAPSHOTS("invoker.updateSnapshots");

        private final String key;

        InvocationProperty(final String s) {
            this.key = s;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    private enum SelectorProperty {
        JAVA_VERSION(".java.version"),
        MAVEN_VERSION(".maven.version"),
        OS_FAMLY(".os.family");

        private final String suffix;

        SelectorProperty(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String toString() {
            return suffix;
        }
    }

    /**
     * The invoker properties being wrapped.
     */
    private final Properties properties;

    /**
     * Creates a new facade for the specified invoker properties. The properties will not be copied, so any changes to
     * them will be reflected by the facade.
     *
     * @param properties The invoker properties to wrap, may be <code>null</code> if none.
     */
    InvokerProperties(Properties properties) {
        this.properties = (properties != null) ? properties : new Properties();
    }

    /**
     * Default value for debug
     * @param defaultDebug a default value
     */
    public void setDefaultDebug(boolean defaultDebug) {
        this.defaultDebug = defaultDebug;
    }

    /**
     * Default value for quiet
     * @param defaultQuiet a default value
     */
    public void setDefaultQuiet(boolean defaultQuiet) {
        this.defaultQuiet = defaultQuiet;
    }

    /**
     * Default value for goals
     * @param defaultGoals a default value
     */
    public void setDefaultGoals(List<String> defaultGoals) {
        this.defaultGoals = defaultGoals;
    }

    /**
     * Default value for profiles
     * @param defaultProfiles a default value
     */
    public void setDefaultProfiles(List<String> defaultProfiles) {
        this.defaultProfiles = defaultProfiles;
    }

    /**
     * Default value for mavenExecutable
     * @param defaultMavenExecutable a default value
     */
    public void setDefaultMavenExecutable(File defaultMavenExecutable) {
        this.defaultMavenExecutable = defaultMavenExecutable;
    }

    /**
     * Default value for mavenOpts
     * @param defaultMavenOpts a default value
     */
    public void setDefaultMavenOpts(String defaultMavenOpts) {
        this.defaultMavenOpts = defaultMavenOpts;
    }

    /**
     * Default value for timeoutInSeconds
     * @param defaultTimeoutInSeconds a default value
     */
    public void setDefaultTimeoutInSeconds(int defaultTimeoutInSeconds) {
        this.defaultTimeoutInSeconds = defaultTimeoutInSeconds;
    }

    /**
     * Default value for environmentVariables
     * @param defaultEnvironmentVariables a default value
     */
    public void setDefaultEnvironmentVariables(Map<String, String> defaultEnvironmentVariables) {
        this.defaultEnvironmentVariables = defaultEnvironmentVariables;
    }

    /**
     * Default value for updateSnapshots
     * @param defaultUpdateSnapshots a default value
     */
    public void setDefaultUpdateSnapshots(boolean defaultUpdateSnapshots) {
        this.defaultUpdateSnapshots = defaultUpdateSnapshots;
    }

    /**
     * Gets the invoker properties being wrapped.
     *
     * @return The invoker properties being wrapped, never <code>null</code>.
     */
    public Properties getProperties() {
        return this.properties;
    }

    /**
     * Gets the name of the corresponding build job.
     *
     * @return The name of the build job or an empty string if not set.
     */
    public String getJobName() {
        return this.properties.getProperty("invoker.name", "");
    }

    /**
     * Gets the description of the corresponding build job.
     *
     * @return The description of the build job or an empty string if not set.
     */
    public String getJobDescription() {
        return this.properties.getProperty("invoker.description", "");
    }

    /**
     * Get the corresponding ordinal value
     *
     * @return The ordinal value
     */
    public int getOrdinal() {
        return Integer.parseInt(this.properties.getProperty("invoker.ordinal", "0"));
    }

    /**
     * Gets the specification of JRE versions on which this build job should be run.
     *
     * @return The specification of JRE versions or an empty string if not set.
     */
    public String getJreVersion() {
        return this.properties.getProperty("invoker.java.version", "");
    }

    /**
     * Gets the specification of JRE versions on which this build job should be run.
     *
     * @return The specification of JRE versions or an empty string if not set.
     */
    public String getJreVersion(int index) {
        return this.properties.getProperty(SELECTOR_PREFIX + index + SelectorProperty.JAVA_VERSION, getJreVersion());
    }

    /**
     * Gets the specification of Maven versions on which this build job should be run.
     *
     * @return The specification of Maven versions on which this build job should be run.
     * @since 1.5
     */
    public String getMavenVersion() {
        return this.properties.getProperty("invoker.maven.version", "");
    }

    /**
     * @param index the selector index
     * @return The specification of Maven versions on which this build job should be run.
     * @since 3.0.0
     */
    public String getMavenVersion(int index) {
        return this.properties.getProperty(SELECTOR_PREFIX + index + SelectorProperty.MAVEN_VERSION, getMavenVersion());
    }

    /**
     * Gets the specification of OS families on which this build job should be run.
     *
     * @return The specification of OS families or an empty string if not set.
     */
    public String getOsFamily() {
        return this.properties.getProperty("invoker.os.family", "");
    }

    /**
     * Gets the specification of OS families on which this build job should be run.
     *
     * @param index the selector index
     * @return The specification of OS families or an empty string if not set.
     * @since 3.0.0
     */
    public String getOsFamily(int index) {
        return this.properties.getProperty(SELECTOR_PREFIX + index + SelectorProperty.OS_FAMLY, getOsFamily());
    }

    public Collection<InvokerToolchain> getToolchains() {
        return getToolchains(Pattern.compile("invoker\\.toolchain\\.([^.]+)\\.(.+)"));
    }

    public Collection<InvokerToolchain> getToolchains(int index) {
        return getToolchains(Pattern.compile("selector\\." + index + "\\.invoker\\.toolchain\\.([^.]+)\\.(.+)"));
    }

    private Collection<InvokerToolchain> getToolchains(Pattern p) {
        Map<String, InvokerToolchain> toolchains = new HashMap<>();
        for (Map.Entry<Object, Object> entry : this.properties.entrySet()) {
            Matcher m = p.matcher(entry.getKey().toString());
            if (m.matches()) {
                String type = m.group(1);
                String providesKey = m.group(2);
                String providesValue = entry.getValue().toString();

                InvokerToolchain tc = toolchains.get(type);
                if (tc == null) {
                    tc = new InvokerToolchain(type);
                    toolchains.put(type, tc);
                }
                tc.addProvides(providesKey, providesValue);
            }
        }
        return toolchains.values();
    }

    /**
     * Extract environment variable from properties for given index.
     * Every environment variable without index is also returned.
     *
     * @param index index to lookup
     * @return map of environment name and value
     */
    private Map<String, String> getEnvironmentVariables(int index) {

        Map<String, String> envItems = new HashMap<>();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Matcher matcher =
                    ENVIRONMENT_VARIABLES_PATTERN.matcher(entry.getKey().toString());
            if (matcher.matches()) {

                if (String.valueOf(index).equals(matcher.group(3))) {
                    // variables with index has higher priority, so override
                    envItems.put(matcher.group(1), entry.getValue().toString());
                } else if (matcher.group(3) == null) {
                    // variables without index has lower priority, so check if exist
                    if (!envItems.containsKey(matcher.group(1))) {
                        envItems.put(matcher.group(1), entry.getValue().toString());
                    }
                }
            }
        }
        return envItems;
    }

    /**
     * Determines whether these invoker properties contain a build definition for the specified invocation index.
     *
     * @param index The one-based index of the invocation to check for, must not be negative.
     * @return <code>true</code> if the invocation with the specified index is defined, <code>false</code> otherwise.
     */
    public boolean isInvocationDefined(int index) {
        return Arrays.stream(InvocationProperty.values())
                .map(InvocationProperty::toString)
                .map(v -> properties.getProperty(v + '.' + index))
                .anyMatch(Objects::nonNull);
    }

    /**
     * Determines whether these invoker properties contain a build definition for the specified selector index.
     *
     * @param index the index
     * @return <code>true</code> if the selector with the specified index is defined, <code>false</code> otherwise.
     * @since 3.0.0
     */
    public boolean isSelectorDefined(int index) {
        return Arrays.stream(SelectorProperty.values())
                .map(v -> v.suffix)
                .map(v -> properties.getProperty(SELECTOR_PREFIX + index + v))
                .anyMatch(Objects::nonNull);
    }

    private <T> void setIfNotNull(Consumer<T> consumer, T value) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * Configures the specified invocation request from these invoker properties. Settings not present in the invoker
     * properties will be left unchanged in the invocation request.
     *
     * @param request The invocation request to configure, must not be <code>null</code>.
     * @param index The one-based index of the invocation to configure, must not be negative.
     */
    public void configureInvocation(InvocationRequest request, int index) {
        get(InvocationProperty.PROJECT, index).ifPresent(project -> {
            File file = new File(request.getBaseDirectory(), project);
            if (file.isFile()) {
                request.setBaseDirectory(file.getParentFile());
                request.setPomFile(file);
            } else {
                request.setBaseDirectory(file);
                request.setPomFile(null);
            }
        });

        setIfNotNull(
                request::setGoals,
                get(InvocationProperty.GOALS, index)
                        .map(s -> StringUtils.split(s, ", \t\n\r\f"))
                        .map(Arrays::asList)
                        .filter(l -> !l.isEmpty())
                        .orElse(defaultGoals));

        setIfNotNull(
                request::setProfiles,
                get(InvocationProperty.PROFILES, index)
                        .map(s -> StringUtils.split(s, ", \t\n\r\f"))
                        .map(Arrays::asList)
                        .filter(l -> !l.isEmpty())
                        .orElse(defaultProfiles));

        setIfNotNull(
                request::setMavenExecutable,
                get(InvocationProperty.MAVEN_EXECUTABLE, index).map(File::new).orElse(defaultMavenExecutable));

        setIfNotNull(
                request::setMavenOpts, get(InvocationProperty.MAVEN_OPTS, index).orElse(defaultMavenOpts));

        get(InvocationProperty.FAILURE_BEHAVIOR, index)
                .map(InvocationRequest.ReactorFailureBehavior::valueOfByLongOption)
                .ifPresent(request::setReactorFailureBehavior);

        get(InvocationProperty.NON_RECURSIVE, index)
                .map(Boolean::parseBoolean)
                .map(b -> !b)
                .ifPresent(request::setRecursive);

        get(InvocationProperty.OFFLINE, index).map(Boolean::parseBoolean).ifPresent(request::setOffline);

        setIfNotNull(
                request::setDebug,
                get(InvocationProperty.DEBUG, index).map(Boolean::parseBoolean).orElse(defaultDebug));

        setIfNotNull(
                request::setQuiet,
                get(InvocationProperty.QUIET, index).map(Boolean::parseBoolean).orElse(defaultQuiet));

        setIfNotNull(
                request::setTimeoutInSeconds,
                get(InvocationProperty.TIMEOUT_IN_SECONDS, index)
                        .map(Integer::parseInt)
                        .orElse(defaultTimeoutInSeconds));

        setIfNotNull(
                request::setUpdateSnapshots,
                get(InvocationProperty.UPDATE_SNAPSHOTS, index)
                        .map(Boolean::parseBoolean)
                        .orElse(defaultUpdateSnapshots));

        Optional.ofNullable(defaultEnvironmentVariables).ifPresent(evn -> evn.forEach(request::addShellEnvironment));

        getEnvironmentVariables(index).forEach(request::addShellEnvironment);
    }

    /**
     * Checks whether the specified exit code matches the one expected for the given invocation.
     *
     * @param exitCode The exit code of the Maven invocation to check.
     * @param index The index of the invocation for which to check the exit code, must not be negative.
     * @return <code>true</code> if the exit code is zero and a success was expected or if the exit code is non-zero and
     *         a failue was expected, <code>false</code> otherwise.
     */
    public boolean isExpectedResult(int exitCode, int index) {
        boolean nonZeroExit = "failure"
                .equalsIgnoreCase(get(InvocationProperty.BUILD_RESULT, index).orElse(null));
        return (exitCode != 0) == nonZeroExit;
    }

    /**
     * Gets the path to the properties file used to set the system properties for the specified invocation.
     *
     * @param index The index of the invocation, must not be negative.
     * @return The path to the properties file or <code>null</code> if not set.
     */
    public String getSystemPropertiesFile(int index) {
        return get(InvocationProperty.SYSTEM_PROPERTIES_FILE, index).orElse(null);
    }

    /**
     * Gets the settings file used for the specified invocation.
     *
     * @param index The index of the invocation, must not be negative.
     * @return the value for the settings file or <code>null</code> if not set.
     */
    public String getSettingsFile(int index) {
        return get(InvocationProperty.SETTINGS_FILE, index).orElse(null);
    }

    /**
     * Gets a value from the invoker properties. The invoker properties are intended to describe the invocation settings
     * for multiple builds of the same project. For this reason, the properties are indexed. First, a property named
     * <code>key.index</code> will be queried. If this property does not exist, the value of the property named
     * <code>key</code> will finally be returned.
     *
     * @param key The (base) key for the invoker property to lookup, must not be <code>null</code>.
     * @param index The index of the invocation for which to retrieve the value, must not be negative.
     * @return The value for the requested invoker property or <code>null</code> if not defined.
     */
    Optional<String> get(String key, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("invalid invocation index: " + index);
        }

        // lookup in properties
        String value = Optional.ofNullable(properties.getProperty(key + '.' + index))
                .orElseGet(() -> properties.getProperty(key));

        return Optional.ofNullable(value).map(String::trim).filter(s -> !s.isEmpty());
    }

    private Optional<String> get(InvocationProperty prop, int index) {
        return get(prop.toString(), index);
    }
}
