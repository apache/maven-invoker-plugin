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

import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

public class InvokerReportRenderer extends AbstractMavenReportRenderer {
    private final I18N i18n;
    private final Locale locale;
    private final Log log;
    private final List<BuildJob> buildJobs;

    public InvokerReportRenderer(Sink sink, I18N i18n, Locale locale, Log log, List<BuildJob> buildJobs) {
        super(sink);
        this.i18n = i18n;
        this.locale = locale;
        this.log = log;
        this.buildJobs = buildJobs;
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * @param key The key to translate.
     * @return the translated key.
     */
    private String getI18nString(String key) {
        return i18n.getString("invoker-report", locale, "report.invoker." + key);
    }

    /**
     * @param key The key to translate.
     * @param args The args to pass to translated string.
     * @return the translated key.
     */
    private String formatI18nString(String key, Object... args) {
        return i18n.format("invoker-report", locale, "report.invoker." + key, args);
    }

    @Override
    protected void renderBody() {
        startSection(getTitle());
        paragraph(getI18nString("description"));

        renderSectionSummary();

        renderSectionDetails();

        endSection();
    }

    private void renderSectionSummary() {
        startSection(getI18nString("summary.title"));

        startTable();

        tableHeader(new String[] {
            getI18nString("summary.builds"),
            getI18nString("summary.success"),
            getI18nString("summary.failures"),
            getI18nString("summary.skipped"),
            getI18nString("summary.successrate"),
            getI18nString("summary.time")
        });

        int totalBuilds = buildJobs.size();
        int totalSuccess = 0;
        int totalFailures = 0;
        int totalSkipped = 0;
        float totalTime = 0.0f;

        for (BuildJob buildJob : buildJobs) {
            switch (buildJob.getResult()) {
                case BuildJob.Result.SUCCESS:
                    totalSuccess++;
                    break;
                case BuildJob.Result.SKIPPED:
                    totalSkipped++;
                    break;
                default:
                    totalFailures++;
            }
            totalTime += buildJob.getTime();
        }

        tableRow(new String[] {
            Integer.toString(totalBuilds),
            Integer.toString(totalSuccess),
            Integer.toString(totalFailures),
            Integer.toString(totalSkipped),
            (totalSuccess + totalFailures > 0)
                    ? formatI18nString("value.successrate", (totalSuccess / (float) (totalSuccess + totalFailures)))
                    : "",
            formatI18nString("value.time", totalTime)
        });

        endTable();

        endSection();
    }

    private void renderSectionDetails() {
        startSection(getI18nString("detail.title"));

        startTable();

        tableHeader(new String[] {
            getI18nString("detail.name"),
            getI18nString("detail.result"),
            getI18nString("detail.time"),
            getI18nString("detail.message")
        });

        for (BuildJob buildJob : buildJobs) {
            renderBuildJob(buildJob);
        }

        endTable();

        endSection();
    }

    private void renderBuildJob(BuildJob buildJob) {
        tableRow(new String[] {
            getBuildJobReportName(buildJob),
            // FIXME image
            buildJob.getResult(),
            formatI18nString("value.time", buildJob.getTime()),
            buildJob.getFailureMessage()
        });
    }

    private String getBuildJobReportName(BuildJob buildJob) {
        String buildJobName = buildJob.getName();
        String buildJobDescription = buildJob.getDescription();
        boolean emptyJobName = buildJobName == null || buildJobName.isEmpty();
        boolean emptyJobDescription = buildJobDescription == null || buildJobDescription.isEmpty();
        boolean isReportJobNameComplete = !emptyJobName && !emptyJobDescription;
        if (isReportJobNameComplete) {
            return formatI18nString("text.name_with_description", buildJobName, buildJobDescription);
        } else {
            String buildJobProject = buildJob.getProject();
            if (!emptyJobName) {
                log.warn(incompleteNameWarning("description", buildJobProject));
            } else if (!emptyJobDescription) {
                log.warn(incompleteNameWarning("name", buildJobProject));
            }
            return buildJobProject;
        }
    }

    private static String incompleteNameWarning(String missing, String pom) {
        return "Incomplete job name-description: " + missing + " is missing. POM (" + pom
                + ") will be used in place of job name!";
    }
}
