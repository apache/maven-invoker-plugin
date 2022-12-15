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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.shared.utils.io.IOUtil;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Tracks a set of build jobs and their results.
 *
 * @author Benjamin Bentmann
 */
class InvokerSession {
    private static final String SEPARATOR =
            buffer().strong("-------------------------------------------------").toString();

    private List<BuildJob> buildJobs;

    private List<BuildJob> failedJobs;

    private List<BuildJob> errorJobs;

    private List<BuildJob> successfulJobs;

    private List<BuildJob> skippedJobs;

    /**
     * Creates a new empty session.
     */
    InvokerSession() {
        buildJobs = new ArrayList<>();
    }

    /**
     * Creates a session that initially contains the specified build jobs.
     *
     * @param buildJobs The build jobs to set, must not be <code>null</code>.
     */
    InvokerSession(List<BuildJob> buildJobs) {
        this.buildJobs = new ArrayList<>(buildJobs);
    }

    /**
     * Adds the specified build job to this session.
     *
     * @param buildJob The build job to add, must not be <code>null</code>.
     */
    public void addJob(BuildJob buildJob) {
        buildJobs.add(buildJob);

        resetStats();
    }

    /**
     * Sets the build jobs of this session.
     *
     * @param buildJobs The build jobs to set, must not be <code>null</code>.
     */
    public void setJobs(List<? extends BuildJob> buildJobs) {
        this.buildJobs = new ArrayList<>(buildJobs);

        resetStats();
    }

    /**
     * Gets the build jobs in this session.
     *
     * @return The build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getJobs() {
        return buildJobs;
    }

    /**
     * Gets the successful build jobs in this session.
     *
     * @return The successful build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getSuccessfulJobs() {
        updateStats();

        return successfulJobs;
    }

    /**
     * Gets the failed build jobs in this session.
     *
     * @return The failed build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getFailedJobs() {
        updateStats();

        return failedJobs;
    }

    /**
     * Gets the build jobs which had errors for this session.
     *
     * @return The build jobs in error for this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getErrorJobs() {
        updateStats();

        return errorJobs;
    }

    /**
     * Gets the skipped build jobs in this session.
     *
     * @return The skipped build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getSkippedJobs() {
        updateStats();

        return skippedJobs;
    }

    private void resetStats() {
        successfulJobs = null;
        failedJobs = null;
        skippedJobs = null;
        errorJobs = null;
    }

    private void updateStats() {
        if (successfulJobs != null && skippedJobs != null && failedJobs != null && errorJobs != null) {
            return;
        }

        successfulJobs = new ArrayList<>();
        failedJobs = new ArrayList<>();
        skippedJobs = new ArrayList<>();
        errorJobs = new ArrayList<>();

        for (BuildJob buildJob : buildJobs) {
            if (BuildJob.Result.SUCCESS.equals(buildJob.getResult())) {
                successfulJobs.add(buildJob);
            } else if (BuildJob.Result.SKIPPED.equals(buildJob.getResult())) {
                skippedJobs.add(buildJob);
            } else if (BuildJob.Result.ERROR.equals(buildJob.getResult())) {
                errorJobs.add(buildJob);
            } else if (buildJob.getResult() != null) {
                failedJobs.add(buildJob);
            }
        }
    }

    /**
     * Prints a summary of this session to the specified logger.
     *
     * @param logger The mojo logger to output messages to, must not be <code>null</code>.
     * @param ignoreFailures A flag whether failures should be ignored or whether a build failure should be signaled.
     */
    public void logSummary(Log logger, boolean ignoreFailures) {
        updateStats();

        logger.info(SEPARATOR);
        logger.info("Build Summary:");
        logger.info("  Passed: " + successfulJobs.size()
                + ", Failed: " + failedJobs.size()
                + ", Errors: " + errorJobs.size()
                + ", Skipped: " + skippedJobs.size());
        logger.info(SEPARATOR);

        logBuildJobList(logger, ignoreFailures, "The following builds failed:", failedJobs);
        logBuildJobList(logger, ignoreFailures, "The following builds finished with error:", errorJobs);
        logBuildJobList(logger, true, "The following builds were skipped:", skippedJobs);
    }

    public void logFailedBuildLog(Log logger, boolean ignoreFailures) throws MojoFailureException {
        updateStats();

        List<BuildJob> jobToLogs = new ArrayList<>(failedJobs);
        jobToLogs.addAll(errorJobs);

        for (BuildJob buildJob : jobToLogs) {
            File buildLogFile = buildJob.getBuildlog() != null ? new File(buildJob.getBuildlog()) : null;
            if (buildLogFile != null && buildLogFile.exists()) {
                try {
                    // prepare message with build.log in one string to omit begin [ERROR], [WARN]
                    // so whole log will be displayed without decoration
                    StringBuilder buildLogMessage = new StringBuilder();
                    buildLogMessage.append(System.lineSeparator());
                    buildLogMessage.append(System.lineSeparator());
                    buildLogMessage.append("*** begin build.log for: " + buildJob.getProject() + " ***");
                    buildLogMessage.append(System.lineSeparator());
                    try (FileReader buildLogReader = new FileReader(buildLogFile)) {
                        buildLogMessage.append(IOUtil.toString(buildLogReader));
                    }
                    buildLogMessage.append("*** end build.log for: " + buildJob.getProject() + " ***");
                    buildLogMessage.append(System.lineSeparator());

                    logWithLevel(logger, ignoreFailures, SEPARATOR);
                    logWithLevel(logger, ignoreFailures, buildLogMessage.toString());
                    logWithLevel(logger, ignoreFailures, SEPARATOR);
                    logWithLevel(logger, ignoreFailures, "");

                } catch (IOException e) {
                    throw new MojoFailureException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Handles the build failures in this session.
     *
     * @param logger The mojo logger to output messages to, must not be <code>null</code>.
     * @param ignoreFailures A flag whether failures should be ignored or whether a build failure should be signaled.
     * @throws MojoFailureException If failures are present and not ignored.
     */
    public void handleFailures(Log logger, boolean ignoreFailures) throws MojoFailureException {
        updateStats();

        if (!failedJobs.isEmpty()) {
            String message = failedJobs.size() + " build" + (failedJobs.size() == 1 ? "" : "s") + " failed.";

            if (ignoreFailures) {
                logger.warn("Ignoring that " + message);
            } else {
                throw new MojoFailureException(message + " See console output above for details.");
            }
        }

        if (!errorJobs.isEmpty()) {
            String message = errorJobs.size() + " build" + (errorJobs.size() == 1 ? "" : "s") + " in error.";

            if (ignoreFailures) {
                logger.warn("Ignoring that " + message);
            } else {
                throw new MojoFailureException(message + " See console output above for details.");
            }
        }
    }

    /**
     * Log list of jobs.
     *
     * @param logger logger to write
     * @param warn flag indicate log level
     * @param buildJobs jobs to list
     */
    private void logBuildJobList(Log logger, boolean warn, String header, List<BuildJob> buildJobs) {
        if (buildJobs.isEmpty()) {
            return;
        }

        logWithLevel(logger, warn, header);

        for (BuildJob buildJob : buildJobs) {
            logWithLevel(logger, warn, "*  " + buildJob.getProject());
        }

        logger.info(SEPARATOR);
    }

    /**
     * Log message in correct level depends on flag.
     *
     * @param logger logger to write
     * @param warn flag indicate log level
     * @param message message to write
     */
    private void logWithLevel(Log logger, boolean warn, String message) {

        if (warn) {
            logger.warn(message);
        } else {
            logger.error(message);
        }
    }
}
