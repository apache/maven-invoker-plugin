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
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest.ReactorFailureBehavior;

import junit.framework.TestCase;

/**
 * Tests the invoker properties facade.
 *
 * @author Benjamin Bentmann
 */
public class InvokerPropertiesTest
    extends TestCase
{

    public void testConstructorNullSafe()
        throws Exception
    {
        InvokerProperties facade = new InvokerProperties( null );
        assertNotNull( facade.getProperties() );
    }

    public void testGetInvokerProperty()
        throws Exception
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

    public void testGetJobName()
        throws Exception
    {
        Properties props = new Properties();
        final String jobName = "Build Job name";
        props.put( "invoker.name", jobName );
        InvokerProperties facade = new InvokerProperties( props );

        assertEquals( jobName, facade.getJobName() );
    }

    public void testIsExpectedResult()
        throws Exception
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

    public void testConfigureRequestGoals()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setGoals( Collections.singletonList( "test" ) );
        facade.configureInvocation( request, 0 );
        assertEquals( Collections.singletonList( "test" ), request.getGoals() );

        props.setProperty( "invoker.goals", "verify" );
        facade.configureInvocation( request, 0 );
        assertEquals( Collections.singletonList( "verify" ), request.getGoals() );

        props.setProperty( "invoker.goals", "   " );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[0] ), request.getGoals() );

        props.setProperty( "invoker.goals", "  clean , test   verify  " );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[] { "clean", "test", "verify" } ), request.getGoals() );

        props.setProperty( "invoker.goals", "" );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[0] ), request.getGoals() );
    }

    public void testConfigureRequestProfiles()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setProfiles( Collections.singletonList( "test" ) );
        facade.configureInvocation( request, 0 );
        assertEquals( Collections.singletonList( "test" ), request.getProfiles() );

        props.setProperty( "invoker.profiles", "verify" );
        facade.configureInvocation( request, 0 );
        assertEquals( Collections.singletonList( "verify" ), request.getProfiles() );

        props.setProperty( "invoker.profiles", "   " );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[0] ), request.getProfiles() );

        props.setProperty( "invoker.profiles", "  clean , test   verify  ," );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[] { "clean", "test", "verify" } ), request.getProfiles() );

        props.setProperty( "invoker.profiles", "" );
        facade.configureInvocation( request, 0 );
        assertEquals( Arrays.asList( new String[0] ), request.getProfiles() );
    }

    public void testConfigureRequestProject()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        File tempPom = File.createTempFile( "maven-invoker-plugin-test", ".pom" );
        File tempDir = tempPom.getParentFile();

        request.setBaseDirectory( tempDir );
        facade.configureInvocation( request, 0 );
        assertEquals( tempDir, request.getBaseDirectory() );
        assertEquals( null, request.getPomFile() );

        props.setProperty( "invoker.project", tempPom.getName() );
        request.setBaseDirectory( tempDir );
        facade.configureInvocation( request, 0 );
        assertEquals( tempDir, request.getBaseDirectory() );
        assertEquals( tempPom, request.getPomFile() );

        props.setProperty( "invoker.project", "" );
        request.setBaseDirectory( tempDir );
        facade.configureInvocation( request, 0 );
        assertEquals( tempDir, request.getBaseDirectory() );
        assertEquals( null, request.getPomFile() );

        tempPom.delete();
    }

    public void testConfigureRequestMavenOpts()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setMavenOpts( "default" );
        facade.configureInvocation( request, 0 );
        assertEquals( "default", request.getMavenOpts() );

        props.setProperty( "invoker.mavenOpts", "-Xmx512m" );
        facade.configureInvocation( request, 0 );
        assertEquals( "-Xmx512m", request.getMavenOpts() );
    }

    public void testConfigureRequestFailureBehavior()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setReactorFailureBehavior( ReactorFailureBehavior.FailAtEnd );
        facade.configureInvocation( request, 0 );
        assertEquals( ReactorFailureBehavior.FailAtEnd, request.getReactorFailureBehavior() );

        props.setProperty( "invoker.failureBehavior", ReactorFailureBehavior.FailNever.getLongOption() );
        facade.configureInvocation( request, 0 );
        assertEquals( "fail-never", request.getReactorFailureBehavior().getLongOption() );
    }

    public void testConfigureRequestRecursion()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setRecursive( true );
        facade.configureInvocation( request, 0 );
        assertTrue( request.isRecursive() );
        request.setRecursive( false );
        facade.configureInvocation( request, 0 );
        assertFalse( request.isRecursive() );

        props.setProperty( "invoker.nonRecursive", "true" );
        facade.configureInvocation( request, 0 );
        assertFalse( request.isRecursive() );

        props.setProperty( "invoker.nonRecursive", "false" );
        facade.configureInvocation( request, 0 );
        assertTrue( request.isRecursive() );
    }

    public void testConfigureRequestOffline()
        throws Exception
    {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties( props );

        InvocationRequest request = new DefaultInvocationRequest();

        request.setOffline( true );
        facade.configureInvocation( request, 0 );
        assertTrue( request.isOffline() );
        request.setOffline( false );
        facade.configureInvocation( request, 0 );
        assertFalse( request.isOffline() );

        props.setProperty( "invoker.offline", "true" );
        facade.configureInvocation( request, 0 );
        assertTrue( request.isOffline() );

        props.setProperty( "invoker.offline", "false" );
        facade.configureInvocation( request, 0 );
        assertFalse( request.isOffline() );
    }

    public void testIsInvocationDefined()
        throws Exception
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
}
