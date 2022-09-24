import java.lang.reflect.Array
import java.util.stream.Collectors

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

assert new File( basedir, 'target/it/project/touch.txt' )

File buildLog = new File( basedir, 'build.log' )

String executionString = "Executing:"
int mavenCommandIndex = buildLog.text.indexOf( executionString )
String commandLine = buildLog.text.substring( mavenCommandIndex, buildLog.text.indexOf( System.lineSeparator(),
        mavenCommandIndex ) )
commandLine = commandLine.substring(commandLine.indexOf("mvn"));

assert Arrays.stream( commandLine.split( '\\s' ) )
    .map( f -> f.replaceAll('\'', ''))
    .filter(f -> f == '-U')
    .count() > 0
