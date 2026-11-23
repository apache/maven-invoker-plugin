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

import org.apache.maven.plugins.invoker.AbstractInvokerMojo.ToolchainPrivateManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link Selector}.
 */
class SelectorTest {
    @Test
    void globalMatch() {
        Selector selector = new Selector("3.2.5", "1.7", null);

        Properties props = new Properties();
        props.setProperty("invoker.maven.version", "3.0+");
        InvokerProperties invokerProperties = new InvokerProperties(props);
        assertThat(selector.getSelection(invokerProperties)).isZero();
    }

    @Test
    void selectorMatch() {
        Selector selector = new Selector("3.2.5", "1.7", null);

        Properties props = new Properties();
        props.setProperty("selector.1.maven.version", "3.0+");
        InvokerProperties invokerProperties = new InvokerProperties(props);
        assertThat(selector.getSelection(invokerProperties)).isZero();

        props.setProperty("selector.1.maven.version", "3.3.1+");
        assertThat(selector.getSelection(invokerProperties)).isEqualTo(Selector.SELECTOR_MULTI);
    }

    @Test
    void selectorWithGlobalMatch() {
        Selector selector = new Selector("3.2.5", "1.7", null);

        Properties props = new Properties();
        // invoker.maven.version is used by all selectors
        props.setProperty("invoker.maven.version", "3.0+");
        props.setProperty("selector.1.java.version", "1.4+");
        props.setProperty("selector.2.os.family", "myos");
        InvokerProperties invokerProperties = new InvokerProperties(props);
        assertThat(selector.getSelection(invokerProperties)).isZero();

        props.setProperty("invoker.maven.version", "3.3.1+");
        assertThat(selector.getSelection(invokerProperties)).isEqualTo(Selector.SELECTOR_MULTI);

        props.setProperty("invoker.maven.version", "3.0+");
        props.setProperty("selector.1.maven.version", "3.3.1+");
        assertThat(selector.getSelection(invokerProperties)).isEqualTo(Selector.SELECTOR_MULTI);

        props.setProperty("selector.2.os.family", "!myos");
        assertThat(selector.getSelection(invokerProperties)).isZero();
    }

    @Test
    void toolchainOnlySelector() {
        ToolchainPrivateManager toolchainPrivateManager = mock(ToolchainPrivateManager.class);
        Selector selector = new Selector("3.2.5", "1.7", toolchainPrivateManager);

        Properties props = new Properties();
        props.setProperty("selector.1.invoker.toolchain.jdk.version", "17");
        InvokerProperties invokerProperties = new InvokerProperties(props);

        when(toolchainPrivateManager.isToolchains(eq("jdk"), any())).thenReturn(false);
        assertThat(selector.getSelection(invokerProperties)).isEqualTo(Selector.SELECTOR_MULTI);

        when(toolchainPrivateManager.isToolchains(eq("jdk"), any())).thenReturn(true);
        assertThat(selector.getSelection(invokerProperties)).isZero();
    }
}
