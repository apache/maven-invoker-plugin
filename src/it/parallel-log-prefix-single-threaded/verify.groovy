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

// MINVOKER-684 control case: with the default parallelThreads=1, output must stay byte-identical to
// before the fix, so none of the per-job lines get a "[<project>] " prefix.
assert buildLog.contains('Building: ' + alphaProject)
assert buildLog.contains('Building: ' + betaProject)
assert buildLog.contains('Running pre-build script')
assert buildLog.contains('Output from setup script for alpha')
assert buildLog.contains('Output from setup script for beta')

assert !buildLog.contains('[' + alphaProject + ']')
assert !buildLog.contains('[' + betaProject + ']')
