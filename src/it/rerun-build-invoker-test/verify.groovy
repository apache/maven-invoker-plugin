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

// both invocations of this IT write to build.log, the second one starts with the header of this project
// (the invoker renames build.log only when it reruns a failed job, so the invocations of invoker.goals.N share one file)
def buildLog = new File(basedir, 'build.log').text
String header = '[INFO] Building rerun-build-invoker-test 1.0-SNAPSHOT'
int second = buildLog.indexOf(header, buildLog.indexOf(header) + 1)
assert second > 0 : 'build.log has only one invocation'

// invoker.test given as user property: the selected project was executed only once - no rerun
def userPropertyLog = buildLog.substring(0, second)
assert userPropertyLog.contains('Rerunning of failed jobs is disabled when the invoker.test user property is used')
assert userPropertyLog.count('FAILED') == 1
assert !userPropertyLog.contains('SUCCESS ')
assert !userPropertyLog.contains('Rerunning 1 failed job')
assert !new File(basedir, 'target/it-user-property/project/build.log.1').exists()

// invokerTest only in the plugin configuration: selecting a project doesn't disable rerunning
def configLog = buildLog.substring(second)
assert !configLog.contains('Rerunning of failed jobs is disabled')
assert configLog.count('FAILED') == 1
assert configLog.count('SUCCESS (') == 1
assert configLog.contains('Rerunning 1 failed job')
assert new File(basedir, 'target/it-config/project/build.log.1').exists()
