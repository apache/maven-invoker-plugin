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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.codehaus.plexus.util.*;
import org.codehaus.plexus.util.xml.*;

try
{
    File itRepoDir = new File( basedir, "target/it-repo" );
    System.out.println( "Checking for existence of: " + itRepoDir );
    if ( !itRepoDir.isDirectory() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String[] files = {
            "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
            "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.pom",
            "org/slf4j/slf4j-api/maven-metadata-local.xml",
            "org/slf4j/slf4j-parent/1.7.36/slf4j-parent-1.7.36.pom",
            "org/slf4j/slf4j-parent/maven-metadata-local.xml",
            "org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar",
            "org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.pom",
            "org/slf4j/slf4j-simple/maven-metadata-local.xml",
            "test/staging-dependencies-test-scope/1.0-SNAPSHOT/staging-dependencies-test-scope-1.0-SNAPSHOT.pom",
            "test/staging-dependencies-test-scope/1.0-SNAPSHOT/staging-dependencies-test-scope-1.0-SNAPSHOT.jar"
      };
    for ( String file : files )
    {
        File stagedFile = new File( itRepoDir, file );
        System.out.println( "Checking for existence of: " + stagedFile );
        if ( !stagedFile.isFile() )
        {
            throw new IllegalStateException( "Missing: " + stagedFile );
        }
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
