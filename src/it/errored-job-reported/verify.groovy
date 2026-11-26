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

// the timeout of the cloned project is not a number, so the job ends with an error and not a build failure
def report = new XmlSlurper().parse(new File(basedir, 'target/invoker-reports-test/BUILD-project.xml'))
assert report.@result.text() == 'error'
assert report.failureMessage.text() != ''

def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Errors: 1')
