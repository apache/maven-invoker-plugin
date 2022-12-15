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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugins.invoker.model.BuildJob;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for JobExecutor.
 *
 * @author Slawomir Jaranowski
 */
public class JobExecutorTest {

    @Test
    public void emptyJobList() {
        JobExecutor jobExecutor = new JobExecutor(Collections.emptyList(), 1);

        jobExecutor.forEach(job -> fail("fail"));
    }

    @Test
    public void failedJob() {
        BuildJob job = aJob("job1", 100);

        JobExecutor jobExecutor = new JobExecutor(Collections.singletonList(job), 1);

        jobExecutor.forEach(j -> fail("fail " + j.getProject()));

        assertThat(job.getResult()).isEqualTo(BuildJob.Result.ERROR);
        assertThat(job.getFailureMessage()).isEqualTo("java.lang.AssertionError: fail job1");
    }

    @Test
    public void jobsShouldBeGroupedAndExecutedInProperOrder() {
        Map<Integer, AtomicInteger> jobsCounter = new HashMap<>();
        jobsCounter.put(100, new AtomicInteger(3));
        jobsCounter.put(10, new AtomicInteger(2));
        jobsCounter.put(1, new AtomicInteger(1));

        BuildJob job1 = aJob("job1-100", 100);
        BuildJob job2 = aJob("job2-100", 100);
        BuildJob job3 = aJob("job3-100", 100);

        BuildJob job4 = aJob("job4-10", 10);
        BuildJob job5 = aJob("job5-10", 10);

        BuildJob job6 = aJob("job6-1", 1);

        // put jobs in wrong order
        List<BuildJob> jobs = Arrays.asList(job4, job5, job1, job2, job6, job3);

        JobExecutor jobExecutor = new JobExecutor(jobs, 10);

        jobExecutor.forEach(job -> {
            jobsCounter.get(job.getOrdinal()).decrementAndGet();

            switch (job.getOrdinal()) {
                case 100:
                    assertThat(jobsCounter.get(10).get())
                            .as("Jobs-10 must not be executed before 100")
                            .isEqualTo(2);

                    assertThat(jobsCounter.get(1).get())
                            .as("Jobs-1 must not be executed before 100")
                            .isEqualTo(1);
                    break;

                case 10:
                    assertThat(jobsCounter.get(100).get())
                            .as("Jobs-100 must be executed before 10")
                            .isZero();

                    assertThat(jobsCounter.get(1).get())
                            .as("Jobs-1 must not be executed before 10")
                            .isEqualTo(1);
                    break;

                case 1:
                    assertThat(jobsCounter.get(100).get())
                            .as("Jobs-100 must be executed before 1")
                            .isZero();

                    assertThat(jobsCounter.get(10).get())
                            .as("Jobs-10 must be executed before 1")
                            .isZero();
                    break;

                default:
                    fail("invalid job ordinal value %d", job.getOrdinal());
                    break;
            }

            job.setResult(BuildJob.Result.SUCCESS);
            job.setDescription(Thread.currentThread().getName());
        });

        // all jobs have success status
        assertThat(jobs).allSatisfy(job -> {
            assertThat(job.getDescription()).isNotBlank();
            assertThat(job.getResult()).as(job.getFailureMessage()).isEqualTo(BuildJob.Result.SUCCESS);
        });

        // jobs run on separate thread
        assertThat(job1.getDescription()).isNotEqualTo(job2.getDescription());
        assertThat(job1.getDescription()).isNotEqualTo(job3.getDescription());
        assertThat(job2.getDescription()).isNotEqualTo(job3.getDescription());

        assertThat(job4.getDescription()).isNotEqualTo(job5.getDescription());
    }

    private BuildJob aJob(String name, int ordinal) {
        BuildJob buildJob = new BuildJob(name);
        buildJob.setOrdinal(ordinal);
        return buildJob;
    }
}
