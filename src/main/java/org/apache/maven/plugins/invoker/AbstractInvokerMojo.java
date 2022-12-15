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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.TrackableBase;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.scriptinterpreter.ScriptException;
import org.apache.maven.shared.scriptinterpreter.ScriptReturnException;
import org.apache.maven.shared.scriptinterpreter.ScriptRunner;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Provides common code for mojos invoking sub builds.
 *
 * @author Stephen Connolly
 * @since 15-Aug-2009 09:09:29
 */
public abstract class AbstractInvokerMojo extends AbstractMojo {
    /**
     * The zero-based column index where to print the invoker result.
     */
    private static final int RESULT_COLUMN = 60;

    /**
     * Flag used to suppress certain invocations. This is useful in tailoring the build using profiles.
     *
     * @since 1.1
     */
    @Parameter(property = "invoker.skip", defaultValue = "false")
    private boolean skipInvocation;

    /**
     * Flag used to suppress the summary output notifying of successes and failures. If set to <code>true</code>, the
     * only indication of the build's success or failure will be the effect it has on the main build (if it fails, the
     * main build should fail as well). If {@link #streamLogs} is enabled, the sub-build summary will also provide an
     * indication.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "false")
    protected boolean suppressSummaries;

    /**
     * Flag used to determine whether the build logs should be output to the normal mojo log.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.streamLogs", defaultValue = "false")
    private boolean streamLogs;

    /**
     * The local repository for caching artifacts. It is strongly recommended to specify a path to an isolated
     * repository like <code>${project.build.directory}/it-repo</code>. Otherwise, your ordinary local repository will
     * be used, potentially soiling it with broken artifacts.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.localRepositoryPath", defaultValue = "${settings.localRepository}")
    private File localRepositoryPath;

    /**
     * Directory to search for integration tests.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.projectsDirectory", defaultValue = "${basedir}/src/it/")
    private File projectsDirectory;

    /**
     * Base directory where all build reports are written to. Every execution of an integration test will produce an XML
     * file which contains the information about success or failure of that particular build job. The format of the
     * resulting XML file is documented in the given <a href="./build-job.html">build-job</a> reference.
     *
     * @since 1.4
     */
    @Parameter(property = "invoker.reportsDirectory", defaultValue = "${project.build.directory}/invoker-reports")
    private File reportsDirectory;

    /**
     * A flag to disable the generation of build reports.
     *
     * @since 1.4
     */
    @Parameter(property = "invoker.disableReports", defaultValue = "false")
    private boolean disableReports;

    /**
     * Directory to which projects should be cloned prior to execution. If set to {@code null}, each integration test
     * will be run in the directory in which the corresponding IT POM was found. In this case, you most likely want to
     * configure your SCM to ignore <code>target</code> and <code>build.log</code> in the test's base directory.
     * (<b>Exception</b> when project using invoker plugin is of <i>maven-plugin</i> packaging:
     * In such case IT projects will be cloned to and executed in <code>target/its</code> by default.)
     *
     * @since 1.1
     */
    @Parameter(property = "invoker.cloneProjectsTo")
    private File cloneProjectsTo;

    // CHECKSTYLE_OFF: LineLength
    /**
     * Some files are normally excluded when copying the IT projects from the directory specified by the parameter
     * projectsDirectory to the directory given by cloneProjectsTo (e.g. <code>.svn</code>, <code>CVS</code>,
     * <code>*~</code>, etc: see <a href=
     * "https://codehaus-plexus.github.io/plexus-utils/apidocs/org/codehaus/plexus/util/AbstractScanner.html#DEFAULTEXCLUDES">
     * reference</a> for full list). Setting this parameter to <code>true</code> will cause all files to be copied to
     * the <code>cloneProjectsTo</code> directory.
     *
     * @since 1.2
     */
    @Parameter(defaultValue = "false")
    private boolean cloneAllFiles;
    // CHECKSTYLE_ON: LineLength

    /**
     * Ensure the {@link #cloneProjectsTo} directory is not polluted with files from earlier invoker runs.
     *
     * @since 1.6
     */
    @Parameter(defaultValue = "true")
    private boolean cloneClean;

    /**
     * A single POM to build, skipping any scanning parameters and behavior.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.pom")
    private File pom;

    /**
     * Include patterns for searching the integration test directory for projects. This parameter is meant to be set
     * from the POM. If this parameter is not set, the plugin will search for all <code>pom.xml</code> files one
     * directory below {@link #projectsDirectory} (i.e. <code>*&#47;pom.xml</code>).<br>
     * <br>
     * Starting with version 1.3, mere directories can also be matched by these patterns. For example, the include
     * pattern <code>*</code> will run Maven builds on all immediate sub directories of {@link #projectsDirectory},
     * regardless if they contain a <code>pom.xml</code>. This allows to perform builds that need/should not depend on
     * the existence of a POM.
     *
     * @since 1.0
     */
    @Parameter
    private List<String> pomIncludes = Collections.singletonList("*/pom.xml");

    /**
     * Exclude patterns for searching the integration test directory. This parameter is meant to be set from the POM. By
     * default, no POM files are excluded. For the convenience of using an include pattern like <code>*</code>, the
     * custom settings file specified by the parameter {@link #settingsFile} will always be excluded automatically.
     *
     * @since 1.0
     */
    @Parameter
    private List<String> pomExcludes = Collections.emptyList();

    /**
     * Include patterns for searching the projects directory for projects that need to be run before the other projects.
     * This parameter allows to declare projects that perform setup tasks like installing utility artifacts into the
     * local repository. Projects matched by these patterns are implicitly excluded from the scan for ordinary projects.
     * Also, the exclusions defined by the parameter {@link #pomExcludes} apply to the setup projects, too. Default
     * value is: <code>setup*&#47;pom.xml</code>.
     *
     * @since 1.3
     */
    @Parameter
    private List<String> setupIncludes = Collections.singletonList("setup*/pom.xml");

    /**
     * The list of goals to execute on each project. Default value is: <code>package</code>.
     *
     * @since 1.0
     */
    @Parameter
    private List<String> goals = Collections.singletonList("package");

    /**
     * Relative path of a selector script to run prior in order to decide if the build should be executed. This script
     * may be written with either BeanShell or Groovy. If the file extension is omitted (e.g. <code>selector</code>),
     * the plugin searches for the file by trying out the well-known extensions <code>.bsh</code> and
     * <code>.groovy</code>. If this script exists for a particular project but returns any non-null value different
     * from <code>true</code>, the corresponding build is flagged as skipped. In this case, none of the pre-build hook
     * script, Maven nor the post-build hook script will be invoked. If this script throws an exception, the
     * corresponding build is flagged as in error, and none of the pre-build hook script, Maven not the post-build hook
     * script will be invoked.
     *
     * @since 1.5
     */
    @Parameter(property = "invoker.selectorScript", defaultValue = "selector")
    private String selectorScript;

    /**
     * Relative path of a pre-build hook script to run prior to executing the build. This script may be written with
     * either BeanShell or Groovy (since 1.3). If the file extension is omitted (e.g. <code>prebuild</code>), the plugin
     * searches for the file by trying out the well-known extensions <code>.bsh</code> and <code>.groovy</code>. If this
     * script exists for a particular project but returns any non-null value different from <code>true</code> or throws
     * an exception, the corresponding build is flagged as a failure. In this case, neither Maven nor the post-build
     * hook script will be invoked.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.preBuildHookScript", defaultValue = "prebuild")
    private String preBuildHookScript;

    /**
     * Relative path of a cleanup/verification hook script to run after executing the build. This script may be written
     * with either BeanShell or Groovy (since 1.3). If the file extension is omitted (e.g. <code>verify</code>), the
     * plugin searches for the file by trying out the well-known extensions <code>.bsh</code> and <code>.groovy</code>.
     * If this script exists for a particular project but returns any non-null value different from <code>true</code> or
     * throws an exception, the corresponding build is flagged as a failure.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.postBuildHookScript", defaultValue = "postbuild")
    private String postBuildHookScript;

    /**
     * Location of a properties file that defines CLI properties for the test.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.testPropertiesFile", defaultValue = "test.properties")
    private String testPropertiesFile;

    /**
     * Common set of properties to pass in on each project's command line, via -D parameters.
     *
     * @since 1.1
     */
    @Parameter
    private Map<String, String> properties;

    /**
     * Whether to show errors in the build output.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.showErrors", defaultValue = "false")
    private boolean showErrors;

    /**
     * Whether to show debug statements in the build output.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.debug", defaultValue = "false")
    private boolean debug;

    /**
     * Whether to execute Maven in quiet mode.
     *
     * @since 3.3.0
     */
    @Parameter(property = "invoker.quiet", defaultValue = "false")
    private boolean quiet;

    /**
     * Suppress logging to the <code>build.log</code> file.
     *
     * @since 1.0
     */
    @Parameter(property = "invoker.noLog", defaultValue = "false")
    private boolean noLog;

    /**
     * By default a {@code build.log} is created in the root of the project. By setting this folder
     * files are written to a different folder, respecting the structure of the projectsDirectory.
     *
     * @since 3.2.0
     */
    @Parameter
    private File logDirectory;

    /**
     * List of profile identifiers to explicitly trigger in the build.
     *
     * @since 1.1
     */
    @Parameter
    private List<String> profiles;

    /**
     * A list of additional properties which will be used to filter tokens in POMs and goal files.
     *
     * @since 1.3
     */
    @Parameter
    private Map<String, String> filterProperties;

