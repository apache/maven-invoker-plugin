/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import groovy.xml.XmlSlurper

File invokerReports = new File( new File( basedir, 'target' ), 'invoker-reports-test' )
assert invokerReports.exists()

// the build failed on the goal named by invoker.failedGoal
def expected = new XmlSlurper().parse( new File( invokerReports, 'BUILD-expected-goal.xml' ) )

assert expected.@result.text() == 'success'

// the build failed, but on another goal than invoker.failedGoal named
def unexpected = new XmlSlurper().parse( new File( invokerReports, 'BUILD-unexpected-goal.xml' ) )

assert unexpected.@result.text() == 'failure-build'
assert unexpected.failureMessage.text().contains( 'maven-clean-plugin:clean' )
assert unexpected.failureMessage.text().contains( 'maven-enforcer-plugin' )

def buildLog = new File( basedir, 'build.log' ).text
assert buildLog.contains( '[INFO]   Passed: 1, Failed: 1, Errors: 0, Skipped: 0' )
