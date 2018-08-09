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

import com.appdynamics.extensions.aws.SingleNamespaceCloudwatchMonitor;
import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;
import com.appdynamics.extensions.aws.config.Configuration;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import static com.appdynamics.extensions.aws.kinesis.analytics.util.Constants.*;

/**
 * Created by pradeep.nair on 8/3/18.
 */
public class KinesisAnalyticsMonitor extends SingleNamespaceCloudwatchMonitor<Configuration> {

    private static final Logger LOGGER = Logger.getLogger(KinesisAnalyticsMonitor.class);

    public KinesisAnalyticsMonitor() {
        super(Configuration.class);
        LOGGER.info(String.format("Using AWS KinesisAnalyticsMonitor Monitor Version [%s]",
                this.getClass().getPackage().getImplementationTitle()));
    }

    @Override
    protected NamespaceMetricStatisticsCollector getNamespaceMetricsCollector(Configuration config) {
        MetricsProcessor metricsProcessor = createMetricsProcessor(config);
        return new NamespaceMetricStatisticsCollector.Builder(config.getAccounts(),
                config.getConcurrencyConfig(),
                config.getMetricsConfig(),
                metricsProcessor,
                config.getMetricPrefix())
                .withCredentialsDecryptionConfig(config.getCredentialsDecryptionConfig())
                .withProxyConfig(config.getProxyConfig())
                .build();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected int getTaskCount() {
        return 3;
    }

    private MetricsProcessor createMetricsProcessor(Configuration config) {
        return new KinesisAnalyticsMetricsProcessor(config.getMetricsConfig().getIncludeMetrics(),
                config.getDimensions());
    }

    public static void main(String[] args) throws Exception{
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(ca);
        KinesisAnalyticsMonitor monitor = new KinesisAnalyticsMonitor();
        Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file",
                "src/main/resources/conf/config.yml");
        monitor.execute(taskArgs, null);
    }
}
