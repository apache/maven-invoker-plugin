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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.maven.plugins.invoker.model.BuildJob;

/**
 * Execute build jobs with parallel.
 *
 * @author Slawomir Jaranowski
 */
class JobExecutor {
    interface ThrowableJobConsumer {
        void accept(BuildJob t) throws Throwable;
    }

    private final List<BuildJob> jobs;
    private final int threadsCount;

    JobExecutor(List<BuildJob> jobs, int threadsCount) {
        this.jobs = jobs;
        this.threadsCount = threadsCount;
    }

    public void forEach(ThrowableJobConsumer jobConsumer) {
        // group and sort jobs by ordinal
        Map<Integer, List<BuildJob>> groupedJobs = jobs.stream()
                .sorted((j1, j2) -> Integer.compare(j2.getOrdinal(), j1.getOrdinal()))
                .collect(Collectors.groupingBy(BuildJob::getOrdinal, LinkedHashMap::new, Collectors.toList()));

        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);

        groupedJobs.forEach((key, value) -> {
            // prepare list of callable tasks
            List<Callable<Void>> callableJobs = value.stream()
                    .map(buildJob -> (Callable<Void>) () -> {
                        try {
                            jobConsumer.accept(buildJob);
                        } catch (Throwable e) {
                            buildJob.setResult(BuildJob.Result.ERROR);
                            buildJob.setFailureMessage(String.valueOf(e));
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            try {
                executorService.invokeAll(callableJobs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        // all task are finished here
        executorService.shutdownNow();
    }
}
