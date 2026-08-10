---
title: Frequently Asked Questions
---

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

<a id="top"></a>

# Frequently Asked Questions

1. [Why do I need to use this plugin?](#question1)
2. [How can I assert that the build of an IT project fails?](#question2)
3. [How can I share common code between the pre-/post-build scripts?](#question3)
4. [How can I invoke multiple Maven builds on the same IT project?](#question4)

<a id="question1"></a>

### Why do I need to use this plugin?

It is essential that you provide some form of integration testing for your projects and the Invoker Plugin
tries to make is easy for you to create integration tests for your Maven Plugins, new Lifecycles, or any other
type of Maven component that you've created. Currently the Invoker Plugin forks Maven to execute the specified
projects, but it is hoped that soon we will integrate the Maven Embedder into the mix to allow you run your
projects in process for great speed.

<a id="question2"></a>

### How can I assert that the build of an IT project fails?

Sometimes you might want to test that error conditions are properly dealt with, i.e. fail a build. To assert a
failure for a particular IT project, put the following setting into the properties file denoted by the
plugin's [`invokerPropertiesFile`](run-mojo.html#invokerPropertiesFile) parameter:
*invoker.buildResult=failure*. Now, the failure of the IT build will be interpreted as a test success.
Likewise, a successful IT build will be considered a test failure.

<a id="question3"></a>

### How can I share common code between the pre-/post-build scripts?

If you want to avoid copy&amp;paste of lengthy code snippets within the hook scripts, you can move this code
into a regular Java class. More precisely, you would place this utility code somewhere in your test source
tree and set the plugin parameter [`addTestClassPath`](run-mojo.html#addTestClassPath) to `true`. For more
details, please see the example [Accessing Test Classes](examples/access-test-classes.html).

<a id="question4"></a>

### How can I invoke multiple Maven builds on the same IT project?

This is not supported in the plugin configuration but rather on a per project basis by means of the invoker
properties. This way, you use properties like `invoker.goals.2` to configure the goals for a second invocation
of Maven. Have a look at the example about using [Invoker Properties](examples/invoker-properties.html).
