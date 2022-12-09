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
import groovy.xml.XmlSlurper

File invokerReports = new File( new File(basedir, "target"), 'invoker-reports-test' )
assert invokerReports.exists()

def build1 = new XmlSlurper().parse( new File( invokerReports, "BUILD-project1.xml" ) )

assert build1.@result.text() == "failure-build"
assert build1.@type.text() == "setup"

def build2 = new XmlSlurper().parse( new File( invokerReports, "BUILD-project2.xml" ) )

assert build2.@result.text() == "skipped"
assert build2.failureMessage.text() == "Skipped due to setup job(s) failure"

def testsuite = new XmlSlurper().parse( new File( invokerReports, "TEST-project2.xml" ) )

assert testsuite.@skipped.text() == "1"
assert testsuite.testcase.skipped.text() == "Skipped due to setup job(s) failure"

def buildLog = new File( basedir, 'build.log' ).text
assert buildLog.contains('[INFO]   Passed: 0, Failed: 1, Errors: 0, Skipped: 1')
