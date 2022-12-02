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

// make sure the Invoker Plugin was indeed run and the build didn't fail somewhere else,
// so check if touch.txt exist

assert new File(basedir, 'target/its/beanshell/touch.txt').exists()
assert new File(basedir, 'target/its/groovy/touch.txt').exists()

// logs
def buildLog = new File(basedir, 'build.log').text
def beanshellLog = new File(basedir, 'target/its/beanshell/build.log').text
def groovyLog = new File(basedir, 'target/its/groovy/build.log').text

// beanshell failed and no log message
assert !buildLog.contains('[INFO] Output form beanshell script')
assert buildLog.contains('[INFO]   org.apache.maven.shared.scriptinterpreter.ScriptEvaluationException: java.lang.OutOfMemoryError: Requested array size exceeds VM limit')
assert buildLog.contains('[INFO]           beanshell' + FS + 'pom.xml ................................ FAILED')

assert beanshellLog.contains('Output form beanshell script')
assert beanshellLog.contains('java.lang.OutOfMemoryError: Requested array size exceeds VM limit')

// groovy failed and no log message
assert !buildLog.contains('[INFO] Output from groovy script')
assert buildLog.contains('Assertion failed:')
assert buildLog.contains('assert pom.contains("<modelVersion>9.9.9</modelVersion>")')
assert buildLog.contains('[INFO]           groovy' + FS + 'pom.xml ................................... FAILED')

assert groovyLog.contains('Output from groovy script')
assert groovyLog.contains('Assertion failed:')
assert groovyLog.contains('assert pom.contains("<modelVersion>9.9.9</modelVersion>")')
