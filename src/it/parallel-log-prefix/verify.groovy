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

def lines = new File(basedir, 'build.log').readLines()

// MINVOKER-684: every console line emitted on behalf of a job is prefixed with the job's project path, so
// output from several jobs stays attributable when it interleaves. The format does not depend on
// parallelThreads.

// start and result lines
assert lines.any { it.contains('[' + alphaProject + '] starting') }
assert lines.any { it.contains('[' + betaProject + '] starting') }
assert lines.any { it.contains('[' + alphaProject + '] SUCCESS (') }
assert lines.any { it.contains('[' + betaProject + '] SUCCESS (') }

// the project name appears once per line, as the prefix
assert lines.findAll { it.contains('] starting') }.every { it.count(alphaProject) + it.count(betaProject) == 1 }

// script-run lines reaching the console via streamLogs are prefixed
assert lines.any { it.contains('[' + alphaProject + '] Running pre-build script') }
assert lines.any { it.contains('[' + betaProject + '] Running pre-build script') }

// streamed script output lines are prefixed
assert lines.any { it.contains('[' + alphaProject + '] Output from setup script for alpha') }
assert lines.any { it.contains('[' + betaProject + '] Output from setup script for beta') }

// the prefix is a console-only concern: the per-job build.log file content must stay untouched
def alphaLog = new File(basedir, 'target/its/alpha/build.log').text
def betaLog = new File(basedir, 'target/its/beta/build.log').text

assert !alphaLog.contains('[' + alphaProject + ']')
assert !betaLog.contains('[' + betaProject + ']')
assert alphaLog.contains('Running pre-build script')
assert betaLog.contains('Running pre-build script')
