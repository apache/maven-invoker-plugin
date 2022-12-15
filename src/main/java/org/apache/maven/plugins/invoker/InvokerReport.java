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
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Generate a report based on the results of the Maven invocations. <strong>Note:</strong> This mojo doesn't fork any
 * lifecycle, if you have a clean working copy, you have to use a command like
 * <code>mvn clean integration-test site</code> to ensure the build results are present when this goal is invoked.
 *
 * @author Olivier Lamy
 * @since 1.4
 */
@Mojo(name = "report", threadSafe = true)
public class InvokerReport extends AbstractMavenReport {

    /**
     * Internationalization component.
     */
    @Component
    protected I18N i18n;

    /**
     * Base directory where all build reports have been written to.
     */
    @Parameter(defaultValue = "${project.build.directory}/invoker-reports", property = "invoker.reportsDirectory")
    private File reportsDirectory;

    /**
     * The number format used to print percent values in the report locale.
     */
    private NumberFormat percentFormat;

    /**
     * The number format used to print time values in the report locale.
     */
    private NumberFormat secondsFormat;

    /**
     * The format used to print build name and description.
     */
    private MessageFormat nameAndDescriptionFormat;

    protected void executeReport(Locale locale) throws MavenReportException {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        percentFormat = new DecimalFormat(getText(locale, "report.invoker.format.percent"), symbols);
        secondsFormat = new DecimalFormat(getText(locale, "report.invoker.format.seconds"), symbols);
        nameAndDescriptionFormat = new MessageFormat(getText(locale, "report.invoker.format.name_with_description"));

        Sink sink = getSink();

        sink.head();

        sink.title();
        sink.text(getText(locale, "report.invoker.result.title"));
        sink.title_();

        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text(getText(locale, "report.invoker.result.title"));
        sink.sectionTitle1_();
        sink.paragraph();
        sink.text(getText(locale, "report.invoker.result.description"));
        sink.paragraph_();
        sink.section1_();

        // ----------------------------------
        // build buildJob beans
        // ----------------------------------
        File[] reportFiles = ReportUtils.getReportFiles(reportsDirectory);
        if (reportFiles.length <= 0) {
            getLog().info("no invoker report files found, skip report generation");
            return;
        }

        BuildJobXpp3Reader buildJobReader = new BuildJobXpp3Reader();

        List<BuildJob> buildJobs = new ArrayList<>(reportFiles.length);
        for (File reportFile : reportFiles) {
            try (XmlStreamReader xmlReader = ReaderFactory.newXmlReader(reportFile)) {
                buildJobs.add(buildJobReader.read(xmlReader));
            } catch (XmlPullParserException e) {
                throw new MavenReportException("Failed to parse report file: " + reportFile, e);
            } catch (IOException e) {
                throw new MavenReportException("Failed to read report file: " + reportFile, e);
            }
        }

        // ----------------------------------
        // summary
        // ----------------------------------

        constructSummarySection(buildJobs, locale);

        // ----------------------------------
        // per file/it detail
        // ----------------------------------

        sink.section2();
        sink.sectionTitle2();

        sink.text(getText(locale, "report.invoker.detail.title"));

        sink.sectionTitle2_();

        sink.section2_();

        // detail tests table header
        sink.table();
        sink.tableRows(null, false);

        sink.tableRow();
        // -------------------------------------------
        // name | Result | time | message
        // -------------------------------------------
        sinkTableHeader(sink, getText(locale, "report.invoker.detail.name"));
        sinkTableHeader(sink, getText(locale, "report.invoker.detail.result"));
        sinkTableHeader(sink, getText(locale, "report.invoker.detail.time"));
        sinkTableHeader(sink, getText(locale, "report.invoker.detail.message"));

        sink.tableRow_();

        for (BuildJob buildJob : buildJobs) {
            renderBuildJob(buildJob);
        }

        sink.tableRows_();
        sink.table_();

        sink.body_();

        sink.flush();
        sink.close();
    }

