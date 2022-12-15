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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest.ReactorFailureBehavior;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the invoker properties facade.
 *
 * @author Benjamin Bentmann
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokerPropertiesTest {

    @Mock
    private InvocationRequest request;

    @Test
    public void testConstructorNullSafe() {
        InvokerProperties facade = new InvokerProperties(null);
        assertThat(facade.getProperties()).isNotNull();
    }

    @Test
    public void testGetInvokerProperty() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        assertThat(facade.get("undefined-key", 0)).isEmpty();

        props.setProperty("key", "value");
        assertThat(facade.get("key", 1)).hasValue("value");

        props.setProperty("key.1", "another-value");
        assertThat(facade.get("key", 1)).hasValue("another-value");
        assertThat(facade.get("key", 2)).hasValue("value");
    }

    @Test
    public void testGetJobName() {
        Properties props = new Properties();
        final String jobName = "Build Job name";
        props.put("invoker.name", jobName);
        InvokerProperties facade = new InvokerProperties(props);

        assertThat(facade.getJobName()).isEqualTo(jobName);
    }

    @Test
    public void testIsExpectedResult() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        assertThat(facade.isExpectedResult(0, 0)).isTrue();
        assertThat(facade.isExpectedResult(1, 0)).isFalse();

        props.setProperty("invoker.buildResult", "success");
        assertThat(facade.isExpectedResult(0, 0)).isTrue();
        assertThat(facade.isExpectedResult(1, 0)).isFalse();

        props.setProperty("invoker.buildResult", "failure");
        assertThat(facade.isExpectedResult(0, 0)).isFalse();
        assertThat(facade.isExpectedResult(1, 0)).isTrue();
    }

    @Test
    public void testConfigureRequestEmptyProperties() {

        InvokerProperties facade = new InvokerProperties(null);

        facade.configureInvocation(request, 0);
        verifyNoInteractions(request);
    }

    @Test
    public void testConfigureRequestGoals() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.goals", "verify");
        facade.configureInvocation(request, 0);
        verify(request).setGoals(Collections.singletonList("verify"));
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.goals", "   ");
        facade.configureInvocation(request, 0);
        verify(request, never()).setGoals(anyList());
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.goals", "");
        facade.configureInvocation(request, 0);
        verify(request, never()).setGoals(anyList());
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.goals", "  clean , test   verify ");
        facade.configureInvocation(request, 0);
        verify(request).setGoals(Arrays.asList("clean", "test", "verify"));
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.clear();

        facade.setDefaultGoals(Arrays.asList("clean", "test"));
        facade.configureInvocation(request, 0);
        verify(request).setGoals(Arrays.asList("clean", "test"));
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestProfiles() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.profiles", "verify");
        facade.configureInvocation(request, 0);
        verify(request).setProfiles(Collections.singletonList("verify"));
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.profiles", "   ");
        facade.configureInvocation(request, 0);
        verify(request, never()).setProfiles(anyList());
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.profiles", "");
        facade.configureInvocation(request, 0);
        verify(request, never()).setProfiles(anyList());
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.profiles", "  clean , test   verify  ,");
        facade.configureInvocation(request, 0);
        verify(request).setProfiles(Arrays.asList("clean", "test", "verify"));
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.clear();
        facade.setDefaultProfiles(Arrays.asList("profile1", "profile2"));
        facade.configureInvocation(request, 0);
        verify(request).setProfiles(Arrays.asList("profile1", "profile2"));
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestProject() throws Exception {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        File tempPom = File.createTempFile("maven-invoker-plugin-test", ".pom");
        try {
            File tempDir = tempPom.getParentFile();
            when(request.getBaseDirectory()).thenReturn(tempDir);

            props.setProperty("invoker.project", tempPom.getName());
            facade.configureInvocation(request, 0);
            verify(request).getBaseDirectory();
            verify(request).setBaseDirectory(tempDir);
            verify(request).setPomFile(tempPom);
            verifyNoMoreInteractions(request);
            clearInvocations(request);

            props.setProperty("invoker.project", "");
            facade.configureInvocation(request, 0);
            verifyNoInteractions(request);
        } finally {
            tempPom.delete();
        }
    }

    @Test
    public void testConfigureRequestMavenExecutable() {
        Properties props = new Properties();

        InvokerProperties facade = new InvokerProperties(props);
        File aDefExecutable = new File("defExecutable");
        facade.setDefaultMavenExecutable(aDefExecutable);

        props.setProperty("invoker.mavenExecutable", "aPropExecutable");
        facade.configureInvocation(request, 0);
        verify(request).setMavenExecutable(new File("aPropExecutable"));
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.clear();

        facade.configureInvocation(request, 0);
        verify(request).setMavenExecutable(aDefExecutable);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestMavenOpts() {
        Properties props = new Properties();

        InvokerProperties facade = new InvokerProperties(props);
        facade.setDefaultMavenOpts("-XxxDef");

        props.setProperty("invoker.mavenOpts", "-Xmx512m");
        facade.configureInvocation(request, 0);
        verify(request).setMavenOpts("-Xmx512m");
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.clear();

        facade.configureInvocation(request, 0);
        verify(request).setMavenOpts("-XxxDef");
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestFailureBehavior() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.failureBehavior", ReactorFailureBehavior.FailNever.getLongOption());
        facade.configureInvocation(request, 0);
        verify(request).setReactorFailureBehavior(eq(ReactorFailureBehavior.FailNever));

        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestFailureBehaviorUnKnownName() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.failureBehavior", "xxxUnKnown");

        assertThatCode(() -> facade.configureInvocation(request, 0))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The string 'xxxUnKnown' can not be converted to enumeration.");

        verifyNoInteractions(request);
    }

    @Test
    public void testConfigureRequestRecursion() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.nonRecursive", "true");
        facade.configureInvocation(request, 0);
        verify(request).setRecursive(false);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.nonRecursive", "false");
        facade.configureInvocation(request, 0);
        verify(request).setRecursive(true);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestOffline() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.offline", "true");
        facade.configureInvocation(request, 0);
        verify(request).setOffline(true);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.offline", "false");
        facade.configureInvocation(request, 0);
        verify(request).setOffline(false);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureRequestDebug() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.debug", "true");
        facade.configureInvocation(request, 0);
        verify(request).setDebug(true);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.debug", "false");
        facade.configureInvocation(request, 0);
        verify(request).setDebug(false);
        verifyNoMoreInteractions(request);

        props.clear();

        facade.setDefaultDebug(true);
        facade.configureInvocation(request, 0);
        verify(request).setDebug(true);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        facade.setDefaultDebug(false);
        facade.configureInvocation(request, 0);
        verify(request).setDebug(false);
        verifyNoMoreInteractions(request);
        clearInvocations(request);
    }

    @Test
    public void testConfigureRequestQuiet() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.quiet", "true");
        facade.configureInvocation(request, 0);
        verify(request).setQuiet(true);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.setProperty("invoker.quiet", "false");
        facade.configureInvocation(request, 0);
        verify(request).setQuiet(false);
        verifyNoMoreInteractions(request);

        props.clear();

        facade.setDefaultQuiet(true);
        facade.configureInvocation(request, 0);
        verify(request).setQuiet(true);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        facade.setDefaultQuiet(false);
        facade.configureInvocation(request, 0);
        verify(request).setQuiet(false);
        verifyNoMoreInteractions(request);
        clearInvocations(request);
    }

    @Test
    public void testConfigureRequestTimeoutInSeconds() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.timeoutInSeconds", "5");
        facade.configureInvocation(request, 0);
        verify(request).setTimeoutInSeconds(5);
        verifyNoMoreInteractions(request);
        clearInvocations(request);

        props.clear();

        facade.setDefaultTimeoutInSeconds(3);
        facade.configureInvocation(request, 0);
        verify(request).setTimeoutInSeconds(3);
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureEnvironmentVariables() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.abcdef", "abcdf");
        props.setProperty("invoker.environmentVariables.KEY1.1", "value1.1");
        props.setProperty("invoker.environmentVariables.KEY1", "value1");
        props.setProperty("invoker.environmentVariables.KEY2", "value2");
        props.setProperty("invoker.environmentVariables.KEY2.1", "value2.1");
        props.setProperty("invoker.environmentVariables.KEY3", "value3");
        facade.configureInvocation(request, 0);
        verify(request).addShellEnvironment("KEY1", "value1");
        verify(request).addShellEnvironment("KEY2", "value2");
        verify(request).addShellEnvironment("KEY3", "value3");
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureEnvironmentVariablesWithIndex() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.abcdef", "abcdf");
        props.setProperty("invoker.environmentVariables.KEY1.1", "value1.1");
        props.setProperty("invoker.environmentVariables.KEY1", "value1");
        props.setProperty("invoker.environmentVariables.KEY2", "value2");
        props.setProperty("invoker.environmentVariables.KEY2.1", "value2.1");
        props.setProperty("invoker.environmentVariables.KEY3", "value3");
        facade.configureInvocation(request, 1);
        verify(request).addShellEnvironment("KEY1", "value1.1");
        verify(request).addShellEnvironment("KEY2", "value2.1");
        verify(request).addShellEnvironment("KEY3", "value3");
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureUpdateSnapshots() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        props.setProperty("invoker.updateSnapshots", "true");
        facade.configureInvocation(request, 1);
        verify(request).setUpdateSnapshots(true);
        clearInvocations(request);
        props.clear();

        props.setProperty("invoker.updateSnapshots", "false");
        facade.configureInvocation(request, 1);
        verify(request).setUpdateSnapshots(false);

        verifyNoMoreInteractions(request);
    }

    @Test
    public void testConfigureUpdateSnapshotsDefault() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        facade.setDefaultUpdateSnapshots(true);
        facade.configureInvocation(request, 1);
        verify(request).setUpdateSnapshots(true);
        clearInvocations(request);

        facade.setDefaultUpdateSnapshots(false);
        facade.configureInvocation(request, 1);
        verify(request).setUpdateSnapshots(false);

        verifyNoMoreInteractions(request);
    }

    @Test
    public void testIsInvocationDefined() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        assertThat(facade.isInvocationDefined(1)).isFalse();

        props.setProperty("invoker.goals", "install");
        assertThat(facade.isInvocationDefined(1)).isFalse();

        props.setProperty("invoker.goals.2", "install");
        assertThat(facade.isInvocationDefined(1)).isFalse();
        assertThat(facade.isInvocationDefined(2)).isTrue();
        assertThat(facade.isInvocationDefined(3)).isFalse();

        props.setProperty("invoker.goals.3", "install");
        assertThat(facade.isInvocationDefined(1)).isFalse();
        assertThat(facade.isInvocationDefined(2)).isTrue();
        assertThat(facade.isInvocationDefined(3)).isTrue();
        assertThat(facade.isInvocationDefined(4)).isFalse();
    }

    @Test
    public void testIsSelectedDefined() {
        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        assertThat(facade.isSelectorDefined(1)).isFalse();

        props.setProperty("invoker.java.version", "1.6+");
        props.setProperty("invoker.maven.version", "3.0+");
        props.setProperty("invoker.os.family", "windows");
        assertThat(facade.isSelectorDefined(1)).isFalse();

        props.setProperty("selector.2.java.version", "1.6+");
        props.setProperty("selector.3.maven.version", "3.0+");
        props.setProperty("selector.4.os.family", "windows");
        assertThat(facade.isSelectorDefined(1)).isFalse();
        assertThat(facade.isSelectorDefined(2)).isTrue();
        assertThat(facade.isSelectorDefined(3)).isTrue();
        assertThat(facade.isSelectorDefined(4)).isTrue();
        assertThat(facade.isSelectorDefined(5)).isFalse();
    }

    @Test
    public void testGetToolchainsForEmptyProperties() {

        Properties props = new Properties();
        InvokerProperties facade = new InvokerProperties(props);

        Collection<InvokerToolchain> toolchains = facade.getToolchains();
        assertThat(toolchains).isEmpty();

        toolchains = facade.getToolchains(1);
        assertThat(toolchains).isEmpty();
    }

    @Test
    public void testGetToolchains() {
        Properties props = new Properties();
        props.put("invoker.toolchain.jdk.version", "11");
        InvokerProperties facade = new InvokerProperties(props);

        Collection<InvokerToolchain> toolchains = facade.getToolchains();
        assertThat(toolchains).hasSize(1);
        InvokerToolchain toolchain = toolchains.iterator().next();
        assertThat(toolchain.getType()).isEqualTo("jdk");
        assertThat(toolchain.getProvides()).containsExactlyEntriesOf(Collections.singletonMap("version", "11"));
    }

    @Test
    public void testGetToolchainsWithIndex() {
        Properties props = new Properties();
        props.put("selector.1.invoker.toolchain.jdk.version", "11");
        InvokerProperties facade = new InvokerProperties(props);

        Collection<InvokerToolchain> toolchains = facade.getToolchains(1);
        assertThat(toolchains).hasSize(1);
        InvokerToolchain toolchain = toolchains.iterator().next();
        assertThat(toolchain.getType()).isEqualTo("jdk");
        assertThat(toolchain.getProvides()).containsExactlyEntriesOf(Collections.singletonMap("version", "11"));
    }
}
