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

# Selector Conditions

Starting with plugin version 1.4, some keys of the [Invoker Properties](./invoker-properties.html) can be used to skip individual projects based on the current Java version or OS family as illustrated in the example below:

```properties
# build project if Java version is 17 or higher, but exclude version 21.0.1
invoker.java.version = 17+, !21.0.1

# build project if OS family is not Windows
invoker.os.family = !windows
```

In addition, with plugin version 1.5, there is the ability to skip individual projects based on the current Maven version as illustrated in the example below:

```properties
# build project if Maven version is 3.9.0 or higher, but exclude version 3.9.2
invoker.maven.version = 3.9.0+, !3.9.2
```

The Java version is the one of the JVM that runs the test builds: the JVM running Maven, or the one given by the
[`javaHome`](../run-mojo.html#javaHome) parameter. The Maven version is the one of the Maven installation that runs
the test builds.

The values of these keys are comma separated tokens. A token prefixed with "\!" denotes an exclusion, otherwise it denotes an inclusion. A project is build if no exclusion matches and if no inclusions are given or at least one of them matches the current environment.

The tokens to describe versions can be suffixed with "+" or "-". The table below shows how the different styles of version tokens translate to a version range:

|Version Token|Equivalent Version Range|
|:---|:---|
|17|\[17,18)|
|17+|\[17,)|
|17-|(,17)|
|3\.9|\[3.9,3.10)|

For the OS family, use tokens such as "windows", "unix" and "mac".

A project can also require a toolchain configured in `toolchains.xml`, for example a JDK 17 toolchain:

```properties
invoker.toolchain.jdk.version = 17
```

To run a project if any one of several combinations matches, use indexed selectors such as `selector.1.java.version`;
see [Invoker Properties](./invoker-properties.html).
