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


def expected = [
    'target/it/project/prebuild.txt',
    'target/it/project/prebuild.1.txt',
    'target/it/project/prebuild.2.txt',
    'target/it/project/postbuild.txt',
    'target/it/project/postbuild.1.txt',
    'target/it/project/postbuild.2.txt',
]

def missingFiles = expected.findAll { !new File(basedir, it).isFile() }

assert missingFiles == []

def log = new File(basedir, 'build.log').text

assert log.contains('[INFO] run pre-build script prebuild.groovy')
assert log.contains('[INFO] run pre-build script prebuild.1.groovy')
assert log.contains('[INFO] run pre-build script prebuild.2.groovy')

assert log.contains('[INFO] run post-build script postbuild.groovy')
assert log.contains('[INFO] run post-build script postbuild.1.groovy')
assert log.contains('[INFO] run post-build script postbuild.2.groovy')

assert log.contains('prebuild=1')
assert log.contains('prebuild=2')
