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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.invoker.AbstractInvokerMojo.ToolchainPrivateManager;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.junit.Test;

/**
 * Tests {@link SelectorUtils}.
 *
 * @author Benjamin Bentmann
 */
public class SelectorUtilsTest
{

    @Test
    public void testParseList()
    {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();

        SelectorUtils.parseList( null, includes, excludes );

        SelectorUtils.parseList( " 1.5, !1.4, 1.6+ ", includes, excludes );
        assertEquals( Arrays.asList( "1.5", "1.6+" ), includes );
        assertEquals( Arrays.asList( "1.4" ), excludes );
    }

    @Test
    public void testParseVersion()
    {
        assertEquals( Arrays.asList( 1, 6, 0, 12 ), SelectorUtils.parseVersion( "1.6.0_12" ) );

        assertEquals( Arrays.asList( 1, 6, 0, 12 ), SelectorUtils.parseVersion( "1.6.0_12+" ) );
        assertEquals( Arrays.asList( 1, 6, 0, 12 ), SelectorUtils.parseVersion( "1.6.0_12-" ) );
    }

    @Test
    public void testCompareVersions()
    {
        assertTrue( SelectorUtils.compareVersions( Arrays.asList( 1, 6 ), Arrays.asList( 1, 6 ) ) == 0 );

        assertTrue( SelectorUtils.compareVersions( Arrays.asList( 1, 5 ), Arrays.asList( 1, 6 ) ) < 0 );
        assertTrue( SelectorUtils.compareVersions( Arrays.asList( 1, 6 ), Arrays.asList( 1, 5 ) ) > 0 );

        assertTrue( SelectorUtils.compareVersions( Arrays.asList( 1 ), Arrays.asList( 1, 6 ) ) < 0 );
        assertTrue( SelectorUtils.compareVersions( Arrays.asList( 1, 6 ), Arrays.asList( 1 ) ) > 0 );
    }

    @Test
    public void testIsMatchingJre()
    {

        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 4, 2, 8 ), "1.5" ) );
        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 5 ), "1.5" ) );
        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 5, 9 ), "1.5" ) );
        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 6 ), "1.5" ) );

        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 4, 2, 8 ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 5 ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 5, 9 ), "1.5+" ) );
        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 6 ), "1.5+" ) );

        assertTrue( SelectorUtils.isJreVersion( Arrays.asList( 1, 4, 2, 8 ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 5 ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 5, 9 ), "1.5-" ) );
        assertFalse( SelectorUtils.isJreVersion( Arrays.asList( 1, 6 ), "1.5-" ) );

        assertTrue( SelectorUtils.isJreVersion( (String) null, "1.5" ) );
        assertTrue( SelectorUtils.isJreVersion( "", "1.5" ) );
    }
    
    @Test
    public void testIsMatchingToolchain() throws Exception
    {
        InvokerToolchain openJdk9 = new InvokerToolchain( "jdk" );
        openJdk9.addProvides( "version", "9" );
        openJdk9.addProvides( "vendor", "openJDK" );

        InvokerToolchain maven360 = new InvokerToolchain( "maven" );
        openJdk9.addProvides( "version", "3.6.0" );

        ToolchainPrivateManager toolchainPrivateManager = mock( ToolchainPrivateManager.class );
        ToolchainPrivate jdkMatching = mock( ToolchainPrivate.class );
        when( jdkMatching.matchesRequirements( isA( Map.class ) ) ).thenReturn( true );
        when( jdkMatching.getType() ).thenReturn( "jdk" );

        ToolchainPrivate jdkMismatch = mock( ToolchainPrivate.class );
        when( jdkMismatch.getType() ).thenReturn( "jdk" );

        when( toolchainPrivateManager.getToolchainPrivates( "jdk" ) )
                .thenReturn( new ToolchainPrivate[] { jdkMatching } );
        assertTrue( SelectorUtils.isToolchain( toolchainPrivateManager, Collections.singleton( openJdk9 ) ) );

        when( toolchainPrivateManager.getToolchainPrivates( "jdk" ) )
                .thenReturn( new ToolchainPrivate[] { jdkMismatch } );
        assertFalse( SelectorUtils.isToolchain( toolchainPrivateManager, Collections.singleton( openJdk9 ) ) );

        when( toolchainPrivateManager.getToolchainPrivates( "jdk" ) )
                .thenReturn( new ToolchainPrivate[] { jdkMatching, jdkMismatch, jdkMatching } );
        assertTrue( SelectorUtils.isToolchain( toolchainPrivateManager, Collections.singleton( openJdk9 ) ) );

        when( toolchainPrivateManager.getToolchainPrivates( "jdk" ) )
                .thenReturn( new ToolchainPrivate[0] );
        assertFalse( SelectorUtils.isToolchain( toolchainPrivateManager, Collections.singleton( openJdk9 ) ) );
        
        when( toolchainPrivateManager.getToolchainPrivates( "jdk" ) )
                .thenReturn( new ToolchainPrivate[] { jdkMatching } );
        when( toolchainPrivateManager.getToolchainPrivates( "maven" ) )
                .thenReturn( new ToolchainPrivate[0] );
        assertFalse( SelectorUtils.isToolchain( toolchainPrivateManager, Arrays.asList( openJdk9, maven360 ) ) );
    }

}
