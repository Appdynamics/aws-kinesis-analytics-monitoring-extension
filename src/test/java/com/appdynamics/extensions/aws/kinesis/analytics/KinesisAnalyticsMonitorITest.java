/*
 * Copyright (c) 2018 AppDynamics,Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.aws.kinesis.analytics;

import com.singularity.ee.agent.systemagent.api.TaskOutput;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by pradeep.nair on 8/3/18.
 */
public class KinesisAnalyticsMonitorITest {

    private KinesisAnalyticsMonitor classUnderTest = new KinesisAnalyticsMonitor();

    @Test
    public void testMetricsCollectionCredentialsEncrypted() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("config-file","src/test/resources/conf/itest-encrypted-config.yml");
        TaskOutput result = classUnderTest.execute(args, null);
        assertEquals(result.getStatusMessage(), "Monitor {} completes.");
    }

    @Test
    public void testMetricsCollectionWithProxy() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("config-file","src/test/resources/conf/itest-proxy-config.yml");
        TaskOutput result = classUnderTest.execute(args, null);
        assertEquals(result.getStatusMessage(), "Monitor {} completes.");
    }
}