    private void constructSummarySection(List<? extends BuildJob> buildJobs, Locale locale) {
        Sink sink = getSink();

        sink.section2();
        sink.sectionTitle2();

        sink.text(getText(locale, "report.invoker.summary.title"));

        sink.sectionTitle2_();
        sink.section2_();

        // ------------------------------------------------------------------------
        // Building a table with
        // it number | succes nb | failed nb | Success rate | total time | avg time
        // ------------------------------------------------------------------------

        sink.table();
        sink.tableRows(null, false);

        sink.tableRow();

        sinkTableHeader(sink, getText(locale, "report.invoker.summary.number"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.success"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.failed"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.skipped"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.success.rate"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.time.total"));
        sinkTableHeader(sink, getText(locale, "report.invoker.summary.time.avg"));

        int number = buildJobs.size();
        int success = 0;
        int failed = 0;
        int skipped = 0;
        double totalTime = 0;

        for (BuildJob buildJob : buildJobs) {
            if (BuildJob.Result.SUCCESS.equals(buildJob.getResult())) {
                success++;
            } else if (BuildJob.Result.SKIPPED.equals(buildJob.getResult())) {
                skipped++;
            } else {
                failed++;
            }
            totalTime += buildJob.getTime();
        }

        sink.tableRow_();
        sink.tableRow();

        sinkCell(sink, Integer.toString(number));
        sinkCell(sink, Integer.toString(success));
        sinkCell(sink, Integer.toString(failed));
        sinkCell(sink, Integer.toString(skipped));

        if (success + failed > 0) {
            sinkCell(sink, percentFormat.format((double) success / (success + failed)));
        } else {
            sinkCell(sink, "");
        }

        sinkCell(sink, secondsFormat.format(totalTime));

        sinkCell(sink, secondsFormat.format(totalTime / number));

        sink.tableRow_();

        sink.tableRows_();
        sink.table_();
    }

    private void renderBuildJob(BuildJob buildJob) {
        Sink sink = getSink();
        sink.tableRow();
        sinkCell(sink, getBuildJobReportName(buildJob));
        // FIXME image
        sinkCell(sink, buildJob.getResult());
        sinkCell(sink, secondsFormat.format(buildJob.getTime()));
        sinkCell(sink, buildJob.getFailureMessage());
        sink.tableRow_();
    }

    private String getBuildJobReportName(BuildJob buildJob) {
        String buildJobName = buildJob.getName();
        String buildJobDescription = buildJob.getDescription();
        boolean emptyJobName = StringUtils.isEmpty(buildJobName);
        boolean emptyJobDescription = StringUtils.isEmpty(buildJobDescription);
        boolean isReportJobNameComplete = !emptyJobName && !emptyJobDescription;
        if (isReportJobNameComplete) {
            return getFormattedName(buildJobName, buildJobDescription);
        } else {
            String buildJobProject = buildJob.getProject();
            if (!emptyJobName) {
                getLog().warn(incompleteNameWarning("description", buildJobProject));
            } else if (!emptyJobDescription) {
                getLog().warn(incompleteNameWarning("name", buildJobProject));
            }
            return buildJobProject;
        }
    }

    private static String incompleteNameWarning(String missing, String pom) {
        return String.format(
                "Incomplete job name-description: %s is missing. " + "POM (%s) will be used in place of job name.",
                missing, pom);
    }

    private String getFormattedName(String name, String description) {
        return nameAndDescriptionFormat.format(new Object[] {name, description});
    }

    public String getDescription(Locale locale) {
        return getText(locale, "report.invoker.result.description");
    }

    public String getName(Locale locale) {
        return getText(locale, "report.invoker.result.name");
    }

    public String getOutputName() {
        return "invoker-report";
    }

    public boolean canGenerateReport() {
        return ReportUtils.getReportFiles(reportsDirectory).length > 0;
    }

    private String getText(Locale locale, String key) {
        return i18n.getString("invoker-report", locale, key);
    }

    private void sinkTableHeader(Sink sink, String header) {
        sink.tableHeaderCell();
        sink.text(header);
        sink.tableHeaderCell_();
    }

    private void sinkCell(Sink sink, String text) {
        sink.tableCell();
        sink.text(text);
        sink.tableCell_();
    }
}
