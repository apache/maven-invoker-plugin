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

File touchFile;

touchFile = new File( basedir, "target/its/beanshell/touch.txt" );
System.out.println( "Checking for existence of touch file: " + touchFile );
if ( !touchFile.exists() )
{
    throw new FileNotFoundException( "Did not find marker file: " + touchFile );
}

touchFile = new File( basedir, "target/its/groovy/touch.txt" );
System.out.println( "Checking for existence of touch file: " + touchFile );
if ( !touchFile.exists() )
{
    throw new FileNotFoundException( "Did not find marker file: " + touchFile );
}

// the outer build log carries the deprecation warning for the BeanShell scripts, once per file, and none for Groovy
File buildLog = new File( basedir, "build.log" )
String log = buildLog.text
['setup.bsh', 'verify.bsh'].each { name ->
    File script = new File( basedir, "target/its/beanshell/" + name )
    assert log.count( "BeanShell scripts are deprecated, port " + script + " to Groovy" ) == 1 : "expected one warning for " + script
}
assert !log.contains( "target/its/groovy/setup.groovy to Groovy" ) && !log.contains( "target/its/groovy/verify.groovy to Groovy" )

return true;
