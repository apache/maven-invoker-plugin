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

import java.security.MessageDigest
import java.util.jar.JarOutputStream

def artifactPath = 'repository/org/apache/maven/plugins/invoker/MINVOKER-680-bom-dependent/1.0/MINVOKER-680-bom-dependent-1.0.jar'
def artifact = new File(basedir, artifactPath)
artifact.parentFile.mkdirs()
new JarOutputStream(new FileOutputStream(artifact)).close()

new File(basedir, 'repository').eachFileRecurse { file ->
  if (file.isFile() && !(file.name.endsWith('.sha1') || file.name.endsWith('.md5'))) {
    ['SHA-1': 'sha1', 'MD5': 'md5'].each { algorithm, extension ->
      def checksum = MessageDigest.getInstance(algorithm).digest(file.bytes).encodeHex().toString()
      new File(file.path + '.' + extension).text = checksum
    }
  }
}
