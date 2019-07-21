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
File buildLog = new File( basedir, 'build.log' )
assert buildLog.text.contains( '[INFO] run post-build script verify.groovy' )

File invokerReports = new File( new File(basedir, "target"), 'invoker-reports' )
assert invokerReports.exists()

// test on first project
def testsuite = new XmlSlurper().parse( new File( invokerReports, "TEST-project.xml" ) )

assert testsuite.@name.text() != null
assert testsuite.@time.text() != null
assert testsuite.@tests.text() == "1"
assert testsuite.@errors.text() == "0"
assert testsuite.@skipped.text() == "0"
assert testsuite.@failures.text() == "0"

assert testsuite.testcase.@name.text() == "project"
def systemOut = testsuite.testcase.'**'.findAll { node -> node.name() == 'system-out' }.get(0)
assert !systemOut.text().isEmpty()


// test on second project
testsuite = new XmlSlurper().parse( new File( invokerReports, "TEST-project_2.xml" ) )

assert testsuite.@name.text() != null
assert testsuite.@time.text() != null
assert testsuite.@tests.text() == "1"
assert testsuite.@errors.text() == "0"
assert testsuite.@skipped.text() == "0"
assert testsuite.@failures.text() == "1"

assert testsuite.testcase.@name.text() == "project_2"
def failureMessage = testsuite.testcase.failure.@message
assert !failureMessage.text().isEmpty()
