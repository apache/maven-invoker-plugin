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

File reportFile = new File(basedir, "target/site/invoker.html")

System.out.println("Checking for existence of report file: " + reportFile)
assert reportFile.exists() : "Report file missing: " + reportFile

allWarningLines = new File( basedir, "build.log" ).readLines().findAll {
    it.contains( "[WARNING] Incomplete job name-description" )
}

missingNameLinesCount = allWarningLines.count {
    it.contains( "name is missing" )
}
missingDescriptionLinesCount = allWarningLines.count {
    it.contains( "description is missing" )
}

expectedMissingNames = 4
assert expectedMissingNames == missingNameLinesCount : "Current missingNameLinesCount: " + missingNameLinesCount + " but expecting: " + expectedMissingNames

expectedMissingDescriptions = 1
assert expectedMissingDescriptions == missingDescriptionLinesCount : "Current missingDescriptionLinesCount: " + missingDescriptionLinesCount + " but expecting: " + expectedMissingDescriptions
