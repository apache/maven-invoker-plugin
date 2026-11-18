<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven Invoker Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-invoker-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-invoker-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-invoker-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-invoker-plugin/README.md)
[![Verify](https://github.com/apache/maven-invoker-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-invoker-plugin/actions/workflows/maven-verify.yml)

The Invoker Plugin runs a set of Maven projects and checks the result of each build. Maven plugins use it for their
integration tests: each test is a small Maven project under `src/it`, and an optional Groovy script checks what the
build produced. The plugin requires Maven 3.9.0 or later and Java 8 or later.

- Documentation: <https://maven.apache.org/plugins/maven-invoker-plugin/>
- Issues: <https://github.com/apache/maven-invoker-plugin/issues>
- Mailing lists: <https://maven.apache.org/mailing-lists.html>

## Build

Build the plugin and run the unit tests:

```shell
mvn verify
```

Also run the integration tests in `src/it`, which use this plugin to test itself:

```shell
mvn -Prun-its verify
```

Run one integration test, for example `selector-scripts`:

```shell
mvn -Prun-its verify -Dinvoker.test=selector-scripts
```

The output of each integration test is in `target/it/<name>/build.log`.

## Contributing

Open an issue or a pull request on GitHub. Format the code with `mvn spotless:apply` before you push, and add a unit
test or an integration test for a change in behavior. Write new integration test scripts in Groovy; BeanShell scripts
are deprecated. See the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html)
for the wider contribution process.
