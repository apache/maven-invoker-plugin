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
package org.apache.maven.plugins.invoker;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The job that runs a hook script, bound to the script variable <code>job</code>. It is created once per attempt of a
 * job and shared by the selector, pre-build and post-build scripts of that attempt.
 *
 * @since 3.11.0
 */
public final class ScriptJob {

    private final Path basedir;

    private final Path buildLog;

    private final Path localRepository;

    private final String mavenVersion;

    ScriptJob(Path basedir, Path buildLog, Path localRepository, String mavenVersion) {
        this.basedir = Objects.requireNonNull(basedir, "basedir");
        this.buildLog = buildLog;
        this.localRepository = localRepository;
        this.mavenVersion = mavenVersion;
    }

    /**
     * Gets the base directory of the job, the directory of the <code>basedir</code> variable.
     *
     * @return The base directory, never <code>null</code>.
     */
    public Path getBasedir() {
        return basedir;
    }

    /**
     * Gets the build log of the job, the file of the <code>buildLog</code> variable.
     *
     * @return The build log, or <code>null</code> if logging is disabled by <code>noLog</code>.
     */
    public Path getBuildLog() {
        return buildLog;
    }

    /**
     * Gets the local repository that the job uses.
     *
     * @return The local repository.
     */
    public Path getLocalRepository() {
        return localRepository;
    }

    /**
     * Gets the version of Maven that runs the job, the value of the <code>mavenVersion</code> variable.
     *
     * @return The Maven version.
     */
    public String getMavenVersion() {
        return mavenVersion;
    }

    @Override
    public String toString() {
        return "ScriptJob[basedir=" + basedir + ", buildLog=" + buildLog + ", localRepository=" + localRepository
                + ", mavenVersion=" + mavenVersion + "]";
    }
}
