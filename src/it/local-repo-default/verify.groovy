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

File badRepoDir = new File( basedir, "target/bad-repo" );
assert !badRepoDir.exists() : "Sub build used wrong local repository (from its settings.xml): " + badRepoDir

/*
 * NOTE: This relies heavily on the Invoker Plugin's configuration of our parent build, not nice but until MINVOKER-49
 * is available here, we have no other way of getting the path.
 */
File localRepoPathOfThisBuild = new File( basedir.getParentFile().getParentFile(), "local-repo" );
System.out.println("Local repository of this build: " + localRepoPathOfThisBuild);

File textFile = new File( basedir, "target/its/project/target/local-repo-path.txt" );
File localRepoPathOfSubBuild = new File( textFile.readLines( 'UTF-8' )[0] );
System.out.println("Local repository of sub build: " + localRepoPathOfSubBuild);

/*
 * This is partly a sanity check of the Maven core (i.e. its MavenSettingsBuilder) which should ensure
 *   ${settings.localRepository} == ${maven.repo.local}
 * This IT is run with maven.repo.local but the Invoker Plugin passes settings.localRepository down to the sub build
 * so these paths must match.
 */
assert localRepoPathOfThisBuild.getCanonicalPath() == localRepoPathOfSubBuild.getCanonicalPath() : "Sub build used wrong local repository: " + localRepoPathOfSubBuild

File installedFile = new File( localRepoPathOfThisBuild, "test/local-repo-default/0.1-SNAPSHOT/local-repo-default-0.1-SNAPSHOT.pom" );
assert installedFile.isFile() : "Installed file missing in local repo: " + installedFile
