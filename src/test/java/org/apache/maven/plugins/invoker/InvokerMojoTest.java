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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 * @since 18 nov. 07
 */
public class InvokerMojoTest extends AbstractMojoTestCase {

    private static final String DUMMY_PROJECT = "dummy" + File.separator + "pom.xml";
    private static final String WITH_POM_DIR_PROJECT = "with-pom-project-dir" + File.separator + "pom.xml";
    private static final String INTERPOLATION_PROJECT = "interpolation" + File.separator + "pom.xml";
    private static final String WITHOUT_POM_PROJECT = "without-pom-project-dir";

    private MavenProject getMavenProject() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target/foo.txt"));
        return mavenProject;
    }

    public void testSingleInvokerTest() throws Exception {
        // given
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        setVariableValueToObject(invokerMojo, "projectsDirectory", new File(dirPath));
        setVariableValueToObject(invokerMojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(invokerMojo, "project", getMavenProject());
        setVariableValueToObject(invokerMojo, "invokerTest", "*dummy*");
        setVariableValueToObject(invokerMojo, "settings", new Settings());

        // when
        List<BuildJob> jobs = invokerMojo.getBuildJobs();

        // then
        assertThat(jobs).map(BuildJob::getProject).containsExactlyInAnyOrder(DUMMY_PROJECT);
    }

    public void testMultiInvokerTest() throws Exception {
        // given
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        setVariableValueToObject(invokerMojo, "projectsDirectory", new File(dirPath));
        setVariableValueToObject(invokerMojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(invokerMojo, "project", getMavenProject());
        setVariableValueToObject(invokerMojo, "invokerTest", "*dummy*,*terpolatio*");
        setVariableValueToObject(invokerMojo, "settings", new Settings());

        // when
        List<BuildJob> jobs = invokerMojo.getBuildJobs();

        // then
        assertThat(jobs).map(BuildJob::getProject).containsExactlyInAnyOrder(DUMMY_PROJECT, INTERPOLATION_PROJECT);
    }

    public void testFullPatternInvokerTest() throws Exception {
        // given
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        setVariableValueToObject(invokerMojo, "projectsDirectory", new File(dirPath));
        setVariableValueToObject(invokerMojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(invokerMojo, "project", getMavenProject());
        setVariableValueToObject(invokerMojo, "invokerTest", "*");
        setVariableValueToObject(invokerMojo, "settings", new Settings());

        // when
        List<BuildJob> jobs = invokerMojo.getBuildJobs();

        // then
        assertThat(jobs)
                .map(BuildJob::getProject)
                .containsExactlyInAnyOrder(
                        DUMMY_PROJECT, WITH_POM_DIR_PROJECT, WITHOUT_POM_PROJECT, INTERPOLATION_PROJECT);
    }

    public void testSetupInProjectList() throws Exception {
        // given
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        setVariableValueToObject(invokerMojo, "projectsDirectory", new File(dirPath));
        setVariableValueToObject(invokerMojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(invokerMojo, "project", getMavenProject());
        setVariableValueToObject(invokerMojo, "settings", new Settings());
        setVariableValueToObject(invokerMojo, "setupIncludes", Collections.singletonList("dum*/pom.xml"));

        // when
        List<BuildJob> jobs = invokerMojo.getBuildJobs();

        // then

        // we have all projects with pom.xml
        assertThat(jobs)
                .map(BuildJob::getProject)
                .containsExactlyInAnyOrder(DUMMY_PROJECT, WITH_POM_DIR_PROJECT, INTERPOLATION_PROJECT);

        // and we have one setup project
        assertThat(jobs)
                .filteredOn(job -> BuildJob.Type.SETUP.equals(job.getType()))
                .map(BuildJob::getProject)
                .containsExactlyInAnyOrder(DUMMY_PROJECT);
    }

    public void testSetupProjectIsFiltered() throws Exception {
        // given
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        setVariableValueToObject(invokerMojo, "projectsDirectory", new File(dirPath));
        setVariableValueToObject(invokerMojo, "invokerPropertiesFile", "invoker.properties");
        setVariableValueToObject(invokerMojo, "project", getMavenProject());
        setVariableValueToObject(invokerMojo, "settings", new Settings());
        setVariableValueToObject(invokerMojo, "setupIncludes", Collections.singletonList("dum*/pom.xml"));
        setVariableValueToObject(invokerMojo, "invokerTest", "*project-dir*");

        // when
        List<BuildJob> jobs = invokerMojo.getBuildJobs();

        // then

        // we have filtered projects
        assertThat(jobs).map(BuildJob::getProject).containsExactlyInAnyOrder(WITH_POM_DIR_PROJECT, WITHOUT_POM_PROJECT);

        // and we don't have a setup project
        assertThat(jobs)
                .filteredOn(job -> BuildJob.Type.SETUP.equals(job.getType()))
                .isEmpty();
    }

    public void testAlreadyCloned() {
        assertFalse(AbstractInvokerMojo.alreadyCloned("dir", Collections.emptyList()));
        assertTrue(AbstractInvokerMojo.alreadyCloned("dir", Collections.singletonList("dir")));
        assertTrue(AbstractInvokerMojo.alreadyCloned("dir" + File.separator + "sub", Collections.singletonList("dir")));
        assertFalse(AbstractInvokerMojo.alreadyCloned("dirs", Collections.singletonList("dir")));
    }

    public void testParallelThreadsSettings() throws IllegalAccessException {
        Object[][] testValues = {
            {"4", 4},
            {"1C", Runtime.getRuntime().availableProcessors()},
            {"2.5C", (int) (Double.parseDouble("2.5") * Runtime.getRuntime().availableProcessors())}
        };

        InvokerMojo invokerMojo = new InvokerMojo();

        for (Object[] testValue : testValues) {
            String parallelThreads = (String) testValue[0];
            int expectedParallelThreads = (Integer) testValue[1];

            setVariableValueToObject(invokerMojo, "parallelThreads", parallelThreads);

            assertEquals(expectedParallelThreads, invokerMojo.getParallelThreadsCount());
        }
    }
}
