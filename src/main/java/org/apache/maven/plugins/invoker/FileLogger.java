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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.scriptinterpreter.FileLoggerMirrorHandler;

/**
 *
 */
class FileLogger extends org.apache.maven.shared.scriptinterpreter.FileLogger implements InvocationOutputHandler {

    /**
     * Creates a new logger that writes to the specified file and optionally mirrors messages to the given mojo logger.
     *
     * @param outputFile The path to the output file, must not be <code>null</code>.
     * @param log The mojo logger to additionally output messages to, may be <code>null</code> if not used.
     * @throws IOException If the output file could not be created.
     */
    FileLogger(File outputFile, final Log log) throws IOException {
        this(outputFile, log, null);
    }

    /**
     * Creates a new logger that writes to the specified file and optionally mirrors messages to the given mojo
     * logger, prefixing every mirrored line with {@code logPrefix}. The file content is never prefixed, only the
     * lines mirrored to the mojo logger, which is how per-job output stays attributable when several jobs run in
     * parallel and interleave on the console.
     *
     * @param outputFile The path to the output file, must not be <code>null</code>.
     * @param log The mojo logger to additionally output messages to, may be <code>null</code> if not used.
     * @param logPrefix The prefix to prepend to every line mirrored to the mojo logger, may be <code>null</code> or
     *            empty if no prefix is needed.
     * @throws IOException If the output file could not be created.
     */
    FileLogger(File outputFile, final Log log, final String logPrefix) throws IOException {
        super(outputFile, toMirrorHandler(log, logPrefix));
    }

    private static FileLoggerMirrorHandler toMirrorHandler(final Log log, final String logPrefix) {
        if (log == null) {
            return null;
        }
        if (logPrefix == null || logPrefix.isEmpty()) {
            return log::info;
        }
        return line -> log.info(logPrefix + line);
    }
}
