<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Invoker Properties

The various parameters in the plugin configuration provide a means to globally configure the goals, profiles etc. used to run a Maven build on the projects. However, for certain projects you might want to specify different settings. To avoid the hassle of multiple plugin executions, you can simply use a file named `invoker.properties` to control the build settings on a per project basis or in one of its ancestor folders to apply it for a group of projects. The exact name of this properties file is configurable but it needs to reside in the base directory of the respective project as shown below:

```unknown
./
+- src/
   +- it/
      +- group-1
         +- invoker.properties
         +- test-project/
            +- pom.xml
            +- invoker.properties
            +- src/
```


## Available Properties

The keys supported in this file typically match the corresponding parameters in the plugin configuration.

The snippet below describes the supported properties:

```properties
# A comma or space separated list of goals/phases to execute, may
# specify an empty list to execute the default goal of the IT project.
# Environment variables used by maven plugins can be added here
invoker.goals = clean install -Dplugin.variable=value

# Or you can give things like this if you need.
invoker.goals.1 = -T2 clean verify

# Optionally, a list of goals to run during further invocations of Maven
invoker.goals.2 = ${project.groupId}:${project.artifactId}:${project.version}:run

# A comma or space separated list of profiles to activate
# can be indexed
invoker.profiles = its,jdk15

# The path to an alternative POM or base directory to invoke Maven on, defaults to the
# project that was originally specified in the plugin configuration
# Since plugin version 1.4
# can be indexed
invoker.project = sub-module

# The maven executable can either be a file relative to ${maven.home}/bin/, test project workspace
# or an absolute file.
# Since plugin version 3.3.0
# can be indexed
invoker.mavenExecutable = mvnw

# The value for the environment variable MAVEN_OPTS
# can be indexed
invoker.mavenOpts = -Dfile.encoding=UTF-16 -Xms32m -Xmx256m

# Possible values are "fail-fast" (default), "fail-at-end" and "fail-never"
# can be indexed
invoker.failureBehavior = fail-never

# The expected result of the build, possible values are "success" (default) and "failure"
# can be indexed
invoker.buildResult = failure

# A boolean value controlling the aggregator mode of Maven, defaults to "false"
# can be indexed
invoker.nonRecursive = true

# A boolean value controlling the network behavior of Maven, defaults to "false"
# Since plugin version 1.4
# can be indexed
invoker.offline = true

# The path to the properties file from which to load user properties, defaults to the
# filename given by the plugin parameter testPropertiesFile
# Since plugin version 1.4
# can be indexed
invoker.userPropertiesFile = test.properties

# An optional human friendly name and description for this build job.
# Both name and description have to be set to be included in the build reports.
# Since plugin version 1.4
invoker.name = Test Build 01
invoker.description = Checks the support for build reports.

# A comma separated list of JRE versions on which this build job should be run.
# Since plugin version 1.4
invoker.java.version = 1.4+, !1.4.1, 1.7-

# A comma separated list of OS families on which this build job should be run.
# Since plugin version 1.4
invoker.os.family = !windows, unix, mac

# A comma separated list of Maven versions on which this build should be run.
# Since plugin version 1.5
invoker.maven.version = 2.0.10+, !2.1.0, !2.2.0

# A mapping for toolchain to ensure it exists
# Since plugin version 3.2.0
invoker.toolchain.<type>.<provides> = value
# Exact match
invoker.toolchain.jdk.version = 11
# Range match
invoker.toolchain.jdk.version = [11,12)

# For java.version, maven.version, os.family and toolchain it is possible to define multiple selectors.
# If one of the indexed selectors matches, the test is executed.
# With the invoker.x.y equivalents you can specify global matchers.
selector.1.java.version = 1.8+
selector.1.maven.version = 3.2.5+
selector.1.os.family = !windows
selector.2.maven.version = 3.0+
selector.3.java.version = 9+

# A boolean value controlling the debug logging level of Maven, defaults to "false"
# Since plugin version 1.8
# can be indexed
invoker.debug = true

# Whether to execute Maven in quiet mode
# Since plugin version 3.3.0
# can be indexed
invoker.quiet = true

# The execution timeout in seconds.
# Since plugin version 3.0.2
# can be indexed
invoker.timeoutInSeconds = 5

# Path to an alternate settings.xml to use for Maven invocation with this IT.
# Since plugin version 3.0.1
# can be indexed
invoker.settingsFile = ../

# An integer value to control run order of projects. sorted in the descending order of the ordinal.
# In other words, the BuildJobs with the highest numbers will be executed first
# Default value is 0 (zero)
# Since plugin version 3.2.1
invoker.ordinal = 3

# The additional value for the environment variable.
# Since plugin version 3.2.2
invoker.environmentVariables.<variableName> = variableValue
invoker.environmentVariables.MY_ENV_NAME = myEnvValue

# A boolean value indicating a check for missing releases and updated snapshots on remote repositories to be done
# Passed to the invoker. Same as passing -U, --update-snapshots flag on the command line
# Since plugin version 3.4.0
invoker.updateSnapshots = true
```

The comments given in the example should be rather self-explanatory. Looking closely, you can also notice that the syntax `${expression}` can be used to filter the property values. What deserves some more description is the possibility to perform several Maven builds on the same project. By default, the Invoker Plugin will perform the following steps for each project:

- Run the pre build hook script if existent
- Invoke Maven in the project directory
- Run the post build hook script if existent

Since plugin version 1.3, you can append a one-based index to the invoker properties in order to enable/configure further invocations of Maven. More precisely, `invoker.goals.1` specifies the goals for the first build, `invoker.goals.2` lists the goals for the second build and so on. These builds will be performed one after the other:

- Run the pre build hook script if existent
- Invoke Maven in the project directory
- Invoke Maven in the project directory
- ...
- Invoke Maven in the project directory
- Run the post build hook script if existent

Most of the properties can be indexed this way, e.g. `invoker.profiles.3` would specify the profiles to use for the third invocation. If the property `invoker.profiles.3` was not defined, the plugin would query the property `invoker.profiles` as a fallback to determine the profiles for the third build. This build loop ends after invocation _i_ if no property `invoker.*.`_i+1_ is defined.

The invoker properties can also be used to skip projects based on the current JRE version or OS family. For more information on this feature, please see [Selector Conditions](./selector-conditions.html).
