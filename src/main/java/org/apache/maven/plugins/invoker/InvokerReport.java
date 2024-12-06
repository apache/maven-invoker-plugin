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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.ReaderFactory;
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
     * Base directory where all build reports have been written to.
     */
    @Parameter(defaultValue = "${project.build.directory}/invoker-reports", property = "invoker.reportsDirectory")
    private File reportsDirectory;

    /**
     * Internationalization component
     */
    protected final I18N i18n;

    @Inject
    public InvokerReport(I18N i18n) {
        this.i18n = i18n;
    }

    protected void executeReport(Locale locale) throws MavenReportException {
        File[] reportFiles = getReportFiles();
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
        InvokerReportRenderer r = new InvokerReportRenderer(getSink(), i18n, locale, getLog(), buildJobs);
        r.render();
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    private String getI18nString(Locale locale, String key) {
        return i18n.getString("invoker-report", locale, "report.invoker." + key);
    }

    /** {@inheritDoc} */
    public String getName(Locale locale) {
        return getI18nString(locale, "name");
    }

    /** {@inheritDoc} */
    public String getDescription(Locale locale) {
        return getI18nString(locale, "description");
    }

    public String getOutputName() {
        return "invoker-report";
    }

    private File[] getReportFiles() {
        return ReportUtils.getReportFiles(reportsDirectory);
    }

    public boolean canGenerateReport() {
        return getReportFiles().length > 0;
    }
}
