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

// ensure script context contains localRepositoryPath
assert new File( basedir, "../../../target/it-repo" ).canonicalFile.equals( localRepositoryPath )

File interpolatedSettings = new File( basedir, "../interpolated-settings.xml" )
assert interpolatedSettings.isFile()

def filename = new File( basedir, "../../../../../local-repo" ).canonicalPath
// Convert URL, see org.apache.maven.plugins.invoker.AbstractInvokerMojo.toUrl(String)
String url = "file://" + new File(  filename  ).toURI().path
if ( url.endsWith( "/" ) )
{
    url = url.substring( 0, url.length() - 1 )
}

def settings = new XmlSlurper().parse( interpolatedSettings )

// ensure right settings and mirror are picked up
def sandboxMirror = settings.mirrors.mirror[0]
assert sandboxMirror.id.text() == "sandbox"
assert sandboxMirror.url.text() != "@localRepositoryUrl@"

// sandboxMirror.url is NOT filled with localRepositoryPath, but with the localRepository of the parent Settings
assert sandboxMirror.url.text() == url
