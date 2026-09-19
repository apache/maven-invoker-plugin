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

def localRepo = new File(basedir, 'lib/target/local-repo')

assert new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker680-bom/1.0-SNAPSHOT/minvoker680-bom-1.0-SNAPSHOT.pom').exists()
assert new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker680-parent/1.0-SNAPSHOT/minvoker680-parent-1.0-SNAPSHOT.pom').exists()
assert new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker680-lib/1.0-SNAPSHOT/minvoker680-lib-1.0-SNAPSHOT.pom').exists()

// the imported BOM's own parent, and the BOM it imports in turn through an inherited property
assert new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker680-bom-parent/1.0-SNAPSHOT/minvoker680-bom-parent-1.0-SNAPSHOT.pom').exists()
assert new File(localRepo, 'org/apache/maven/plugins/invoker/minvoker680-inner-bom/1.0-SNAPSHOT/minvoker680-inner-bom-1.0-SNAPSHOT.pom').exists()
