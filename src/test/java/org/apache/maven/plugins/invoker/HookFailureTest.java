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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.scriptinterpreter.ScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.plugins.invoker.TestUtil.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests which failure a job reports when a hook fails after an earlier failure of the same job.
 */
class HookFailureTest {

    @TempDir
    Path tempDir;

    private Invoker invoker;

    private InvokerMojo mojo;

    private Path projectDir;

    private File reportsDirectory;

    @BeforeEach
    void setUp() throws Exception {
        invoker = mock(Invoker.class);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(tempDir.resolve("pom.xml").toFile());
        mojo = new InvokerMojo(invoker, null, null, new InterpolatorUtils(mavenProject));

        Path projects = Files.createDirectories(tempDir.resolve("src/it"));
        projectDir = Files.createDirectories(projects.resolve("job"));
        Files.write(projectDir.resolve("pom.xml"), "<project/>".getBytes(StandardCharsets.UTF_8));
        reportsDirectory = Files.createDirectories(tempDir.resolve("reports")).toFile();

        setVariableValueToObject(mojo, "projectsDirectory", projects.toFile());
        setVariableValueToObject(mojo, "reportsDirectory", reportsDirectory);
        setVariableValueToObject(mojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(mojo, "project", mavenProject);
        setVariableValueToObject(mojo, "settings", new Settings());
        setVariableValueToObject(mojo, "filteredPomPrefix", "");
        setVariableValueToObject(mojo, "noLog", true);
        setVariableValueToObject(mojo, "suppressSummaries", true);
        setVariableValueToObject(mojo, "preBuildHookScript", "prebuild");
        setVariableValueToObject(mojo, "postBuildHookScript", "postbuild");
        setVariableValueToObject(mojo, "scriptRunner", new ScriptRunner());
        setVariableValueToObject(mojo, "actualMavenVersion", "3.9.0");
        mojo.setLog(mock(Log.class));
    }

    private void script(String name, String body) throws IOException {
        Files.write(projectDir.resolve(name), body.getBytes(StandardCharsets.UTF_8));
    }

    private void invocationExits(int exitCode) throws Exception {
        InvocationResult result = mock(InvocationResult.class);
        when(result.getExitCode()).thenReturn(exitCode);
        when(invoker.execute(any(InvocationRequest.class))).thenReturn(result);
    }

    private BuildJob runJob() throws Exception {
        BuildJob job = new BuildJob("job/pom.xml");
        mojo.runBuild(tempDir.resolve("src/it").toFile(), job, null, null, "21", new Properties());
        return job;
    }

    private boolean runBuild(FileLogger logger) throws Exception {
        return mojo.runBuild(
                projectDir.toFile(),
                projectDir.resolve("pom.xml").toFile(),
                null,
                null,
                new InvokerProperties(new Properties()),
                logger);
    }

    @Test
    void failedBuildIsKeptWhenPostBuildHookFails() throws Exception {
        invocationExits(1);
        script("postbuild.groovy", "assert false : 'post-build hook failed'\n");

        BuildJob job = runJob();

        assertThat(job.getResult()).isEqualTo(BuildJob.Result.FAILURE_BUILD);
        assertThat(job.getFailureMessage()).contains("The build exited with code 1");
    }

    @Test
    void failedPreBuildHookIsKeptWhenPostBuildHookFails() throws Exception {
        script("prebuild.groovy", "assert false : 'pre-build hook failed'\n");
        script("postbuild.groovy", "assert false : 'post-build hook failed'\n");

        BuildJob job = runJob();

        assertThat(job.getResult()).isEqualTo(BuildJob.Result.FAILURE_PRE_HOOK);
        assertThat(job.getFailureMessage()).contains("pre-build hook failed").doesNotContain("post-build hook failed");
    }

    @Test
    void postBuildHookFailureIsAttachedToTheFirstFailure() throws Exception {
        invocationExits(1);
        script("postbuild.groovy", "assert false : 'post-build hook failed'\n");
        File buildLog = tempDir.resolve("build.log").toFile();
        FileLogger logger = new FileLogger(buildLog, null);

        try {
            assertThatThrownBy(() -> runBuild(logger)).isInstanceOfSatisfying(RunFailureException.class, e -> {
                assertThat(e.getType()).isEqualTo(BuildJob.Result.FAILURE_BUILD);
                assertThat(e.getSuppressed())
                        .singleElement()
                        .isInstanceOfSatisfying(
                                RunFailureException.class,
                                hook -> assertThat(hook.getType()).isEqualTo(BuildJob.Result.FAILURE_POST_HOOK));
            });
        } finally {
            logger.close();
        }
        assertThat(new String(Files.readAllBytes(buildLog.toPath()), StandardCharsets.UTF_8))
                .contains("Post-build hook failed after an earlier failure");
    }

    @Test
    void unexpectedExceptionIsKeptWhenPostBuildHookFails() throws Exception {
        when(invoker.execute(any(InvocationRequest.class))).thenThrow(new IllegalStateException("boom"));
        script("postbuild.groovy", "assert false : 'post-build hook failed'\n");

        assertThatThrownBy(() -> runBuild(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom")
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
    }

    @Test
    void postBuildHookStillRunsAfterFailedBuild() throws Exception {
        invocationExits(1);
        script("postbuild.groovy", "new File(basedir, 'post-build-ran.txt').createNewFile()\n");

        BuildJob job = runJob();

        assertThat(job.getResult()).isEqualTo(BuildJob.Result.FAILURE_BUILD);
        assertThat(projectDir.resolve("post-build-ran.txt")).exists();
    }
}
