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

// both invocations of this IT write to build.log, the second one starts with the header of this project:
// the first has streamLogs=false, the second streamLogs=true
def buildLog = new File(basedir, 'build.log').text
String header = '[INFO] Building script-streamLogs 1.0-SNAPSHOT'
int second = buildLog.indexOf(header, buildLog.indexOf(header) + 1)
assert second > 0 : 'build.log has only one invocation'

// the build logs of the projects are in the clone directory of the respective invocation
for ( String itDir : [ 'its-stream-false', 'its-stream-true' ] ) {
    // make sure the Invoker Plugin was indeed run and the build didn't fail somewhere else,
    // so check if touch.txt exist
    assert new File(basedir, "target/${itDir}/beanshell/touch.txt").exists()
    assert new File(basedir, "target/${itDir}/groovy/touch.txt").exists()

    def beanshellLog = new File(basedir, "target/${itDir}/beanshell/build.log").text
    assert beanshellLog.contains('Output form beanshell script')
    assert beanshellLog.contains('java.lang.OutOfMemoryError: Requested array size exceeds VM limit')
    def groovyLog = new File(basedir, "target/${itDir}/groovy/build.log").text
    assert groovyLog.contains('Output from groovy script')
    assert groovyLog.contains('Assertion failed:')
    assert groovyLog.contains('assert pom.contains("<modelVersion>9.9.9</modelVersion>")')
}

// streamLogs=false: failed scripts show no output in the build log
def notStreamed = buildLog.substring(0, second)
// beanshell failed and no log message
assert !notStreamed.contains('[INFO] Output form beanshell script')
assert notStreamed.contains('[INFO]   org.apache.maven.shared.scriptinterpreter.ScriptEvaluationException: java.lang.OutOfMemoryError: Requested array size exceeds VM limit')
assert notStreamed.contains('[INFO]           beanshell' + FS + 'pom.xml ................................ FAILED')
// groovy failed and no log message
assert !notStreamed.contains('[INFO] Output from groovy script')
assert notStreamed.contains('Assertion failed:')
assert notStreamed.contains('assert pom.contains("<modelVersion>9.9.9</modelVersion>")')
assert notStreamed.contains('[INFO]           groovy' + FS + 'pom.xml ................................... FAILED')

// streamLogs=true: the output of the failed scripts is in the build log
def streamed = buildLog.substring(second)
// beanshell failed and log message
assert streamed.contains('[INFO] Output form beanshell script')
assert streamed.contains('java.lang.OutOfMemoryError: Requested array size exceeds VM limit')
assert streamed.contains('[INFO]           beanshell' + FS + 'pom.xml ................................ FAILED')
// groovy failed and log message
assert streamed.contains('[INFO] Output from groovy script')
assert streamed.contains('Assertion failed:')
assert streamed.contains('assert pom.contains("<modelVersion>9.9.9</modelVersion>")')
assert streamed.contains('[INFO]           groovy' + FS + 'pom.xml ................................... FAILED')