    /**
     * A comma separated list of projectname patterns to run. Specify this parameter to run individual tests by file
     * name, overriding the {@link #setupIncludes}, {@link #pomIncludes} and {@link #pomExcludes} parameters. Each
     * pattern you specify here will be used to create an include/exclude pattern formatted like
     * <code>${projectsDirectory}/<i>pattern</i></code>. To exclude a test, prefix the pattern with a '<code>!</code>'.
     * So you can just type <nobr><code>-Dinvoker.test=SimpleTest,Comp*Test,!Compare*</code></nobr> to run builds in
     * <code>${projectsDirectory}/SimpleTest</code> and <code>${projectsDirectory}/ComplexTest</code>, but not
     * <code>${projectsDirectory}/CompareTest</code>
     *
     * @since 1.1 (exclusion since 1.8)
     */
    @Parameter(property = "invoker.test")
    private String invokerTest;

    /**
     * Path to an alternate <code>settings.xml</code> to use for Maven invocation with all ITs. Note that the
     * <code>&lt;localRepository&gt;</code> element of this settings file is always ignored, i.e. the path given by the
     * parameter {@link #localRepositoryPath} is dominant.
     *
     * @since 1.2
     */
    @Parameter(property = "invoker.settingsFile")
    private File settingsFile;

    /**
     * The <code>MAVEN_OPTS</code> environment variable to use when invoking Maven. This value can be overridden for
     * individual integration tests by using {@link #invokerPropertiesFile}.
     *
     * @since 1.2
     */
    @Parameter(property = "invoker.mavenOpts")
    private String mavenOpts;

    /**
     * The home directory of the Maven installation to use for the forked builds. Defaults to the current Maven
     * installation.
     *
     * @since 1.3
     */
    @Parameter(property = "invoker.mavenHome")
    private File mavenHome;

    /**
     * mavenExecutable can either be a file relative to <code>${maven.home}/bin/</code>, test project workspace
     * or an absolute file.
     *
     * @since 1.8
     */
    @Parameter(property = "invoker.mavenExecutable")
    private File mavenExecutable;

    /**
     * The <code>JAVA_HOME</code> environment variable to use for forked Maven invocations. Defaults to the current Java
     * home directory.
     *
     * @since 1.3
     */
    @Parameter(property = "invoker.javaHome")
    private File javaHome;

    /**
     * The file encoding for the pre-/post-build scripts and the list files for goals and profiles.
     *
     * @since 1.2
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * A flag whether the test class path of the project under test should be included in the class path of the
     * pre-/post-build scripts. If set to <code>false</code>, the class path of script interpreter consists only of the
     * <a href="dependencies.html">runtime dependencies</a> of the Maven Invoker Plugin. If set the <code>true</code>,
     * the project's test class path will be prepended to the interpreter class path. Among others, this feature allows
     * the scripts to access utility classes from the test sources of your project.
     *
     * @since 1.2
     */
    @Parameter(property = "invoker.addTestClassPath", defaultValue = "false")
    private boolean addTestClassPath;

    /**
     * The name of an optional project-specific file that contains properties used to specify settings for an individual
     * Maven invocation. Any property present in the file will override the corresponding setting from the plugin
     * configuration. The values of the properties are filtered and may use expressions like
     * <code>${project.version}</code> to reference project properties or values from the parameter
     * {@link #filterProperties}.<p/>
     *
     * <p>
     * As of 3.2.0 it is possible to put this folder in any of the ancestor folders, where properties will be inherited.
     * This way you can provide a single properties file for a group of projects
     * </p>
     *
     * The snippet below describes the supported properties:
     * <pre>
     * # A comma or space separated list of goals/phases to execute, may
     * # specify an empty list to execute the default goal of the IT project.
     * # Environment variables used by maven plugins can be added here
     * invoker.goals = clean install -Dplugin.variable=value
     *
     * # Or you can give things like this if you need.
     * invoker.goals = -T2 clean verify
     *
     * # Optionally, a list of goals to run during further invocations of Maven
     * invoker.goals.2 = ${project.groupId}:${project.artifactId}:${project.version}:run
     *
     * # A comma or space separated list of profiles to activate
     * # can be indexed
     * invoker.profiles = its,jdk15
     *
     * # The path to an alternative POM or base directory to invoke Maven on, defaults to the
     * # project that was originally specified in the plugin configuration
     * # Since plugin version 1.4
     * # can be indexed
     * invoker.project = sub-module
     *
     * # The maven executable can either be a file relative to <code>${maven.home}/bin/</code>, test project workspace
     * # or an absolute file.
     * # Since plugin version 3.3.0
     * # can be indexed
     * invoker.mavenExecutable = mvnw
     *
     * # The value for the environment variable MAVEN_OPTS
     * # can be indexed
     * invoker.mavenOpts = -Dfile.encoding=UTF-16 -Xms32m -Xmx256m
     *
     * # Possible values are &quot;fail-fast&quot; (default), &quot;fail-at-end&quot; and &quot;fail-never&quot;
     * # can be indexed
     * invoker.failureBehavior = fail-never
     *
     * # The expected result of the build, possible values are &quot;success&quot; (default) and &quot;failure&quot;
     * # can be indexed
     * invoker.buildResult = failure
     *
     * # A boolean value controlling the aggregator mode of Maven, defaults to &quot;false&quot;
     *
     * # can be indexed
     * invoker.nonRecursive = true
     *
     * # A boolean value controlling the network behavior of Maven, defaults to &quot;false&quot;
     * # Since plugin version 1.4
     * # can be indexed
     * invoker.offline = true
     *
     * # The path to the properties file from which to load system properties, defaults to the
     * # filename given by the plugin parameter testPropertiesFile
     * # Since plugin version 1.4
     * # can be indexed
     * invoker.systemPropertiesFile = test.properties
     *
     * # An optional human friendly name and description for this build job.
     * # Both name and description have to be set to be included in the build reports.
     * # Since plugin version 1.4
     * invoker.name = Test Build 01
     * invoker.description = Checks the support for build reports.
     *
     * # A comma separated list of JRE versions on which this build job should be run.
     * # Since plugin version 1.4
     * invoker.java.version = 1.4+, !1.4.1, 1.7-
     *
     * # A comma separated list of OS families on which this build job should be run.
     * # Since plugin version 1.4
     * invoker.os.family = !windows, unix, mac
     *
     * # A comma separated list of Maven versions on which this build should be run.
     * # Since plugin version 1.5
     * invoker.maven.version = 2.0.10+, !2.1.0, !2.2.0
     *
     * # A mapping for toolchain to ensure it exists
     * # Since plugin version 3.2.0
     * invoker.toolchain.&lt;type&gt;.&lt;provides&gt; = value
     * invoker.toolchain.jdk.version = 11
     *
     * # For java.version, maven.version, os.family and toolchain it is possible to define multiple selectors.
     * # If one of the indexed selectors matches, the test is executed.
     * # With the invoker.x.y equivalents you can specify global matchers.
     * selector.1.java.version = 1.8+
     * selector.1.maven.version = 3.2.5+
     * selector.1.os.family = !windows
     * selector.2.maven.version = 3.0+
     * selector.3.java.version = 9+
     *
     * # A boolean value controlling the debug logging level of Maven, , defaults to &quot;false&quot;
     * # Since plugin version 1.8
     * # can be indexed
     * invoker.debug = true
     *
     * # Whether to execute Maven in quiet mode
     * # Since plugin version 3.3.0
     * # can be indexed
     * invoker.quiet = true
     *
     * The execution timeout in seconds.
     * # Since plugin version 3.0.2
     * # can be indexed
     * invoker.timeoutInSeconds = 5
     *
     * # Path to an alternate settings.xml to use for Maven invocation with this IT.
     * # Since plugin version 3.0.1
     * # can be indexed
     * invoker.settingsFile = ../
     *
     * # An integer value to control run order of projects. sorted in the descending order of the ordinal.
     * # In other words, the BuildJobs with the highest numbers will be executed first
     * # Default value is 0 (zero)
     * # Since plugin version 3.2.1
     * invoker.ordinal = 3
     *
     * # The additional value for the environment variable.
     * # Since plugin version 3.2.2
     * invoker.environmentVariables.&lt;variableName&gt; = variableValue
     * invoker.environmentVariables.MY_ENV_NAME = myEnvValue
     *
     * # A boolean value indicating a check for missing releases and updated snapshots on remote repositories to be done
     * # Passed to the invoker. Same as passing -U, --update-snapshots flag on the command line
     * # Since plugin version 3.4.0
     * invoker.updateSnapshots = true
     *
     * </pre>
     *
     * @since 1.2
     */
    @Parameter(property = "invoker.invokerPropertiesFile", defaultValue = "invoker.properties")
    private String invokerPropertiesFile;

    /**
     * flag to enable show mvn version used for running its (cli option : -V,--show-version )
     *
     * @since 1.4
     */
    @Parameter(property = "invoker.showVersion", defaultValue = "false")
    private boolean showVersion;

    /**
     * <p>Number of threads for running tests in parallel. This will be the number of maven forked process in parallel.
     * When terminated with "C", the number part is multiplied by the number of processors (cores) available
     * to the Java virtual machine. Floating point value are only accepted together with "C".</p>
     *
     * <p>Example values: "1.5C", "4"</p>
     *
     * @since 1.6
     */
    @Parameter(property = "invoker.parallelThreads", defaultValue = "1")
    private String parallelThreads;

    /**
     * If enable and if you have a settings file configured for the execution, it will be merged with your user
     * settings.
     *
     * @since 1.6
     */
    @Parameter(property = "invoker.mergeUserSettings", defaultValue = "false")
    private boolean mergeUserSettings;

