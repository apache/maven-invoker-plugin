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
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Helper component for interpolating values.
 */
@Named
class InterpolatorUtils {

    private final Interpolator atInterpolator;

    /**
     * A default constructor.
     *
     * @param mavenProject a MavenProject
     */
    @Inject
    InterpolatorUtils(MavenProject mavenProject) {
        atInterpolator = new RegexBasedInterpolator("[@\\$]\\{(.+?)", "}");
        atInterpolator.addValueSource(new MapBasedValueSource(mavenProject.getProperties()));
    }

    public String interpolateAtPattern(String value) throws MojoExecutionException {

        if (value == null || !(value.contains("@{") || value.contains("${"))) {
            return value;
        }

        try {
            return atInterpolator.interpolate(value);
        } catch (InterpolationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
