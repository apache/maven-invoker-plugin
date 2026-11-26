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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.scriptinterpreter.ScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.plugins.invoker.TestUtil.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests how a job is reported when it ends with something else than a build failure.
 */
class ErroredJobTest {

    @TempDir
    Path tempDir;

    private Invoker invoker;

    private Log log;

    private InvokerMojo mojo;

    private File reportsDirectory;

    @BeforeEach
    void setUp() throws Exception {
        invoker = mock(Invoker.class);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(tempDir.resolve("pom.xml").toFile());
        mojo = new InvokerMojo(invoker, null, null, new InterpolatorUtils(mavenProject));

        Path projects = Files.createDirectories(tempDir.resolve("src/it"));
        Files.createDirectories(projects.resolve("job"));
        Files.write(projects.resolve("job/pom.xml"), "<project/>".getBytes());
        reportsDirectory = Files.createDirectories(tempDir.resolve("reports")).toFile();

        setVariableValueToObject(mojo, "projectsDirectory", projects.toFile());
        setVariableValueToObject(mojo, "reportsDirectory", reportsDirectory);
        setVariableValueToObject(mojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(mojo, "project", mavenProject);
        setVariableValueToObject(mojo, "settings", new Settings());
        setVariableValueToObject(mojo, "filteredPomPrefix", "");
        setVariableValueToObject(mojo, "noLog", true);
        setVariableValueToObject(mojo, "suppressSummaries", true);
        setVariableValueToObject(mojo, "scriptRunner", new ScriptRunner());
        setVariableValueToObject(mojo, "actualMavenVersion", "3.9.0");
        log = mock(Log.class);
        mojo.setLog(log);
    }

    private BuildJob readReport() throws Exception {
        File report = new File(reportsDirectory, "BUILD-job.xml");
        assertThat(report).isFile();
        return new BuildJobXpp3Reader().read(Files.newBufferedReader(report.toPath()));
    }

    @Test
    void runtimeExceptionIsWrittenToTheReportAsError() throws Exception {
        when(invoker.execute(any())).thenThrow(new IllegalStateException("boom"));
        BuildJob job = new BuildJob("job");

        assertThatThrownBy(() ->
                        mojo.runBuild(tempDir.resolve("src/it").toFile(), job, null, null, "21", new Properties()))
                .isInstanceOf(IllegalStateException.class);

        BuildJob reported = readReport();
        assertThat(reported.getResult()).isEqualTo(BuildJob.Result.ERROR);
        assertThat(reported.getFailureMessage()).contains("IllegalStateException", "boom");
        verify(log).error((CharSequence) contains("IllegalStateException: boom"));
    }

    @Test
    void jobThatCannotBeSetUpIsWrittenToTheReportAsError() throws Exception {
        // a malformed unicode escape, so reading the invoker properties fails before the build starts
        Files.write(tempDir.resolve("src/it/job/invoker.properties"), "invoker.name = \\uZZZZ\n".getBytes());
        BuildJob job = new BuildJob("job");

        assertThatThrownBy(() ->
                        mojo.runBuild(tempDir.resolve("src/it").toFile(), job, null, null, "21", new Properties()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(readReport().getResult()).isEqualTo(BuildJob.Result.ERROR);
    }

    @Test
    void scriptUserPropertiesArePassedAsStrings() {
        Properties scriptProperties = new Properties();
        scriptProperties.put("text", "plain");
        scriptProperties.put("number", 42);
        scriptProperties.put("charSequence", new StringBuilder("builder"));
        Properties userProperties = new Properties();
        userProperties.setProperty("existing", "kept");

        AbstractInvokerMojo.addScriptUserProperties(userProperties, scriptProperties);

        assertThat(userProperties.get("text")).isEqualTo("plain");
        assertThat(userProperties.get("number")).isEqualTo("42");
        assertThat(userProperties.get("charSequence")).isEqualTo("builder");
        assertThat(userProperties.get("existing")).isEqualTo("kept");
    }
}
