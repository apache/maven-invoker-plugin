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

/**
 * @author Olivier Lamy
 * @since 18 nov. 07
 */
public class InvokerMojoTest extends AbstractMojoTestCase
{

    private MavenProject getMavenProject()
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile( new File( "target/foo.txt" ) );
        return mavenProject;
    }

    public void testSingleInvokerTest() throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerPropertiesFile", "invoker.properties" );
        setVariableValueToObject( invokerMojo, "project", getMavenProject() );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*" );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        List<BuildJob> poms = invokerMojo.getBuildJobs();
        assertEquals( 1, poms.size() );
    }

    public void testMultiInvokerTest() throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerPropertiesFile", "invoker.properties" );
        setVariableValueToObject( invokerMojo, "project", getMavenProject() );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*,*terpolatio*" );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        List<BuildJob> poms = invokerMojo.getBuildJobs();
        assertEquals( 2, poms.size() );
    }

    public void testFullPatternInvokerTest() throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerPropertiesFile", "invoker.properties" );
        setVariableValueToObject( invokerMojo, "project", getMavenProject() );
        setVariableValueToObject( invokerMojo, "invokerTest", "*" );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        List<BuildJob> poms = invokerMojo.getBuildJobs();
        assertEquals( 4, poms.size() );
    }

    public void testAlreadyCloned()
    {
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dir", Collections.<String>emptyList() ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir", Collections.singletonList( "dir" ) ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir" + File.separator + "sub",
                Collections.singletonList( "dir" ) ) );
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dirs", Collections.singletonList( "dir" ) ) );
    }

    public void testParallelThreadsSettings() throws IllegalAccessException
    {
        Object[][] testValues = {
                {"4", 4},
                {"1C", Runtime.getRuntime().availableProcessors()},
                {"2.5C", (int) ( Double.parseDouble( "2.5" ) * Runtime.getRuntime().availableProcessors() )}
        };

        InvokerMojo invokerMojo = new InvokerMojo();

        for ( Object[] testValue : testValues )
        {
            String parallelThreads = (String) testValue[0];
            int expectedParallelThreads = (Integer) testValue[1];

            setVariableValueToObject( invokerMojo, "parallelThreads", parallelThreads );

            assertEquals( expectedParallelThreads, invokerMojo.getParallelThreadsCount() );
        }
    }

}
