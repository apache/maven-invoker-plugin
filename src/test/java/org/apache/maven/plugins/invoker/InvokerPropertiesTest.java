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
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest.ReactorFailureBehavior;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the invoker properties facade.
 *
 * @author Benjamin Bentmann
 */
@RunWith( MockitoJUnitRunner.class )
public class InvokerPropertiesTest
{

    @Mock
    private InvocationRequest request;

    @Test
    public void testConstructorNullSafe()
    {
        InvokerProperties facade = new InvokerProperties( null );
        assertNotNull( facade.getProperties() );
    }

    @Test
    public void testGetInvokerProperty()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        assertNull( facade.get( "undefined-key", 0 ) );

        props.setProperty( "key", "value" );
        assertEquals( "value", facade.get( "key", 1 ) );

        props.setProperty( "key.1", "another-value" );
        assertEquals( "another-value", facade.get( "key", 1 ) );
        assertEquals( "value", facade.get( "key", 2 ) );
    }

    @Test
    public void testGetJobName()
    {
        Properties props = new Properties();
        final String jobName = "Build Job name";
        props.put( "invoker.name", jobName );
        InvokerProperties facade = new InvokerProperties( props );

        assertEquals( jobName, facade.getJobName() );
    }

    @Test
    public void testIsExpectedResult()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        assertTrue( facade.isExpectedResult( 0, 0 ) );
        assertFalse( facade.isExpectedResult( 1, 0 ) );

        props.setProperty( "invoker.buildResult", "success" );
        assertTrue( facade.isExpectedResult( 0, 0 ) );
        assertFalse( facade.isExpectedResult( 1, 0 ) );

        props.setProperty( "invoker.buildResult", "failure" );
        assertFalse( facade.isExpectedResult( 0, 0 ) );
        assertTrue( facade.isExpectedResult( 1, 0 ) );
    }

    @Test
    public void testConfigureRequestEmptyProperties()
    {

        InvokerProperties facade = new InvokerProperties( null );

        facade.configureInvocation( request, 0 );
        verifyZeroInteractions( request );
    }

    @Test
    public void testConfigureRequestGoals()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.goals", "verify" );
        facade.configureInvocation( request, 0 );
        verify( request ).setGoals( eq( Collections.singletonList( "verify" ) ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.goals", "   " );
        facade.configureInvocation( request, 0 );
        verify( request ).setGoals( eq( Collections.<String>emptyList() ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.goals", "" );
        facade.configureInvocation( request, 0 );
        verify( request ).setGoals( eq( Collections.<String>emptyList() ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.goals", "  clean , test   verify " );
        facade.configureInvocation( request, 0 );
        verify( request ).setGoals( eq( Arrays.asList( "clean", "test", "verify" ) ) );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestProfiles()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.profiles", "verify" );
        facade.configureInvocation( request, 0 );
        verify( request ).setProfiles( eq( Collections.singletonList( "verify" ) ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.profiles", "   " );
        facade.configureInvocation( request, 0 );
        verify( request ).setProfiles( eq( Collections.<String>emptyList() ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.profiles", "" );
        facade.configureInvocation( request, 0 );
        verify( request ).setProfiles( eq( Collections.<String>emptyList() ) );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.profiles", "  clean , test   verify  ," );
        facade.configureInvocation( request, 0 );
        verify( request ).setProfiles( eq( Arrays.asList( "clean", "test", "verify" ) ) );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestProject() throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        File tempPom = File.createTempFile( "maven-invoker-plugin-test", ".pom" );
        try
        {
            File tempDir = tempPom.getParentFile();
            when( request.getBaseDirectory() ).thenReturn( tempDir );

            props.setProperty( "invoker.project", tempPom.getName() );
            facade.configureInvocation( request, 0 );
            verify( request ).getBaseDirectory();
            verify( request ).setBaseDirectory( eq( tempDir ) );
            verify( request ).setPomFile( eq( tempPom ) );
            verifyNoMoreInteractions( request );
            clearInvocations( request );

            props.setProperty( "invoker.project", "" );
            facade.configureInvocation( request, 0 );
            verify( request ).getBaseDirectory();
            verify( request ).setBaseDirectory( eq( tempDir ) );
            verify( request ).setPomFile( null );
            verifyNoMoreInteractions( request );
        }
        finally
        {
            tempPom.delete();
        }
    }

    @Test
    public void testConfigureRequestMavenOpts()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.mavenOpts", "-Xmx512m" );
        facade.configureInvocation( request, 0 );
        verify( request ).setMavenOpts( "-Xmx512m" );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestFailureBehavior()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.failureBehavior", ReactorFailureBehavior.FailNever.getLongOption() );
        facade.configureInvocation( request, 0 );
        verify( request ).setReactorFailureBehavior( eq( ReactorFailureBehavior.FailNever ) );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestFailureBehaviorUnKnownName()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.failureBehavior", "xxxUnKnown" );
        try
        {
            facade.configureInvocation( request, 0 );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "The string 'xxxUnKnown' can not be converted to enumeration.", e.getMessage() );
        }
        verifyZeroInteractions( request );
    }


    @Test
    public void testConfigureRequestRecursion()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.nonRecursive", "true" );
        facade.configureInvocation( request, 0 );
        verify( request ).setRecursive( false );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.nonRecursive", "false" );
        facade.configureInvocation( request, 0 );
        verify( request ).setRecursive( true );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestOffline()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.offline", "true" );
        facade.configureInvocation( request, 0 );
        verify( request ).setOffline( true );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.offline", "false" );
        facade.configureInvocation( request, 0 );
        verify( request ).setOffline( false );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureRequestDebug()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.debug", "true" );
        facade.configureInvocation( request, 0 );
        verify( request ).setDebug( true );
        verifyNoMoreInteractions( request );
        clearInvocations( request );

        props.setProperty( "invoker.debug", "false" );
        facade.configureInvocation( request, 0 );
        verify( request ).setDebug( false );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureEnvironmentVariables()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.abcdef", "abcdf" );
        props.setProperty( "invoker.environmentVariables.KEY1.1", "value1.1" );
        props.setProperty( "invoker.environmentVariables.KEY1", "value1" );
        props.setProperty( "invoker.environmentVariables.KEY2", "value2" );
        props.setProperty( "invoker.environmentVariables.KEY2.1", "value2.1" );
        props.setProperty( "invoker.environmentVariables.KEY3", "value3" );
        facade.configureInvocation( request, 0 );
        verify( request ).addShellEnvironment( "KEY1", "value1" );
        verify( request ).addShellEnvironment( "KEY2", "value2" );
        verify( request ).addShellEnvironment( "KEY3", "value3" );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testConfigureEnvironmentVariablesWithIndex()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        props.setProperty( "invoker.abcdef", "abcdf" );
        props.setProperty( "invoker.environmentVariables.KEY1.1", "value1.1" );
        props.setProperty( "invoker.environmentVariables.KEY1", "value1" );
        props.setProperty( "invoker.environmentVariables.KEY2", "value2" );
        props.setProperty( "invoker.environmentVariables.KEY2.1", "value2.1" );
        props.setProperty( "invoker.environmentVariables.KEY3", "value3" );
        facade.configureInvocation( request, 1 );
        verify( request ).addShellEnvironment( "KEY1", "value1.1" );
        verify( request ).addShellEnvironment( "KEY2", "value2.1" );
        verify( request ).addShellEnvironment( "KEY3", "value3" );
        verifyNoMoreInteractions( request );
    }

    @Test
    public void testIsInvocationDefined()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        assertFalse( facade.isInvocationDefined( 1 ) );

        props.setProperty( "invoker.goals", "install" );
        assertFalse( facade.isInvocationDefined( 1 ) );

        props.setProperty( "invoker.goals.2", "install" );
        assertFalse( facade.isInvocationDefined( 1 ) );
        assertTrue( facade.isInvocationDefined( 2 ) );
        assertFalse( facade.isInvocationDefined( 3 ) );

        props.setProperty( "invoker.goals.3", "install" );
        assertFalse( facade.isInvocationDefined( 1 ) );
        assertTrue( facade.isInvocationDefined( 2 ) );
        assertTrue( facade.isInvocationDefined( 3 ) );
        assertFalse( facade.isInvocationDefined( 4 ) );
    }

    @Test
    public void testIsSelectedDefined()
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        assertFalse( facade.isSelectorDefined( 1 ) );

        props.setProperty( "invoker.java.version", "1.6+" );
        props.setProperty( "invoker.maven.version", "3.0+" );
        props.setProperty( "invoker.os.family", "windows" );
        assertFalse( facade.isSelectorDefined( 1 ) );

        props.setProperty( "selector.2.java.version", "1.6+" );
        props.setProperty( "selector.3.maven.version", "3.0+" );
        props.setProperty( "selector.4.os.family", "windows" );
        assertFalse( facade.isSelectorDefined( 1 ) );
        assertTrue( facade.isSelectorDefined( 2 ) );
        assertTrue( facade.isSelectorDefined( 3 ) );
        assertTrue( facade.isSelectorDefined( 4 ) );
        assertFalse( facade.isSelectorDefined( 5 ) );
    }

    @Test
    public void testGetToolchainsForEmptyProperties()
    {

        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        Collection<InvokerToolchain> toolchains = facade.getToolchains();
        assertNotNull( toolchains );
        assertEquals( 0, toolchains.size() );

        toolchains = facade.getToolchains( 1 );
        assertNotNull( toolchains );
        assertEquals( 0, toolchains.size() );
    }

    @Test
    public void testGetToolchains()
    {
        Properties props = new Properties();
        props.put( "invoker.toolchain.jdk.version", "11" );
        InvokerProperties facade = new InvokerProperties( props );

        Collection<InvokerToolchain> toolchains = facade.getToolchains();
        assertNotNull( toolchains );
        assertEquals( 1, toolchains.size() );
        InvokerToolchain toolchain = toolchains.iterator().next();
        assertEquals( "jdk", toolchain.getType() );
        assertEquals( Collections.singletonMap( "version", "11" ), toolchain.getProvides() );
    }

    @Test
    public void testGetToolchainsWithIndex()
    {
        Properties props = new Properties();
        props.put( "selector.1.invoker.toolchain.jdk.version", "11" );
        InvokerProperties facade = new InvokerProperties( props );

        Collection<InvokerToolchain> toolchains = facade.getToolchains( 1 );
        assertNotNull( toolchains );
        assertEquals( 1, toolchains.size() );
        InvokerToolchain toolchain = toolchains.iterator().next();
        assertEquals( "jdk", toolchain.getType() );
        assertEquals( Collections.singletonMap( "version", "11" ), toolchain.getProvides() );
    }

}
