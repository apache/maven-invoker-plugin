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
def alphaProject = 'alpha' + FS + 'pom.xml'
def betaProject = 'beta' + FS + 'pom.xml'

// make sure both sub builds were indeed run
assert new File(basedir, 'target/its/alpha/touch.txt').exists()
assert new File(basedir, 'target/its/beta/touch.txt').exists()

def buildLog = new File(basedir, 'build.log').text
def lines = buildLog.readLines()

// MINVOKER-684: with parallelThreads > 1, every console line emitted on behalf of a job is prefixed with
// the job's project path, so interleaved output from several jobs stays attributable.

// the "Building: <project>" line is prefixed
assert lines.any { it.contains('[' + alphaProject + '] Building: ' + alphaProject) }
assert lines.any { it.contains('[' + betaProject + '] Building: ' + betaProject) }

// script-run lines reaching the console via streamLogs are prefixed
assert lines.any { it.contains('[' + alphaProject + '] Running pre-build script') }
assert lines.any { it.contains('[' + betaProject + '] Running pre-build script') }

// streamed script output lines are prefixed
assert lines.any { it.contains('[' + alphaProject + '] Output from setup script for alpha') }
assert lines.any { it.contains('[' + betaProject + '] Output from setup script for beta') }

// the result summary line already carries the project name via the padded column, so it must not get a
// second, bracketed copy of it; "SUCCESS (" is unique to that summary line (as opposed to the streamed
// "BUILD SUCCESS" output of the nested Maven invocation, which has no trailing elapsed time)
def alphaResultLine = lines.find { it.contains(alphaProject) && it.contains('SUCCESS (') }
assert alphaResultLine != null
assert !alphaResultLine.contains('[' + alphaProject + ']')

def betaResultLine = lines.find { it.contains(betaProject) && it.contains('SUCCESS (') }
assert betaResultLine != null
assert !betaResultLine.contains('[' + betaProject + ']')

// the prefix is a console-only concern: the per-job build.log file content must stay untouched
def alphaLog = new File(basedir, 'target/its/alpha/build.log').text
def betaLog = new File(basedir, 'target/its/beta/build.log').text

assert !alphaLog.contains('[' + alphaProject + ']')
assert !betaLog.contains('[' + betaProject + ']')
assert alphaLog.contains('Running pre-build script')
assert betaLog.contains('Running pre-build script')
