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

def FS = System.getProperty('file.separator')

assert new File(basedir, 'target/its/jre-version-match/build.log').exists()
assert !(new File(basedir, 'target/its/jre-version-mismatch/build.log').exists())
assert new File(basedir, 'target/its/maven-version-match/build.log').exists()
assert !(new File(basedir, 'target/its/maven-version-mismatch/build.log').exists())
assert new File(basedir, 'target/its/os-family-match/build.log').exists()
assert !(new File(basedir, 'target/its/os-family-mismatch/build.log').exists())
assert !(new File(basedir, 'target/its/toolchain-mismatch/build.log').exists())

def log = new File( basedir, 'build.log').text

assert log.contains("jre-version-match${FS}pom.xml ........................ SUCCESS")
assert log.contains("jre-version-mismatch${FS}pom.xml ..................... SKIPPED due to JRE version")
assert log.contains("maven-version-match${FS}pom.xml ...................... SUCCESS")
assert log.contains("maven-version-mismatch${FS}pom.xml ................... SKIPPED due to Maven version")
assert log.contains("os-family-match${FS}pom.xml .......................... SUCCESS")
assert log.contains("os-family-mismatch${FS}pom.xml ....................... SKIPPED due to OS")
assert log.contains("toolchain-mismatch${FS}pom.xml ....................... SKIPPED due to Toolchain")