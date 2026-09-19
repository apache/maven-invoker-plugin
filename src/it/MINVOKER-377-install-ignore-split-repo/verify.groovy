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

// The outer build enabled a split local repository via test.properties and the forked IT build's
// settings.xml enables it too. invoker:install must write the flat layout regardless, and invoker:run
// must make the forked build read the flat layout regardless (MINVOKER-377).
def localRepo = new File(basedir, 'target/local-repo')

def pom = new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker377/1.0-SNAPSHOT/minvoker377-1.0-SNAPSHOT.pom')
def jar = new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker377/1.0-SNAPSHOT/minvoker377-1.0-SNAPSHOT.jar')

assert pom.isFile()
assert jar.isFile()

def splitDir = new File(localRepo, 'installed')
assert !splitDir.exists()

// the forked build resolved the installed artifact from the flat layout ...
def consumerLog = new File(basedir, 'target/it/consumer/build.log').text
assert consumerLog.contains('BUILD SUCCESS')

// ... and did not read the repository as split, which would have put its own downloads under cached/
assert !new File(localRepo, 'cached').exists()
