import java.nio.file.Files

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


assert new File(basedir, 'target/it/setup/touch.txt').exists()
assert new File(basedir, 'target/it/project/touch.txt').exists()

def buildLog = new File(basedir, 'build.log').text

def fs = File.separator

assert buildLog.count("setup${fs}pom.xml .................................... FAILED") == 1
assert buildLog.count("setup${fs}pom.xml .................................... SUCCESS") == 1

assert buildLog.count("project${fs}pom.xml .................................. FAILED") == 1
assert buildLog.count("project${fs}pom.xml .................................. SUCCESS") == 1

def setupBuildLog1 = new File(basedir, 'target/it/setup/build.log.1').text
assert setupBuildLog1.count("[INFO] BUILD SUCCESS") == 1

def setupBuildLog = new File(basedir, 'target/it/setup/build.log').text
assert setupBuildLog.count("[INFO] BUILD SUCCESS") == 1

def projectBuildLog1 = new File(basedir, 'target/it/project/build.log.1').text
assert projectBuildLog1.count("[INFO] BUILD SUCCESS") == 1

def projectBuildLog = new File(basedir, 'target/it/project/build.log').text
assert projectBuildLog.count("[INFO] BUILD SUCCESS") == 1
