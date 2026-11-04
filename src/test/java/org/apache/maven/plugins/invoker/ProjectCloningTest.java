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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.apache.maven.plugins.invoker.TestUtil.getBasedir;
import static org.apache.maven.plugins.invoker.TestUtil.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the cloning of IT projects into {@code cloneProjectsTo} and the filtering of their POMs.
 */
class ProjectCloningTest {

    private static final File FILTERING = new File(getBasedir(), "src/test/resources/filtering");

    @TempDir
    Path tempDir;

    private final InvokerMojo mojo = new InvokerMojo(null, null, null, null);

    private File outerBasedir;

    private File cloneTo;

    @BeforeEach
    void setUp() throws Exception {
        outerBasedir = tempDir.resolve("outer").toFile();
        cloneTo = tempDir.resolve("outer/target/it").toFile();

        // the project that configures the invoker plugin, its values feed the filtering of the cloned IT projects
        MavenProject project = new MavenProject();
        project.setGroupId("org.apache.maven.plugins.invoker");
        project.setArtifactId("outer-project");
        project.setVersion("1.0-SNAPSHOT");
        project.setName("PASSED");
        project.setFile(new File(outerBasedir, "pom.xml"));
        project.getModel().getProperties().setProperty("propertyFromPropertiesSection", "PROPERTY-FROM-POM");
        project.getModel().getProperties().setProperty("itProperty", "FAILED");

        Settings settings = new Settings();
        settings.setLocalRepository(tempDir.resolve("local-repo").toString());

        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "settings", settings);
        setVariableValueToObject(mojo, "cloneProjectsTo", cloneTo);
    }

    private void cloneFrom(String projectsDirectory, String... projectPaths) throws Exception {
        setVariableValueToObject(mojo, "projectsDirectory", new File(FILTERING, projectsDirectory));
        mojo.cloneProjects(Arrays.asList(projectPaths));
    }

    private String read(String path, Charset charset) throws Exception {
        return new String(Files.readAllBytes(new File(cloneTo, path).toPath()), charset);
    }

    @Test
    void pomIsFilteredWithTheDocumentedPrecedence() throws Exception {
        Properties filterProperties = new Properties();
        filterProperties.setProperty("propertyFromPluginConfig", "PROPERTY-FROM-PLUGIN-CONFIG");
        filterProperties.setProperty("project-is-not-the-pom.version", "PASSED");
        filterProperties.setProperty("project.name", "FAILED");
        filterProperties.setProperty("itProperty", "PASSED");
        filterProperties.setProperty("project.nonExistingPomValue", "PASSED");
        filterProperties.setProperty("basedir", "FAILED");
        filterProperties.setProperty("baseurl", "FAILED");
        filterProperties.setProperty("localRepository", "FAILED");
        setVariableValueToObject(mojo, "filterProperties", filterProperties);

        cloneFrom("pom-filtering", "project/pom.xml");

        String xml = read("project/pom.xml", StandardCharsets.UTF_8);
        // Maven expressions are left for Maven itself
        assertThat(xml).contains("<prop0>${project.version}</prop0>");
        assertThat(xml).contains("<prop1>1.0-SNAPSHOT</prop1>");
        assertThat(xml).contains("<prop2>PROPERTY-FROM-PLUGIN-CONFIG</prop2>");
        assertThat(xml).contains("<prop3>PROPERTY-FROM-POM</prop3>");
        // the prefix matters, project-is-not-the-pom.version is not project.version
        assertThat(xml).contains("<prop4>PASSED</prop4>");
        // values of the POM precede filterProperties
        assertThat(xml).contains("<prop5>PASSED</prop5>");
        assertThat(xml).contains("<prop6>PASSED</prop6>");
        // filterProperties precede the properties of the POM
        assertThat(xml).contains("<prop7>PASSED</prop7>");
        // project.* falls back to filterProperties when the POM has no such value
        assertThat(xml).contains("<prop8>PASSED</prop8>");
        // built-in values are not overridden by filterProperties
        assertThat(xml).contains("<prop9>" + outerBasedir.getAbsolutePath() + "</prop9>");
        assertThat(xml).containsPattern("<prop10>file:///[^<]+</prop10>");
        assertThat(xml).contains("<prop11>" + tempDir.resolve("local-repo") + "</prop11>");
        assertThat(xml).containsPattern("<prop12>file:///[^<]+</prop12>");
        assertThat(xml).doesNotContain("FAILED").doesNotContain("@");
    }

    @ParameterizedTest
    @CsvSource({"latin-1, ISO-8859-1", "utf-8, UTF-8"})
    void pomKeepsItsEncodingWhileFiltering(String project, String encoding) throws Exception {
        Properties filterProperties = new Properties();
        filterProperties.setProperty("latin1Chars", "©®ÄÖÜäöüß");
        setVariableValueToObject(mojo, "filterProperties", filterProperties);

        cloneFrom("pom-filtering-encoding", project + "/pom.xml");

        String xml = read(project + "/pom.xml", Charset.forName(encoding));
        assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
        assertThat(xml).contains("<prop0>©®ÄÖÜäöüß</prop0>");
        assertThat(xml).contains("<prop1>©®ÄÖÜäöüß</prop1>");
    }

    @ParameterizedTest
    @CsvSource({"project/pom.xml", "no-pom-project"})
    void mavenExtensionsAreFiltered(String projectPath) throws Exception {
        // 'project' has a pom.xml, 'no-pom-project' has none: in both cases .mvn/extensions.xml must be filtered
        cloneFrom("extensions", projectPath);

        String project = projectPath.split("/")[0];
        File extensions = new File(cloneTo, project + "/.mvn/extensions.xml");
        assertThat(extensions).as("filtered extensions.xml").isFile();
        String xml = read(project + "/.mvn/extensions.xml", StandardCharsets.UTF_8);
        assertThat(xml)
                .contains("<groupId>org.apache.maven.plugins.invoker</groupId>")
                .contains("<artifactId>outer-project</artifactId>")
                .contains("<version>1.0-SNAPSHOT</version>")
                .doesNotContain("@project.groupId@")
                .doesNotContain("@project.artifactId@")
                .doesNotContain("@project.version@");
    }
}
