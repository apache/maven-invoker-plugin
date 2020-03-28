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

def FS = File.separator
def buildLogOfProject = new File( basedir, 'target/it/project/build.log' ).text

def buildLog = new File( basedir, 'build.log' ).text

assert buildLog.contains( '*** begin build.log for: project' + FS + 'pom.xml ***' )
assert buildLog.contains( buildLogOfProject )
assert buildLog.contains( '*** end build.log for: project' + FS + 'pom.xml ***' )

assert buildLog.contains( 'maven-invoker-plugin:' + projectVersion + ':integration-test' )
assert buildLog.contains( '[ERROR] Failed to execute goal org.apache.maven.plugins:maven-invoker-plugin:' + projectVersion + ':verify' )

