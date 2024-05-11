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

import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterpolatorUtilsTest {

    @Mock
    private MavenProject mavenProject;

    static Stream<Arguments> testAtInterpolate() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("test", "test"),
                Arguments.of("test@test", "test@test"),
                Arguments.of("test$test", "test$test"),
                Arguments.of("@{test}", "testInProps"),
                Arguments.of("${test}", "testInProps"),
                Arguments.of("test @{test} test", "test testInProps test"),
                Arguments.of("test ${test} test", "test testInProps test"),
                Arguments.of("@{test} @{test}", "testInProps testInProps"),
                Arguments.of("${test} ${test}", "testInProps testInProps"),
                Arguments.of("@{test} ${test}", "testInProps testInProps"));
    }

    @ParameterizedTest
    @MethodSource
    void testAtInterpolate(String input, String expected) throws Exception {
        // given
        Properties properties = new Properties();
        properties.put("test", "testInProps");
        when(mavenProject.getProperties()).thenReturn(properties);
        InterpolatorUtils interpolatorUtils = new InterpolatorUtils(mavenProject);

        // when
        String output = interpolatorUtils.interpolateAtPattern(input);

        // then
        assertThat(output).isEqualTo(expected);
    }
}
