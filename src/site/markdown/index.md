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

# Apache Maven Invoker Plugin

The Invoker Plugin runs a set of Maven projects and checks the result of each build. Its main use is integration
testing of Maven plugins and extensions: each test is a small Maven project under `src/it` that exercises the plugin
under test, and an optional Groovy script checks what the build produced.

The plugin requires Maven 3.9.0 or later and Java 8 or later.

Most users come to the plugin for these tasks:

- Run all integration tests in `src/it` as part of the build:

    ```shell
    mvn verify
    ```

- Rerun one or a few integration tests:

    ```shell
    mvn verify -Dinvoker.test=first-it,second-*
    ```

- Check the result of a build with a [post-build Groovy script](./examples/pre-post-build-script.html), and
  configure each test with an [`invoker.properties`](./examples/invoker-properties.html) file.
- Skip the integration tests:

    ```shell
    mvn install -Dinvoker.skip=true
    ```

The [usage page](./usage.html) shows the plugin configuration these commands rely on.

## Goals Overview

The plugin has four goals that take part in the default build lifecycle:

- [invoker:install](./install-mojo.html) copies the project artifacts and dependencies of the main build into a dedicated local repository to prepare the execution of the selected sub projects in an isolated environment.
- [invoker:integration-test](./integration-test-mojo.html) runs a set of Maven projects in a directory.
- [invoker:verify](./verify-mojo.html) verifies the result of `invoker:integration-test`.
- [invoker:run](./run-mojo.html) runs a set of Maven projects in a directory and verifies the result. This is equivalent to running both `invoker:integration-test` and `invoker:verify`.

One goal is meant for the site lifecycle:

- [invoker:report](./report-mojo.html) integrates the results from previous builds into the site.

[invoker:help](./help-mojo.html) displays help information on the plugin's goals and parameters.

## Usage

General instructions on how to use the Invoker Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

If you still have questions about the plugin's usage, see the [FAQ](./faq.html) and ask on the [user mailing list](./mailing-lists.html), whose archive may already answer your question.

If the plugin is missing a feature or has a defect, open an issue in our [issue tracker](./issue-management.html). For a bug, include a debug log (`mvn -X`) and, ideally, a small project that reproduces the problem. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## Examples

The following example configurations are available to illustrate selected use cases in more detail:

- [Clone projects](./examples/clone-projects.html) to a temporary directory before running.
- [Debug tests](./examples/debugging.html) with a remote JVM debugger (`mvnDebug`).
- [Filter files](./examples/filtering.html) to introduce some updates before starting the build.
- [Install](./examples/install-artifacts.html) projects artifacts to a local repository before running.
- [Run a Groovy script](./examples/pre-post-build-script.html) to prepare or verify project (BeanShell scripts are deprecated).
- [Fast Invoker Plugin configuration](./examples/fast-use.html) to accelerate project execution.
- [Access test classes](./examples/access-test-classes.html) to share code between hook scripts.
- [Use Invoker Properties](./examples/invoker-properties.html) to configure goals, profiles etc. for individual projects.
- [Use Selector Conditions](./examples/selector-conditions.html) to skip projects based on the Java version, Maven version, OS family or available toolchains.
- [Use Selector Scripts](./examples/selector-scripts.html) to skip projects based on a Groovy script.
- [Parallel projects execution](./examples/parallel-projects-execution.html) to speed up project execution.
- [Prepare the Build Environment](./examples/prepare-build-env.html) by building some setup projects before other projects.
- [Skip invocations](./examples/skipping.html) or the installation step.
- [Inspect logs for failed tests](./examples/logs-for-failed-tests.html) in the main build output.
- [Using with other integration test frameworks](./examples/integration-test-verify.html) by decoupling checking the results of the integration tests from executing the integration tests.

You can also study some real-life usages of the Invoker Plugin by browsing its own integration tests which are located in the directory `src/it` of the [project source tree](./scm.html).
