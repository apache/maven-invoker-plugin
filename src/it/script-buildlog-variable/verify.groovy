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

// each run: all three hooks saw the build log of the job, and it is where the run puts it
def expected = [
    "its-default": new File(basedir, "target/its-default/project/build.log"),
    "its-logdir" : new File(basedir, "target/logs/project/build.log")
]
expected.each { dir, log ->
    assert log.isFile() : "no build log at ${log}"
    ["selector", "setup", "verify"].each { hook ->
        File seen = new File(basedir, "target/${dir}/project/${hook}.txt")
        assert seen.isFile() : "the ${hook} script did not run in ${dir}"
        String path = seen.getText('UTF-8')
        assert new File(path).canonicalFile == log.canonicalFile : "${hook} script in ${dir} saw ${path}, expected ${log}"
    }
}

return true
