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

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugins.invoker.model.BuildJob;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

import static org.apache.maven.plugins.invoker.TestUtil.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the files the invoker writes next to or about a build: the build log and the JUnit report.
 */
class BuildOutputFilesTest {

    @TempDir
    Path tempDir;

    private final InvokerMojo mojo = new InvokerMojo(null, null, null, null);

    @BeforeEach
    void setUp() throws Exception {
        setVariableValueToObject(
                mojo, "projectsDirectory", tempDir.resolve("src/it").toFile());
    }

    private void closeLogger(FileLogger logger) {
        if (logger != null) {
            logger.close();
        }
    }

    @Test
    void buildLogGoesToLogDirectoryNotToTheClone() throws Exception {
        File clone = tempDir.resolve("target/it").toFile();
        File module = new File(clone, "module");
        module.mkdirs();
        mojo.setCloneProjectsTo(clone);
        setVariableValueToObject(
                mojo, "logDirectory", tempDir.resolve("target/it-logs").toFile());

        closeLogger(mojo.setupBuildLogFile(module, 1, ""));

        assertThat(tempDir.resolve("target/it-logs/module/build.log")).isRegularFile();
        assertThat(tempDir.resolve("target/it/module/build.log")).doesNotExist();
    }

    @Test
    void noCloneMeansTheBuildLogStaysInTheSourceProject() throws Exception {
        File project = tempDir.resolve("src/it/clone-none").toFile();
        project.mkdirs();
        // "none" is the special value that switches cloning off
        mojo.setCloneProjectsTo(new File("none"));
        assertThat(ReflectionUtils.getValueIncludingSuperclasses("cloneProjectsTo", mojo))
                .isNull();

        closeLogger(mojo.setupBuildLogFile(project, 1, ""));

        assertThat(tempDir.resolve("src/it/clone-none/build.log")).isRegularFile();
    }

    private BuildJob buildJob(String result, String failureMessage, File buildLog) {
        BuildJob job = new BuildJob("project/pom.xml");
        job.setResult(result);
        job.setFailureMessage(failureMessage);
        job.setTime(1.5f);
        job.setBuildlog(buildLog.getPath());
        return job;
    }

    private Element readReport(String name) throws Exception {
        File report = tempDir.resolve("reports/TEST-" + name + ".xml").toFile();
        assertThat(report).isFile();
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(report)
                .getDocumentElement();
    }

    private Element child(Element parent, String name) {
        assertThat(parent.getElementsByTagName(name).getLength()).as(name).isEqualTo(1);
        return (Element) parent.getElementsByTagName(name).item(0);
    }

    private void writeReport(BuildJob job, String name) throws Exception {
        setVariableValueToObject(
                mojo, "reportsDirectory", tempDir.resolve("reports").toFile());
        setVariableValueToObject(mojo, "junitPackageName", "org.apache.maven.plugins.invoker");
        Files.createDirectories(tempDir.resolve("reports"));
        mojo.writeJunitReport(job, name);
    }

    @Test
    void junitReportOfSuccessfulBuildCarriesTheBuildLog() throws Exception {
        Path log = tempDir.resolve("build.log");
        Files.write(log, "BUILD SUCCESS".getBytes(StandardCharsets.UTF_8));

        writeReport(buildJob(BuildJob.Result.SUCCESS, null, log.toFile()), "project");

        Element suite = readReport("project");
        assertThat(suite.getAttribute("name")).isEqualTo("org.apache.maven.plugins.invoker.project");
        assertThat(suite.getAttribute("time")).isEqualTo("1.5");
        assertThat(suite.getAttribute("tests")).isEqualTo("1");
        assertThat(suite.getAttribute("errors")).isEqualTo("0");
        assertThat(suite.getAttribute("skipped")).isEqualTo("0");
        assertThat(suite.getAttribute("failures")).isEqualTo("0");
        Element testcase = child(suite, "testcase");
        assertThat(testcase.getAttribute("name")).isEqualTo("project");
        assertThat(child(testcase, "system-out").getTextContent()).isEqualTo("BUILD SUCCESS");
        assertThat(testcase.getElementsByTagName("failure").getLength()).isZero();
    }

    @Test
    void junitReportOfFailedBuildHasFailureMessage() throws Exception {
        Path log = tempDir.resolve("build.log");
        Files.write(log, "BUILD FAILURE".getBytes(StandardCharsets.UTF_8));

        writeReport(buildJob(BuildJob.Result.FAILURE_BUILD, "script failed", log.toFile()), "project_2");

        Element suite = readReport("project_2");
        assertThat(suite.getAttribute("tests")).isEqualTo("1");
        assertThat(suite.getAttribute("errors")).isEqualTo("0");
        assertThat(suite.getAttribute("skipped")).isEqualTo("0");
        assertThat(suite.getAttribute("failures")).isEqualTo("1");
        Element testcase = child(suite, "testcase");
        assertThat(testcase.getAttribute("name")).isEqualTo("project_2");
        assertThat(child(testcase, "failure").getAttribute("message")).isEqualTo("script failed");
        assertThat(child(testcase, "system-out").getTextContent()).isEqualTo("BUILD FAILURE");
    }

    @Test
    void junitReportCountsSkippedAndErroredBuilds() throws Exception {
        File missingLog = tempDir.resolve("missing.log").toFile();

        writeReport(buildJob(BuildJob.Result.SKIPPED, "not applicable", missingLog), "skipped");
        writeReport(buildJob(BuildJob.Result.ERROR, null, missingLog), "errored");

        Element skipped = readReport("skipped");
        assertThat(skipped.getAttribute("skipped")).isEqualTo("1");
        assertThat(skipped.getAttribute("failures")).isEqualTo("0");
        assertThat(child(child(skipped, "testcase"), "skipped").getTextContent())
                .isEqualTo("not applicable");
        Element errored = readReport("errored");
        assertThat(errored.getAttribute("errors")).isEqualTo("1");
        assertThat(errored.getAttribute("failures")).isEqualTo("0");
        // a build without log file still gets an (empty) system-out
        assertThat(child(child(errored, "testcase"), "system-out").getTextContent())
                .isEmpty();
    }
}