    /**
     * Additional environment variables to set on the command line.
     *
     * @since 1.8
     */
    @Parameter
    private Map<String, String> environmentVariables;

    /**
     * Additional variables for use in the hook scripts.
     *
     * @since 1.9
     */
    @Parameter
    private Map<String, String> scriptVariables;

    /**
     *
     * @since 3.0.2
     */
    @Parameter(defaultValue = "0", property = "invoker.timeoutInSeconds")
    private int timeoutInSeconds;

    /**
     * Write test result in junit format.
     * @since 3.1.2
     */
    @Parameter(defaultValue = "false", property = "invoker.writeJunitReport")
    private boolean writeJunitReport;

    /**
     * The package name use in junit report
     * @since 3.1.2
     */
    @Parameter(defaultValue = "maven.invoker.it", property = "invoker.junitPackageName")
    private String junitPackageName = "maven.invoker.it";

    /**
     * Only invoke maven projects if their sources have been modified since
     * they were last built. Only works in conjunction with <code>cloneProjectsTo</code>.
     *
     * @since 3.2.2
     */
    @Parameter(defaultValue = "false", property = "invoker.updateOnly")
    private boolean updateOnly = false;

    /**
     * Force a check for missing releases and updated snapshots on remote repositories. This is passed to the invoked
     * maven projects (it is the same as if you were to use the -U, --update-snapshots flag on the command line).
     *
     * @since 3.4.0
     */
    @Parameter(defaultValue = "false", property = "invoker.updateSnapshots")
    private boolean updateSnapshots;

    // internal state variables

    /**
     * The scripter runner that is responsible to execute hook scripts.
     */
    private ScriptRunner scriptRunner;

    /**
     * A string used to prefix the file name of the filtered POMs in case the POMs couldn't be filtered in-place (i.e.
     * the projects were not cloned to a temporary directory), can be <code>null</code>. This will be set to
     * <code>null</code> if the POMs have already been filtered during cloning.
     */
    private String filteredPomPrefix = "interpolated-";

    /**
     * The format for elapsed build time.
     */
    private final DecimalFormat secFormat = new DecimalFormat("(0.0 s)", new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * The version of Maven which is used to run the builds
     */
    private String actualMavenVersion;

    // used components - readonly parameters

    @Parameter(property = "plugin.artifacts", required = true, readonly = true)
    private List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true)
    private List<String> testClassPath;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    private MojoExecution mojoExecution;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    @Component
    private Invoker invoker;

    @Component
    private SettingsBuilder settingsBuilder;

    @Component
    private ToolchainManagerPrivate toolchainManagerPrivate;

