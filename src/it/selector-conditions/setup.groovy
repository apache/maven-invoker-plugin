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

def javaHome = System.getProperty('java.home')
def javaVersion = System.getProperty('java.version')
def toolchains = """
<toolchains xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd" 
    xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <toolchain>
    <type>jdk</type>
    <provides>
      <vendor>myTestJdk</vendor>
      <version>${javaVersion}</version>
    </provides>
    <configuration>
      <jdkHome>${javaHome}</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
"""

// generate toolchains.xml with the current JDK for the test of toolchain selector conditions
new File(basedir, 'toolchains.xml').text = toolchains

return true
