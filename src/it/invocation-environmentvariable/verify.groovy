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

def buildLog = new File( basedir, 'build.log' ).text
assert buildLog.contains('[INFO] BUILD SUCCESS')

def buildLod273 = new File( basedir, 'src/it/minvoker-273/build.log' ).text

// MINVOKER273_PROPERTIES1 is present in system environment and should be removed
assert !buildLod273.contains('MINVOKER273_PROPERTIES1')

// MINVOKER273_PROPERTIES2 is present in system environment and should be present in invocation
assert buildLod273.contains('MINVOKER273_PROPERTIES2=minvoker273_properties2')

// MINVOKER273_PROPERTIES3 is not present in system environment and should not be present in invocation
assert !buildLod273.contains('MINVOKER273_PROPERTIES3')

// MINVOKER-372: MAVEN_ARGS=-Dleaked.arg=true is set on this outer build's own environment (see
// invoker.properties) but must not leak into the forked invocation of minvoker-372; if it did, the
// "leaked" profile would activate and fail that build.
def buildLog372 = new File( basedir, 'src/it/minvoker-372/build.log' ).text
assert buildLog372.contains('] BUILD SUCCESS')

// MINVOKER-172: every forked invocation gets MAVEN_SKIP_RC=1 so ~/.mavenrc/etc cannot override MAVEN_OPTS.
def buildLog172 = new File( basedir, 'src/it/minvoker-172/build.log' ).text
assert buildLog172.contains('] BUILD SUCCESS')