    /**
     * Invokes Maven on the configured test projects.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If the goal encountered severe errors.
     * @throws org.apache.maven.plugin.MojoFailureException If any of the Maven builds failed.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipInvocation) {
            getLog().info("Skipping invocation per configuration."
                    + " If this is incorrect, ensure the skipInvocation parameter is not set to true.");
            return;
        }

        if (StringUtils.isEmpty(encoding)) {
            getLog().warn("File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                    + ", i.e. build is platform dependent!");
        }

        // done it here to prevent issues with concurrent access in case of parallel run
        if (!disableReports) {
            setupReportsFolder();
        }

        List<BuildJob> buildJobs;
        if (pom == null) {
            try {
                buildJobs = getBuildJobs();
            } catch (final IOException e) {
                throw new MojoExecutionException(
                        "Error retrieving POM list from includes, " + "excludes, and projects directory. Reason: "
                                + e.getMessage(),
                        e);
            }
        } else {
            try {
                projectsDirectory = pom.getCanonicalFile().getParentFile();
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to discover projectsDirectory from " + "pom File parameter. Reason: " + e.getMessage(),
                        e);
            }

            buildJobs = Collections.singletonList(new BuildJob(pom.getName()));
        }

        if (buildJobs.isEmpty()) {
            doFailIfNoProjects();

            getLog().info("No projects were selected for execution.");
            return;
        }

        setupActualMavenVersion();

        handleScriptRunnerWithScriptClassPath();

        Collection<String> collectedProjects = new LinkedHashSet<>();
        for (BuildJob buildJob : buildJobs) {
            collectProjects(projectsDirectory, buildJob.getProject(), collectedProjects, true);
        }

        File projectsDir = projectsDirectory;

        if (cloneProjectsTo == null && "maven-plugin".equals(project.getPackaging())) {
            cloneProjectsTo = new File(project.getBuild().getDirectory(), "its");
        }

        if (updateOnly) {
            if (cloneProjectsTo == null) {
                getLog().warn("updateOnly functionality is not supported without cloning the projects");
            } else if (lastModifiedRecursive(projectsDirectory) <= lastModifiedRecursive(cloneProjectsTo)) {
                getLog().debug("Skipping invocation as cloned projects are up-to-date "
                        + "and updateOnly parameter is set to true.");
                return;
            } else {
                getLog().debug("Cloned projects are out of date");
            }
        }

        if (cloneProjectsTo != null) {
            cloneProjects(collectedProjects);
            projectsDir = cloneProjectsTo;
        } else {
            getLog().warn("Filtering of parent/child POMs is not supported without cloning the projects");
        }

        // First run setup jobs.
        List<BuildJob> setupBuildJobs = getSetupJobs(buildJobs);

        if (!setupBuildJobs.isEmpty()) {
            // Run setup jobs in single thread mode.
            //
            // Jobs are ordered according to ordinal value from invoker.properties
            getLog().info("Running " + setupBuildJobs.size() + " setup job" + ((setupBuildJobs.size() < 2) ? "" : "s")
                    + ":");
            runBuilds(projectsDir, setupBuildJobs, 1);
            getLog().info("Setup done.");
        }

        List<BuildJob> nonSetupBuildJobs = getNonSetupJobs(buildJobs);

        if (setupBuildJobs.isEmpty() || setupBuildJobs.stream().allMatch(BuildJob::isNotError)) {
            // We will run the non setup jobs with the configured
            // parallelThreads number.
            runBuilds(projectsDir, nonSetupBuildJobs, getParallelThreadsCount());
        } else {
            for (BuildJob buildJob : nonSetupBuildJobs) {
                buildJob.setResult(BuildJob.Result.SKIPPED);
                buildJob.setFailureMessage("Skipped due to setup job(s) failure");
                writeBuildReport(buildJob);
            }
        }

        writeSummaryFile(buildJobs);
        processResults(new InvokerSession(buildJobs));
    }

    private void setupActualMavenVersion() throws MojoExecutionException {
        if (mavenHome != null) {
            try {
                actualMavenVersion = SelectorUtils.getMavenVersion(mavenHome);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            actualMavenVersion = SelectorUtils.getMavenVersion();
        }
    }

    /**
     * Find the latest lastModified recursively within a directory structure.
     *
     * @param file the root file to check.
     * @return the latest lastModified time found.
     */
    private long lastModifiedRecursive(File file) {
        long lastModified = file.lastModified();

        final File[] entries = file.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                lastModified = Math.max(lastModified, lastModifiedRecursive(entry));
            }
        }

        return lastModified;
    }

    /**
     * This will create the necessary folders for the reports.
     *
     * @throws MojoExecutionException in case of failure during creation of the reports folder.
     */
    private void setupReportsFolder() throws MojoExecutionException {
        // If it exists from previous run...
        if (reportsDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(reportsDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failure while trying to delete " + reportsDirectory.getAbsolutePath(), e);
            }
        }
        if (!reportsDirectory.mkdirs()) {
            throw new MojoExecutionException("Failure while creating the " + reportsDirectory.getAbsolutePath());
        }
    }

    private List<BuildJob> getSetupJobs(List<BuildJob> buildJobs) {
        return buildJobs.stream()
                .filter(buildJob -> buildJob.getType().equals(BuildJob.Type.SETUP))
                .collect(Collectors.toList());
    }

    private List<BuildJob> getNonSetupJobs(List<BuildJob> buildJobs) {
        return buildJobs.stream()
                .filter(buildJob -> !buildJob.getType().equals(BuildJob.Type.SETUP))
                .collect(Collectors.toList());
    }

    private void handleScriptRunnerWithScriptClassPath() {
        final List<String> scriptClassPath;
        if (addTestClassPath) {
            scriptClassPath = new ArrayList<>(testClassPath);
            for (Artifact pluginArtifact : pluginArtifacts) {
                scriptClassPath.remove(pluginArtifact.getFile().getAbsolutePath());
            }
        } else {
            scriptClassPath = null;
        }
        scriptRunner = new ScriptRunner();
        scriptRunner.setScriptEncoding(encoding);
        scriptRunner.setGlobalVariable("localRepositoryPath", localRepositoryPath);
        scriptRunner.setGlobalVariable("mavenVersion", actualMavenVersion);
        if (scriptVariables != null) {
            scriptVariables.forEach((key, value) -> scriptRunner.setGlobalVariable(key, value));
        }
        scriptRunner.setClassPath(scriptClassPath);
    }

    private void writeSummaryFile(List<BuildJob> buildJobs) throws MojoExecutionException {

        File summaryReportFile = new File(reportsDirectory, "invoker-summary.txt");

        try (Writer writer = new BufferedWriter(new FileWriter(summaryReportFile))) {
            for (BuildJob buildJob : buildJobs) {
                if (!BuildJob.Result.SUCCESS.equals(buildJob.getResult())) {
                    writer.append(buildJob.getResult());
                    writer.append(" [");
                    writer.append(buildJob.getProject());
                    writer.append("] ");
                    if (buildJob.getFailureMessage() != null) {
                        writer.append(" ");
                        writer.append(buildJob.getFailureMessage());
                    }
                    writer.append("\n");
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write summary report " + summaryReportFile, e);
        }
    }

    protected void doFailIfNoProjects() throws MojoFailureException {
        // should only be used during run and verify
    }

    /**
     * Processes the results of invoking the build jobs.
     *
     * @param invokerSession The session with the build jobs, must not be <code>null</code>.
     * @throws MojoFailureException If the mojo had failed as a result of invoking the build jobs.
     * @since 1.4
     */
    abstract void processResults(InvokerSession invokerSession) throws MojoFailureException;

    /**
     * Collects all projects locally reachable from the specified project. The method will as such try to read the POM
     * and recursively follow its parent/module elements.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param projectPath The relative path of the current project, can denote either the POM or its base directory,
     *            must not be <code>null</code>.
     * @param projectPaths The set of already collected projects to add new projects to, must not be <code>null</code>.
     *            This set will hold the relative paths to either a POM file or a project base directory.
     * @param included A flag indicating whether the specified project has been explicitly included via the parameter
     *            {@link #pomIncludes}. Such projects will always be added to the result set even if there is no
     *            corresponding POM.
     * @throws org.apache.maven.plugin.MojoExecutionException If the project tree could not be traversed.
     */
    private void collectProjects(
            File projectsDir, String projectPath, Collection<String> projectPaths, boolean included)
            throws MojoExecutionException {
        projectPath = projectPath.replace('\\', '/');
        File pomFile = new File(projectsDir, projectPath);
        if (pomFile.isDirectory()) {
            pomFile = new File(pomFile, "pom.xml");
            if (!pomFile.exists()) {
                if (included) {
                    projectPaths.add(projectPath);
                }
                return;
            }
            if (!projectPath.endsWith("/")) {
                projectPath += '/';
            }
            projectPath += "pom.xml";
        } else if (!pomFile.isFile()) {
            return;
        }
        if (!projectPaths.add(projectPath)) {
            return;
        }
        getLog().debug("Collecting parent/child projects of " + projectPath);

        Model model = PomUtils.loadPom(pomFile);

        try {
            String projectsRoot = projectsDir.getCanonicalPath();
            String projectDir = pomFile.getParent();

            String parentPath = "../pom.xml";
            if (model.getParent() != null
                    && StringUtils.isNotEmpty(model.getParent().getRelativePath())) {
                parentPath = model.getParent().getRelativePath();
            }
            String parent = relativizePath(new File(projectDir, parentPath), projectsRoot);
            if (parent != null) {
                collectProjects(projectsDir, parent, projectPaths, false);
            }

            Collection<String> modulePaths = new LinkedHashSet<>(model.getModules());

            model.getProfiles().forEach(profile -> modulePaths.addAll(profile.getModules()));

            for (String modulePath : modulePaths) {
                String module = relativizePath(new File(projectDir, modulePath), projectsRoot);
                if (module != null) {
                    collectProjects(projectsDir, module, projectPaths, false);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to analyze POM: " + pomFile, e);
        }
    }

    /**
     * Copies the specified projects to the directory given by {@link #cloneProjectsTo}. A project may either be denoted
     * by a path to a POM file or merely by a path to a base directory. During cloning, the POM files will be filtered.
     *
     * @param projectPaths The paths to the projects to clone, relative to the projects directory, must not be
     *            <code>null</code> nor contain <code>null</code> elements.
     * @throws org.apache.maven.plugin.MojoExecutionException If the the projects could not be copied/filtered.
     */
    private void cloneProjects(Collection<String> projectPaths) throws MojoExecutionException {
        if (!cloneProjectsTo.mkdirs() && cloneClean) {
            try {
                FileUtils.cleanDirectory(cloneProjectsTo);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Could not clean the cloneProjectsTo directory. Reason: " + e.getMessage(), e);
            }
        }

        // determine project directories to clone
        Collection<String> dirs = new LinkedHashSet<>();
        for (String projectPath : projectPaths) {
            if (!new File(projectsDirectory, projectPath).isDirectory()) {
                projectPath = getParentPath(projectPath);
            }
            dirs.add(projectPath);
        }

        boolean filter;

        // clone project directories
        try {
            filter = !cloneProjectsTo.getCanonicalFile().equals(projectsDirectory.getCanonicalFile());

            List<String> clonedSubpaths = new ArrayList<>();

            for (String subpath : dirs) {
                // skip this project if its parent directory is also scheduled for cloning
                if (!".".equals(subpath) && dirs.contains(getParentPath(subpath))) {
                    continue;
                }

                // avoid copying subdirs that are already cloned.
                if (!alreadyCloned(subpath, clonedSubpaths)) {
                    // avoid creating new files that point to dir/.
                    if (".".equals(subpath)) {
                        String cloneSubdir = relativizePath(cloneProjectsTo, projectsDirectory.getCanonicalPath());

                        // avoid infinite recursion if the cloneTo path is a subdirectory.
                        if (cloneSubdir != null) {
                            File temp = Files.createTempDirectory("pre-invocation-clone.")
                                    .toFile();

                            copyDirectoryStructure(projectsDirectory, temp);

                            FileUtils.deleteDirectory(new File(temp, cloneSubdir));

                            copyDirectoryStructure(temp, cloneProjectsTo);
                        } else {
                            copyDirectoryStructure(projectsDirectory, cloneProjectsTo);
                        }
                    } else {
                        File srcDir = new File(projectsDirectory, subpath);
                        File dstDir = new File(cloneProjectsTo, subpath);
                        copyDirectoryStructure(srcDir, dstDir);
                    }

                    clonedSubpaths.add(subpath);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to clone projects from: " + projectsDirectory + " to: " + cloneProjectsTo + ". Reason: "
                            + e.getMessage(),
                    e);
        }

        // filter cloned POMs
        if (filter) {
            for (String projectPath : projectPaths) {
                File pomFile = new File(cloneProjectsTo, projectPath);
                if (pomFile.isFile()) {
                    buildInterpolatedFile(pomFile, pomFile);
                }

                // MINVOKER-186
                // The following is a temporary solution to support Maven 3.3.1 (.mvn/extensions.xml) filtering
                // Will be replaced by MINVOKER-117 with general filtering mechanism
                File baseDir = pomFile.getParentFile();
                File mvnDir = new File(baseDir, ".mvn");
                if (mvnDir.isDirectory()) {
                    File extensionsFile = new File(mvnDir, "extensions.xml");
                    if (extensionsFile.isFile()) {
                        buildInterpolatedFile(extensionsFile, extensionsFile);
                    }
                }
                // END MINVOKER-186
            }
            filteredPomPrefix = null;
        }
    }

    /**
     * Gets the parent path of the specified relative path.
     *
     * @param path The relative path whose parent should be retrieved, must not be <code>null</code>.
     * @return The parent path or "." if the specified path has no parent, never <code>null</code>.
     */
    private String getParentPath(String path) {
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (lastSep < 0) ? "." : path.substring(0, lastSep);
    }

    /**
     * Copied a directory structure with default exclusions (.svn, CVS, etc)
     *
     * @param sourceDir The source directory to copy, must not be <code>null</code>.
     * @param destDir The target directory to copy to, must not be <code>null</code>.
     * @throws java.io.IOException If the directory structure could not be copied.
     */
    private void copyDirectoryStructure(File sourceDir, File destDir) throws IOException {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDir);
        if (!cloneAllFiles) {
            scanner.addDefaultExcludes();
        }
        scanner.scan();

        /*
         * NOTE: Make sure the destination directory is always there (even if empty) to support POM-less ITs.
         */
        destDir.mkdirs();
        // Create all the directories, including any symlinks present in source
        FileUtils.mkDirs(sourceDir, scanner.getIncludedDirectories(), destDir);

        for (String includedFile : scanner.getIncludedFiles()) {
            File sourceFile = new File(sourceDir, includedFile);
            File destFile = new File(destDir, includedFile);
            FileUtils.copyFile(sourceFile, destFile);

            // ensure clone project must be writable for additional changes
            destFile.setWritable(true);
        }
    }

    /**
     * Determines whether the specified sub path has already been cloned, i.e. whether one of its ancestor directories
     * was already cloned.
     *
     * @param subpath The sub path to check, must not be <code>null</code>.
     * @param clonedSubpaths The list of already cloned paths, must not be <code>null</code> nor contain
     *            <code>null</code> elements.
     * @return <code>true</code> if the specified path has already been cloned, <code>false</code> otherwise.
     */
    static boolean alreadyCloned(String subpath, List<String> clonedSubpaths) {
        for (String path : clonedSubpaths) {
            if (".".equals(path) || subpath.equals(path) || subpath.startsWith(path + File.separator)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Runs the specified build jobs.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param buildJobs The build jobs to run must not be <code>null</code> nor contain <code>null</code> elements.
     * @throws org.apache.maven.plugin.MojoExecutionException If any build could not be launched.
     */
    private void runBuilds(final File projectsDir, List<BuildJob> buildJobs, int runWithParallelThreads)
            throws MojoExecutionException {
        if (!localRepositoryPath.exists()) {
            localRepositoryPath.mkdirs();
        }

        // -----------------------------------------------
        // interpolate settings file
        // -----------------------------------------------

        File interpolatedSettingsFile = interpolateSettings(settingsFile);

        final File mergedSettingsFile = mergeSettings(interpolatedSettingsFile);

        final CharSequence actualJreVersion;
        // @todo if ( javaVersions ) ... to be picked up from toolchains
        if (javaHome != null) {
            actualJreVersion = resolveExternalJreVersion();
        } else {
            actualJreVersion = SelectorUtils.getJreVersion();
        }

        final Path projectsPath = this.projectsDirectory.toPath();

        Set<Path> folderGroupSet = new HashSet<>();
        folderGroupSet.add(Paths.get("."));
        for (BuildJob buildJob : buildJobs) {
            Path p = Paths.get(buildJob.getProject());

            if (Files.isRegularFile(projectsPath.resolve(p))) {
                p = p.getParent();
            }

            if (p != null) {
                p = p.getParent();
            }

            while (p != null && folderGroupSet.add(p)) {
                p = p.getParent();
            }
        }

        List<Path> folderGroup = new ArrayList<>(folderGroupSet);
        Collections.sort(folderGroup);

        final Map<Path, Properties> globalInvokerProperties = new HashMap<>();

        for (Path path : folderGroup) {
            Properties ancestorProperties =
                    globalInvokerProperties.get(projectsPath.resolve(path).getParent());

            Path currentInvokerProperties = projectsPath.resolve(path).resolve(invokerPropertiesFile);

            Properties currentProperties;
            if (Files.isRegularFile(currentInvokerProperties)) {
                if (ancestorProperties != null) {
                    currentProperties = new Properties(ancestorProperties);

                } else {
                    currentProperties = new Properties();
                }
            } else {
                currentProperties = ancestorProperties;
            }

            if (Files.isRegularFile(currentInvokerProperties)) {
                try (InputStream in = new FileInputStream(currentInvokerProperties.toFile())) {
                    currentProperties.load(in);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to read invoker properties: " + currentInvokerProperties);
                }
            }

            if (currentProperties != null) {
                globalInvokerProperties.put(projectsPath.resolve(path).normalize(), currentProperties);
            }
        }

        try {
            if (runWithParallelThreads > 1) {
                getLog().info("use parallelThreads " + runWithParallelThreads);
            }

            JobExecutor jobExecutor = new JobExecutor(buildJobs, runWithParallelThreads);
            jobExecutor.forEach(job -> {
                Path ancestorFolder = getAncestorFolder(projectsPath.resolve(job.getProject()));

                runBuild(
                        projectsDir,
                        job,
                        mergedSettingsFile,
                        javaHome,
                        actualJreVersion,
                        globalInvokerProperties.get(ancestorFolder));
            });
        } finally {
            if (interpolatedSettingsFile != null && cloneProjectsTo == null) {
                interpolatedSettingsFile.delete();
            }
            if (mergedSettingsFile != null && mergedSettingsFile.exists()) {
                mergedSettingsFile.delete();
            }
        }
    }

    private Path getAncestorFolder(Path p) {
        Path ancestor = p;
        if (Files.isRegularFile(ancestor)) {
            ancestor = ancestor.getParent();
        }
        if (ancestor != null) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    /**
     * Interpolate settings.xml file.
     * @param settingsFile a settings file
     *
     * @return The interpolated settings.xml file.
     * @throws MojoExecutionException in case of a problem.
     */
    private File interpolateSettings(File settingsFile) throws MojoExecutionException {
        File interpolatedSettingsFile = null;
        if (settingsFile != null) {
            if (cloneProjectsTo != null) {
                interpolatedSettingsFile = new File(cloneProjectsTo, "interpolated-" + settingsFile.getName());
            } else {
                interpolatedSettingsFile =
                        new File(settingsFile.getParentFile(), "interpolated-" + settingsFile.getName());
            }
            buildInterpolatedFile(settingsFile, interpolatedSettingsFile);
        }
        return interpolatedSettingsFile;
    }

    /**
     * Merge the settings file
     *
     * @param interpolatedSettingsFile The interpolated settings file.
     * @return The merged settings file.
     * @throws MojoExecutionException Fail the build in case the merged settings file can't be created.
     */
    private File mergeSettings(File interpolatedSettingsFile) throws MojoExecutionException {
        File mergedSettingsFile;
        Settings mergedSettings = this.settings;
        if (mergeUserSettings) {
            if (interpolatedSettingsFile != null) {
                // Have to merge the specified settings file (dominant) and the one of the invoking Maven process
                try {
                    SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
                    request.setGlobalSettingsFile(interpolatedSettingsFile);

                    Settings dominantSettings = settingsBuilder.build(request).getEffectiveSettings();
                    Settings recessiveSettings = cloneSettings();
                    SettingsUtils.merge(dominantSettings, recessiveSettings, TrackableBase.USER_LEVEL);

                    mergedSettings = dominantSettings;
                    getLog().debug("Merged specified settings file with settings of invoking process");
                } catch (SettingsBuildingException e) {
                    throw new MojoExecutionException("Could not read specified settings file", e);
                }
            }
        }

        if (this.settingsFile != null && !mergeUserSettings) {
            mergedSettingsFile = interpolatedSettingsFile;
        } else {
            try {
                mergedSettingsFile = writeMergedSettingsFile(mergedSettings);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create temporary file for invoker settings.xml", e);
            }
        }
        return mergedSettingsFile;
    }

    private File writeMergedSettingsFile(Settings mergedSettings) throws IOException {
        File mergedSettingsFile;
        mergedSettingsFile = File.createTempFile("invoker-settings", ".xml");

        SettingsXpp3Writer settingsWriter = new SettingsXpp3Writer();

        try (FileWriter fileWriter = new FileWriter(mergedSettingsFile)) {
            settingsWriter.write(fileWriter, mergedSettings);
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Created temporary file for invoker settings.xml: " + mergedSettingsFile.getAbsolutePath());
        }
        return mergedSettingsFile;
    }

    private Settings cloneSettings() {
        Settings recessiveSettings = SettingsUtils.copySettings(this.settings);

        // MINVOKER-133: reset sourceLevelSet
        resetSourceLevelSet(recessiveSettings);
        for (org.apache.maven.settings.Mirror mirror : recessiveSettings.getMirrors()) {
            resetSourceLevelSet(mirror);
        }
        for (org.apache.maven.settings.Server server : recessiveSettings.getServers()) {
            resetSourceLevelSet(server);
        }
        for (org.apache.maven.settings.Proxy proxy : recessiveSettings.getProxies()) {
            resetSourceLevelSet(proxy);
        }
        for (org.apache.maven.settings.Profile profile : recessiveSettings.getProfiles()) {
            resetSourceLevelSet(profile);
        }

        return recessiveSettings;
    }

    private void resetSourceLevelSet(org.apache.maven.settings.TrackableBase trackable) {
        try {
            ReflectionUtils.setVariableValueInObject(trackable, "sourceLevelSet", Boolean.FALSE);
            getLog().debug("sourceLevelSet: "
                    + ReflectionUtils.getValueIncludingSuperclasses("sourceLevelSet", trackable));
        } catch (IllegalAccessException e) {
            // noop
        }
    }

    private CharSequence resolveExternalJreVersion() {
        Artifact pluginArtifact =
                mojoExecution.getMojoDescriptor().getPluginDescriptor().getPluginArtifact();
        pluginArtifact.getFile();

        Commandline commandLine = new Commandline();
        commandLine.setExecutable(new File(javaHome, "bin/java").getAbsolutePath());
        commandLine.createArg().setValue("-cp");
        commandLine.createArg().setFile(pluginArtifact.getFile());
        commandLine.createArg().setValue(SystemPropertyPrinter.class.getName());
        commandLine.createArg().setValue("java.version");

        final StringBuilder actualJreVersion = new StringBuilder();
        StreamConsumer consumer = actualJreVersion::append;
        try {
            CommandLineUtils.executeCommandLine(commandLine, consumer, null);
        } catch (CommandLineException e) {
            getLog().warn(e.getMessage());
        }
        return actualJreVersion;
    }

    /**
     * Interpolate the pom file.
     *
     * @param pomFile The pom file.
     * @param basedir The base directory.
     * @return interpolated pom file location in case we have interpolated the pom file otherwise the original pom file
     *         will be returned.
     * @throws MojoExecutionException
     */
    private File interpolatePomFile(File pomFile, File basedir) throws MojoExecutionException {
        File interpolatedPomFile = null;
        if (pomFile != null) {
            if (StringUtils.isNotEmpty(filteredPomPrefix)) {
                interpolatedPomFile = new File(basedir, filteredPomPrefix + pomFile.getName());
                buildInterpolatedFile(pomFile, interpolatedPomFile);
            } else {
                interpolatedPomFile = pomFile;
            }
        }
        return interpolatedPomFile;
    }

    /**
     * Runs the specified project.
     *
     * @param projectsDir The base directory of all projects, must not be <code>null</code>.
     * @param buildJob The build job to run, must not be <code>null</code>.
     * @param settingsFile The (already interpolated) user settings file for the build, may be <code>null</code> to use
     *            the current user settings.
     * @param globalInvokerProperties
     * @throws org.apache.maven.plugin.MojoExecutionException If the project could not be launched.
     */
    private void runBuild(
            File projectsDir,
            BuildJob buildJob,
            File settingsFile,
            File actualJavaHome,
            CharSequence actualJreVersion,
            Properties globalInvokerProperties)
            throws MojoExecutionException {
        // FIXME: Think about the following code part -- START
        File pomFile = new File(projectsDir, buildJob.getProject());
        File basedir;
        if (pomFile.isDirectory()) {
            basedir = pomFile;
            pomFile = new File(basedir, "pom.xml");
            if (!pomFile.exists()) {
                pomFile = null;
            } else {
                buildJob.setProject(buildJob.getProject() + File.separator + "pom.xml");
            }
        } else {
            basedir = pomFile.getParentFile();
        }

        File interpolatedPomFile = interpolatePomFile(pomFile, basedir);
        // FIXME: Think about the following code part -- ^^^^^^^ END

        getLog().info(buffer().a("Building: ").strong(buildJob.getProject()).toString());

        InvokerProperties invokerProperties = getInvokerProperties(basedir, globalInvokerProperties);

        // let's set what details we can
        buildJob.setName(invokerProperties.getJobName());
        buildJob.setDescription(invokerProperties.getJobDescription());

        try {
            int selection = getSelection(invokerProperties, actualJreVersion);
            if (selection == 0) {
                long milliseconds = System.currentTimeMillis();
                boolean executed;

                FileLogger buildLogger = setupBuildLogFile(basedir);
                if (buildLogger != null) {
                    buildJob.setBuildlog(buildLogger.getOutputFile().getAbsolutePath());
                }

                try {
                    executed = runBuild(
                            basedir, interpolatedPomFile, settingsFile, actualJavaHome, invokerProperties, buildLogger);
                } finally {
                    milliseconds = System.currentTimeMillis() - milliseconds;
                    buildJob.setTime(milliseconds / 1000.0);

                    if (buildLogger != null) {
                        buildLogger.close();
                    }
                }

                if (executed) {
                    buildJob.setResult(BuildJob.Result.SUCCESS);

                    if (!suppressSummaries) {
                        getLog().info(pad(buildJob).success("SUCCESS").a(' ') + formatTime(buildJob.getTime()));
                    }
                } else {
                    buildJob.setResult(BuildJob.Result.SKIPPED);

                    if (!suppressSummaries) {
                        getLog().info(pad(buildJob).warning("SKIPPED").a(' ') + formatTime(buildJob.getTime()));
                    }
                }
            } else {
                buildJob.setResult(BuildJob.Result.SKIPPED);

                List<String> messages = new ArrayList<>();

                if (selection == Selector.SELECTOR_MULTI) {
                    messages.add("non-matching selectors");
                } else {
                    if ((selection & Selector.SELECTOR_MAVENVERSION) != 0) {
                        messages.add("Maven version");
                    }
                    if ((selection & Selector.SELECTOR_JREVERSION) != 0) {
                        messages.add("JRE version");
                    }
                    if ((selection & Selector.SELECTOR_OSFAMILY) != 0) {
                        messages.add("OS");
                    }
                    if ((selection & Selector.SELECTOR_TOOLCHAIN) != 0) {
                        messages.add("Toolchain");
                    }
                }

                String message = String.join(", ", messages);
                if (!suppressSummaries) {
                    getLog().info(pad(buildJob).warning("SKIPPED") + " due to " + message);
                }

                // Abuse failureMessage, the field in the report which should contain the reason for skipping
                // Consider skipCode + I18N
                buildJob.setFailureMessage("Skipped due to " + message);
            }
        } catch (RunFailureException e) {
            buildJob.setResult(e.getType());
            buildJob.setFailureMessage(e.getMessage());

            if (!suppressSummaries) {
                getLog().info("  " + e.getMessage());
                getLog().info(pad(buildJob).failure("FAILED").a(' ') + formatTime(buildJob.getTime()));
            }
        } finally {
            deleteInterpolatedPomFile(interpolatedPomFile);
            writeBuildReport(buildJob);
        }
    }

    private MessageBuilder pad(BuildJob buildJob) {
        MessageBuilder buffer = buffer(128);

        buffer.a("          ");
        buffer.a(buildJob.getProject());

        int l = 10 + buildJob.getProject().length();

        if (l < RESULT_COLUMN) {
            buffer.a(' ');
            l++;

            if (l < RESULT_COLUMN) {
                for (int i = RESULT_COLUMN - l; i > 0; i--) {
                    buffer.a('.');
                }
            }
        }

        return buffer.a(' ');
    }

    /**
     * Delete the interpolated pom file if it has been created before.
     *
     * @param interpolatedPomFile The interpolated pom file.
     */
    private void deleteInterpolatedPomFile(File interpolatedPomFile) {
        if (interpolatedPomFile != null && StringUtils.isNotEmpty(filteredPomPrefix)) {
            interpolatedPomFile.delete();
        }
    }

    /**
     * Determines whether selector conditions of the specified invoker properties match the current environment.
     *
     * @param invokerProperties The invoker properties to check, must not be <code>null</code>.
     * @return <code>0</code> if the job corresponding to the properties should be run, otherwise a bitwise value
     *         representing the reason why it should be skipped.
     */
    private int getSelection(InvokerProperties invokerProperties, CharSequence actualJreVersion) {
        return new Selector(actualMavenVersion, actualJreVersion.toString(), getToolchainPrivateManager())
                .getSelection(invokerProperties);
    }

    private ToolchainPrivateManager getToolchainPrivateManager() {
        return new ToolchainPrivateManager(toolchainManagerPrivate, session);
    }

    /**
     * Writes the XML report for the specified build job unless report generation has been disabled.
     *
     * @param buildJob The build job whose report should be written, must not be <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the report could not be written.
     */
    private void writeBuildReport(BuildJob buildJob) throws MojoExecutionException {
        if (disableReports) {
            return;
        }

        String safeFileName =
                buildJob.getProject().replace('/', '_').replace('\\', '_').replace(' ', '_');
        if (safeFileName.endsWith("_pom.xml")) {
            safeFileName = safeFileName.substring(0, safeFileName.length() - "_pom.xml".length());
        }

        File reportFile = new File(reportsDirectory, "BUILD-" + safeFileName + ".xml");
        try (FileOutputStream fos = new FileOutputStream(reportFile);
                Writer osw = new OutputStreamWriter(fos, buildJob.getModelEncoding())) {
            BuildJobXpp3Writer writer = new BuildJobXpp3Writer();

            writer.write(osw, buildJob);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write build report " + reportFile, e);
        }

        if (writeJunitReport) {
            writeJunitReport(buildJob, safeFileName);
        }
    }

    private void writeJunitReport(BuildJob buildJob, String safeFileName) throws MojoExecutionException {
        File reportFile = new File(reportsDirectory, "TEST-" + safeFileName + ".xml");
        Xpp3Dom testsuite = new Xpp3Dom("testsuite");
        testsuite.setAttribute("name", junitPackageName + "." + safeFileName);
        testsuite.setAttribute("time", Double.toString(buildJob.getTime()));

        // set default value for required attributes
        testsuite.setAttribute("tests", "1");
        testsuite.setAttribute("errors", "0");
        testsuite.setAttribute("skipped", "0");
        testsuite.setAttribute("failures", "0");

        Xpp3Dom testcase = new Xpp3Dom("testcase");
        testsuite.addChild(testcase);
        switch (buildJob.getResult()) {
            case BuildJob.Result.SUCCESS:
                break;
            case BuildJob.Result.SKIPPED:
                testsuite.setAttribute("skipped", "1");
                // adding the failure element
                Xpp3Dom skipped = new Xpp3Dom("skipped");
                testcase.addChild(skipped);
                skipped.setValue(buildJob.getFailureMessage());
                break;
            case BuildJob.Result.ERROR:
                testsuite.setAttribute("errors", "1");
                break;
            default:
                testsuite.setAttribute("failures", "1");
                // adding the failure element
                Xpp3Dom failure = new Xpp3Dom("failure");
                testcase.addChild(failure);
                failure.setAttribute("message", buildJob.getFailureMessage());
        }
        testcase.setAttribute("classname", junitPackageName + "." + safeFileName);
        testcase.setAttribute("name", safeFileName);
        testcase.setAttribute("time", Double.toString(buildJob.getTime()));
        Xpp3Dom systemOut = new Xpp3Dom("system-out");
        testcase.addChild(systemOut);

        File buildLogFile = buildJob.getBuildlog() != null ? new File(buildJob.getBuildlog()) : null;

        if (buildLogFile != null && buildLogFile.exists()) {
            getLog().debug("fileLogger:" + buildLogFile);
            try {
                systemOut.setValue(FileUtils.fileRead(buildLogFile));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read logfile " + buildLogFile, e);
            }
        } else {
            getLog().debug(safeFileName + "not exists buildLogFile = " + buildLogFile);
        }

        try (FileOutputStream fos = new FileOutputStream(reportFile);
                Writer osw = new OutputStreamWriter(fos, buildJob.getModelEncoding())) {
            Xpp3DomWriter.write(osw, testsuite);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write JUnit build report " + reportFile, e);
        }
    }

    /**
     * Formats the specified build duration time.
     *
     * @param seconds The duration of the build.
     * @return The formatted time, never <code>null</code>.
     */
    private String formatTime(double seconds) {
        return secFormat.format(seconds);
    }

    /**
     * Runs the specified project.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param pomFile The (already interpolated) POM file, may be <code>null</code> for a POM-less Maven invocation.
     * @param settingsFile The (already interpolated) user settings file for the build, may be <code>null</code>. Will
     *            be merged with the settings file of the invoking Maven process.
     * @param invokerProperties The properties to use.
     * @param logger file logger to write execution build.log
     * @return <code>true</code> if the project was launched or <code>false</code> if the selector script indicated that
     *         the project should be skipped.
     * @throws org.apache.maven.plugin.MojoExecutionException If the project could not be launched.
     * @throws RunFailureException If either a hook script or the build itself
     *             failed.
     */
    private boolean runBuild(
            File basedir,
            File pomFile,
            File settingsFile,
            File actualJavaHome,
            InvokerProperties invokerProperties,
            FileLogger logger)
            throws MojoExecutionException, RunFailureException {
        if (getLog().isDebugEnabled() && !invokerProperties.getProperties().isEmpty()) {
            Properties props = invokerProperties.getProperties();
            getLog().debug("Using invoker properties:");
            for (String key : new TreeSet<>(props.stringPropertyNames())) {
                String value = props.getProperty(key);
                getLog().debug("  " + key + " = " + value);
            }
        }

        Map<String, Object> context = new LinkedHashMap<>();

        boolean selectorResult = true;

        try {
            try {
                scriptRunner.run("selector script", basedir, selectorScript, context, logger);
            } catch (ScriptReturnException e) {
                selectorResult = false;
                return false;
            } catch (ScriptException e) {
                throw new RunFailureException(BuildJob.Result.ERROR, e);
            }

            try {
                scriptRunner.run("pre-build script", basedir, preBuildHookScript, context, logger);
            } catch (ScriptException e) {
                throw new RunFailureException(BuildJob.Result.FAILURE_PRE_HOOK, e);
            }

            for (int invocationIndex = 1; ; invocationIndex++) {
                if (invocationIndex > 1 && !invokerProperties.isInvocationDefined(invocationIndex)) {
                    break;
                }

                final InvocationRequest request = new DefaultInvocationRequest();

                request.setBatchMode(true);

                // values only from Mojo configurations
                request.setLocalRepositoryDirectory(localRepositoryPath);
                request.setShowErrors(showErrors);
                request.setShowVersion(showVersion);
                request.setJavaHome(actualJavaHome);
                request.setMavenHome(mavenHome);
                setupLoggerForBuildJob(logger, request);

                request.setBaseDirectory(basedir);
                request.setPomFile(pomFile);

                String customSettingsFile = invokerProperties.getSettingsFile(invocationIndex);
                if (customSettingsFile != null) {
                    File interpolateSettingsFile = interpolateSettings(new File(customSettingsFile));
                    File mergeSettingsFile = mergeSettings(interpolateSettingsFile);

                    request.setUserSettingsFile(mergeSettingsFile);
                } else {
                    request.setUserSettingsFile(settingsFile);
                }

                Properties systemProperties =
                        getSystemProperties(basedir, invokerProperties.getSystemPropertiesFile(invocationIndex));
                request.setProperties(systemProperties);

                invokerProperties.configureInvocation(request, invocationIndex);

                if (getLog().isDebugEnabled()) {
                    try {
                        getLog().debug("Using MAVEN_OPTS: " + request.getMavenOpts());
                        getLog().debug("Executing: " + new MavenCommandLineBuilder().build(request));
                    } catch (CommandLineConfigurationException e) {
                        getLog().debug("Failed to display command line: " + e.getMessage());
                    }
                }

                try {
                    InvocationResult result = invoker.execute(request);
                    verify(result, invocationIndex, invokerProperties, logger);
                } catch (final MavenInvocationException e) {
                    getLog().debug("Error invoking Maven: " + e.getMessage(), e);
                    throw new RunFailureException(
                            "Maven invocation failed. " + e.getMessage(), BuildJob.Result.FAILURE_BUILD);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (selectorResult) {
                runPostBuildHook(basedir, context, logger);
            }
        }
        return true;
    }

    int getParallelThreadsCount() {
        if (parallelThreads.endsWith("C")) {
            double parallelThreadsMultiple =
                    Double.parseDouble(parallelThreads.substring(0, parallelThreads.length() - 1));
            return (int) (parallelThreadsMultiple * Runtime.getRuntime().availableProcessors());
        } else {
            return Integer.parseInt(parallelThreads);
        }
    }

    private void runPostBuildHook(File basedir, Map<String, Object> context, FileLogger logger)
            throws MojoExecutionException, RunFailureException {
        try {
            scriptRunner.run("post-build script", basedir, postBuildHookScript, context, logger);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ScriptException e) {
            throw new RunFailureException(e.getMessage(), BuildJob.Result.FAILURE_POST_HOOK, e);
        }
    }

    private void setupLoggerForBuildJob(final FileLogger logger, final InvocationRequest request) {
        if (logger != null) {
            request.setErrorHandler(logger);
            request.setOutputHandler(logger);
        }
    }

    /**
     * Initializes the build logger for the specified project. This will write the logging information into
     * {@code build.log}.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @return The build logger or <code>null</code> if logging has been disabled.
     * @throws org.apache.maven.plugin.MojoExecutionException If the log file could not be created.
     */
    private FileLogger setupBuildLogFile(File basedir) throws MojoExecutionException {
        FileLogger logger = null;

        if (!noLog) {
            Path projectLogDirectory;
            if (logDirectory == null) {
                projectLogDirectory = basedir.toPath();
            } else if (cloneProjectsTo != null) {
                projectLogDirectory =
                        logDirectory.toPath().resolve(cloneProjectsTo.toPath().relativize(basedir.toPath()));
            } else {
                projectLogDirectory =
                        logDirectory.toPath().resolve(projectsDirectory.toPath().relativize(basedir.toPath()));
            }

            try {
                if (streamLogs) {
                    logger = new FileLogger(
                            projectLogDirectory.resolve("build.log").toFile(), getLog());
                } else {
                    logger = new FileLogger(
                            projectLogDirectory.resolve("build.log").toFile());
                }

                getLog().debug("Build log initialized in: " + projectLogDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("Error initializing build logfile in: " + projectLogDirectory, e);
            }
        }

        return logger;
    }

    /**
     * Gets the system properties to use for the specified project.
     *
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param filename The filename to the properties file to load, may be <code>null</code> to use the default path
     *            given by {@link #testPropertiesFile}.
     * @return The system properties to use, may be empty but never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If the properties file exists but could not be read.
     */
    private Properties getSystemProperties(final File basedir, final String filename) throws MojoExecutionException {
        Properties collectedTestProperties = new Properties();

        if (properties != null) {
            // MINVOKER-118: property can have empty value, which is not accepted by collectedTestProperties
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getValue() != null) {
                    collectedTestProperties.put(entry.getKey(), entry.getValue());
                }
            }
        }

        File propertiesFile = null;
        if (filename != null) {
            propertiesFile = new File(basedir, filename);
        } else if (testPropertiesFile != null) {
            propertiesFile = new File(basedir, testPropertiesFile);
        }

        if (propertiesFile != null && propertiesFile.isFile()) {

            try (InputStream fin = new FileInputStream(propertiesFile)) {
                Properties loadedProperties = new Properties();
                loadedProperties.load(fin);
                collectedTestProperties.putAll(loadedProperties);
            } catch (IOException e) {
                throw new MojoExecutionException("Error reading system properties from " + propertiesFile);
            }
        }

        return collectedTestProperties;
    }

    /**
     * Verifies the invocation result.
     *
     * @param result The invocation result to check, must not be <code>null</code>.
     * @param invocationIndex The index of the invocation for which to check the exit code, must not be negative.
     * @param invokerProperties The invoker properties used to check the exit code, must not be <code>null</code>.
     * @param logger The build logger, may be <code>null</code> if logging is disabled.
     */
    private void verify(
            InvocationResult result, int invocationIndex, InvokerProperties invokerProperties, FileLogger logger)
            throws RunFailureException {
        if (result.getExecutionException() != null) {
            throw new RunFailureException(
                    "The Maven invocation failed. "
                            + result.getExecutionException().getMessage(),
                    BuildJob.Result.ERROR);
        } else if (!invokerProperties.isExpectedResult(result.getExitCode(), invocationIndex)) {
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("The build exited with code ")
                    .append(result.getExitCode())
                    .append(". ");
            if (logger != null) {
                buffer.append("See ");
                buffer.append(logger.getOutputFile().getAbsolutePath());
                buffer.append(" for details.");
            } else {
                buffer.append("See console output for details.");
            }
            throw new RunFailureException(buffer.toString(), BuildJob.Result.FAILURE_BUILD);
        }
    }

    private List<String> calculateIncludes() {
        if (invokerTest != null) {
            String[] testRegexes = StringUtils.split(invokerTest, ",");
            return Arrays.stream(testRegexes)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("!"))
                    .collect(Collectors.toList());
        } else {
            Set<String> uniqueIncludes = new HashSet<>();
            uniqueIncludes.addAll(pomIncludes);
            uniqueIncludes.addAll(setupIncludes);
            return new ArrayList<>(uniqueIncludes);
        }
    }

    private List<String> calculateExcludes() throws IOException {
        List<String> excludes;

        if (invokerTest != null) {
            String[] testRegexes = StringUtils.split(invokerTest, ",");
            excludes = Arrays.stream(testRegexes)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> s.startsWith("!"))
                    .map(s -> s.substring(1))
                    .collect(Collectors.toList());
        } else {
            excludes = pomExcludes != null ? new ArrayList<>(pomExcludes) : new ArrayList<>();
        }

        if (this.settingsFile != null) {
            String exclude = relativizePath(this.settingsFile, projectsDirectory.getCanonicalPath());
            if (exclude != null) {
                excludes.add(exclude.replace('\\', '/'));
                getLog().debug("Automatically excluded " + exclude + " from project scanning");
            }
        }
        return excludes;
    }

    /**
     * Gets the build jobs that should be processed. Note that the order of the returned build jobs is significant.
     *
     * @return The build jobs to process, may be empty but never <code>null</code>.
     * @throws java.io.IOException If the projects directory could not be scanned.
     */
    List<BuildJob> getBuildJobs() throws IOException, MojoExecutionException {

        List<String> includes = calculateIncludes();
        List<String> excludes = calculateExcludes();
        List<BuildJob> buildJobsAll = scanProjectsDirectory(includes, excludes);
        List<BuildJob> buildJobsSetup = scanProjectsDirectory(setupIncludes, excludes);

        List<String> setupProjects =
                buildJobsSetup.stream().map(BuildJob::getProject).collect(Collectors.toList());

        for (BuildJob job : buildJobsAll) {
            if (setupProjects.contains(job.getProject())) {
                job.setType(BuildJob.Type.SETUP);
            }
            InvokerProperties invokerProperties =
                    getInvokerProperties(new File(projectsDirectory, job.getProject()).getParentFile(), null);
            job.setOrdinal(invokerProperties.getOrdinal());
        }

        relativizeProjectPaths(buildJobsAll);

        return buildJobsAll;
    }

    /**
     * Scans the projects directory for projects to build. Both (POM) files and mere directories will be matched by the
     * scanner patterns. If the patterns match a directory which contains a file named "pom.xml", the results will
     * include the path to this file rather than the directory path in order to avoid duplicate invocations of the same
     * project.
     *
     * @param includes The include patterns for the scanner, may be <code>null</code>.
     * @param excludes The exclude patterns for the scanner, may be <code>null</code> to exclude nothing.
     * @return The build jobs matching the patterns, never <code>null</code>.
     * @throws java.io.IOException If the project directory could not be scanned.
     */
    private List<BuildJob> scanProjectsDirectory(List<String> includes, List<String> excludes) throws IOException {
        if (!projectsDirectory.isDirectory()) {
            return Collections.emptyList();
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(projectsDirectory.getCanonicalFile());
        scanner.setFollowSymlinks(false);
        if (includes != null) {
            scanner.setIncludes(includes.toArray(new String[0]));
        }
        if (excludes != null) {
            if ((includes == null || includes.isEmpty()) && !excludes.isEmpty()) {
                scanner.setIncludes(new String[] {"*"});
            }
            scanner.setExcludes(excludes.toArray(new String[0]));
        }
        scanner.addDefaultExcludes();
        scanner.scan();

        Map<String, BuildJob> matches = new LinkedHashMap<>();

        for (String includedFile : scanner.getIncludedFiles()) {
            matches.put(includedFile, new BuildJob(includedFile));
        }

        for (String includedDir : scanner.getIncludedDirectories()) {
            String includedFile = includedDir + File.separatorChar + "pom.xml";
            if (new File(scanner.getBasedir(), includedFile).isFile()) {
                matches.put(includedFile, new BuildJob(includedFile));
            } else {
                matches.put(includedDir, new BuildJob(includedDir));
            }
        }

        return new ArrayList<>(matches.values());
    }

    /**
     * Relativizes the project paths of the specified build jobs against the directory specified by
     * {@link #projectsDirectory} (if possible). If a project path does not denote a sub path of the projects directory,
     * it is returned as is.
     *
     * @param buildJobs The build jobs whose project paths should be relativized, must not be <code>null</code> nor
     *            contain <code>null</code> elements.
     * @throws java.io.IOException If any path could not be relativized.
     */
    private void relativizeProjectPaths(List<BuildJob> buildJobs) throws IOException {
        String projectsDirPath = projectsDirectory.getCanonicalPath();

        for (BuildJob buildJob : buildJobs) {
            String projectPath = buildJob.getProject();

            File file = new File(projectPath);

            if (!file.isAbsolute()) {
                file = new File(projectsDirectory, projectPath);
            }

            String relativizedPath = relativizePath(file, projectsDirPath);

            if (relativizedPath == null) {
                relativizedPath = projectPath;
            }

            buildJob.setProject(relativizedPath);
        }
    }

    /**
     * Relativizes the specified path against the given base directory. Besides relativization, the returned path will
     * also be normalized, e.g. directory references like ".." will be removed.
     *
     * @param path The path to relativize, must not be <code>null</code>.
     * @param basedir The (canonical path of the) base directory to relativize against, must not be <code>null</code>.
     * @return The relative path in normal form or <code>null</code> if the input path does not denote a sub path of the
     *         base directory.
     * @throws java.io.IOException If the path could not be relativized.
     */
    private String relativizePath(File path, String basedir) throws IOException {
        String relativizedPath = path.getCanonicalPath();

        if (relativizedPath.startsWith(basedir)) {
            relativizedPath = relativizedPath.substring(basedir.length());
            if (relativizedPath.startsWith(File.separator)) {
                relativizedPath = relativizedPath.substring(File.separator.length());
            }

            return relativizedPath;
        } else {
            return null;
        }
    }

    /**
     * Returns the map-based value source used to interpolate POMs and other stuff.
     *
     * @param escapeXml {@code true}, to escape any XML special characters in the property values; {@code false}, to not
     * escape any property values.
     *
     * @return The map-based value source for interpolation, never <code>null</code>.
     */
    private Map<String, Object> getInterpolationValueSource(final boolean escapeXml) {
        Map<String, Object> props = new HashMap<>();

        if (filterProperties != null) {
            props.putAll(filterProperties);
        }
        props.put("basedir", this.project.getBasedir().getAbsolutePath());
        props.put("baseurl", toUrl(this.project.getBasedir().getAbsolutePath()));
        if (settings.getLocalRepository() != null) {
            props.put("localRepository", settings.getLocalRepository());
            props.put("localRepositoryUrl", toUrl(settings.getLocalRepository()));
        }

        return new CompositeMap(this.project, props, escapeXml);
    }

    /**
     * Converts the specified filesystem path to a URL. The resulting URL has no trailing slash regardless whether the
     * path denotes a file or a directory.
     *
     * @param filename The filesystem path to convert, must not be <code>null</code>.
     * @return The <code>file:</code> URL for the specified path, never <code>null</code>.
     */
    private static String toUrl(String filename) {
        /*
         * NOTE: Maven fails to properly handle percent-encoded "file:" URLs (WAGON-111) so don't use File.toURI() here
         * as-is but use the decoded path component in the URL.
         */
        String url = "file://" + new File(filename).toURI().getPath();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Interpolates the specified POM/settings file to a temporary file. The destination file may be same as the input
     * file, i.e. interpolation can be performed in-place.
     * <p>
     * <b>Note:</b>This methods expects the file to be a XML file and applies special XML escaping during interpolation.
     * </p>
     *
     * @param originalFile The XML file to interpolate, must not be <code>null</code>.
     * @param interpolatedFile The target file to write the interpolated contents of the original file to, must not be
     * <code>null</code>.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If the target file could not be created.
     */
    void buildInterpolatedFile(File originalFile, File interpolatedFile) throws MojoExecutionException {
        getLog().debug("Interpolate " + originalFile.getPath() + " to " + interpolatedFile.getPath());

        try {
            String xml;

            Map<String, Object> composite = getInterpolationValueSource(true);

            // interpolation with token @...@
            try (Reader reader =
                    new InterpolationFilterReader(ReaderFactory.newXmlReader(originalFile), composite, "@", "@")) {
                xml = IOUtil.toString(reader);
            }

            try (Writer writer = WriterFactory.newXmlWriter(interpolatedFile)) {
                interpolatedFile.getParentFile().mkdirs();

                writer.write(xml);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to interpolate file " + originalFile.getPath(), e);
        }
    }

    /**
     * Gets the (interpolated) invoker properties for an integration test.
     *
     * @param projectDirectory The base directory of the IT project, must not be <code>null</code>.
     * @return The invoker properties, may be empty but never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException If an I/O error occurred during reading the properties.
     */
    private InvokerProperties getInvokerProperties(final File projectDirectory, Properties globalInvokerProperties)
            throws MojoExecutionException {
        Properties props;
        if (globalInvokerProperties != null) {
            props = new Properties(globalInvokerProperties);
        } else {
            props = new Properties();
        }

        File propertiesFile = new File(projectDirectory, invokerPropertiesFile);
        if (propertiesFile.isFile()) {
            try (InputStream in = new FileInputStream(propertiesFile)) {
                props.load(in);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read invoker properties: " + propertiesFile, e);
            }
        }

        Interpolator interpolator = new RegexBasedInterpolator();
        interpolator.addValueSource(new MapBasedValueSource(getInterpolationValueSource(false)));
        // CHECKSTYLE_OFF: LineLength
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            try {
                value = interpolator.interpolate(value, "");
            } catch (InterpolationException e) {
                throw new MojoExecutionException("Failed to interpolate invoker properties: " + propertiesFile, e);
            }
            props.setProperty(key, value);
        }

        InvokerProperties invokerProperties = new InvokerProperties(props);

        // set default value for Invoker - it will be used if not present in properties
        invokerProperties.setDefaultDebug(debug);
        invokerProperties.setDefaultQuiet(quiet);
        invokerProperties.setDefaultGoals(goals);
        invokerProperties.setDefaultProfiles(profiles);
        invokerProperties.setDefaultMavenExecutable(mavenExecutable);
        invokerProperties.setDefaultMavenOpts(mavenOpts);
        invokerProperties.setDefaultTimeoutInSeconds(timeoutInSeconds);
        invokerProperties.setDefaultEnvironmentVariables(environmentVariables);
        invokerProperties.setDefaultUpdateSnapshots(updateSnapshots);

        return invokerProperties;
    }

    static class ToolchainPrivateManager {
        private ToolchainManagerPrivate manager;

        private MavenSession session;

        ToolchainPrivateManager(ToolchainManagerPrivate manager, MavenSession session) {
            this.manager = manager;
            this.session = session;
        }

        ToolchainPrivate[] getToolchainPrivates(String type) throws MisconfiguredToolchainException {
            return manager.getToolchainsForType(type, session);
        }
    }
}
