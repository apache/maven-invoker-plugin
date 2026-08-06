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
The Invoker Plugin is used to run a set of Maven projects. The plugin can determine whether each project execution is successful, and optionally can verify the output generated from a given project execution.

This plugin is in particular handy to perform integration tests for other Maven plugins. The Invoker Plugin can be employed to run a set of test projects that have been designed to assert certain features of the plugin under test.

## Goals Overview

The plugin has four goals meant to participate in the default build lifecycle:

- [invoker:install](./install-mojo.html) copies the project artifacts and dependencies of the main build into a dedicated local repository to prepare the execution of the selected sub projects in an isolated environment.
- [invoker:integration-test](./integration-test-mojo.html) runs a set of Maven projects in a directory.
- [invoker:verify](./verify-mojo.html) verifies the result of `invoker:integration-test`.
- [invoker:run](./run-mojo.html) runs a set of Maven projects in a directory and verifies the result. This is equivalent to running both `invoker:integration-test` and `invoker:verify`.

This last goal is intended for usage with the site lifecycle:

- [invoker:report](./report-mojo.html) integrates the results from previous builds into the site.
## Usage

General instructions on how to use the Invoker Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## Examples

The following example configurations are available to illustrate selected use cases in more detail:

- [Clone projects](./examples/clone-projects.html) to a temporary directory before running.
- [Filter files](./examples/filtering.html) to introduce some updates before starting the build.
- [Install](./examples/install-artifacts.html) projects artifacts to a local repository before running.
- [Run a BeanShell or Groovy script](./examples/pre-post-build-script.html) to prepare or verify project.
- [Fast Invoker Plugin configuration](./examples/fast-use.html) to accelerate project execution.
- [Access test classes](./examples/access-test-classes.html) to share code between hook scripts.
- [Use Invoker Properties](./examples/invoker-properties.html) to configure goals, profiles etc. for individual projects.
- [Use Selector Conditions](./examples/selector-conditions.html) to skip projects based on JRE version or OS family.
- [Parallel projects execution](./examples/parallel-projects-execution.html) to speed up project execution.
- [Prepare the Build Environment](./examples/prepare-build-env.html) by building some setup projects before other projects.
- [Using with other integration test frameworks](./examples/integration-test-verify.html) by decoupling checking the results of the integration tests from executing the integration tests.

You can also study some real-life usages of the Invoker Plugin by browsing its own integration tests which are located in the directory `src/it` of the [project source tree](./scm.html).
