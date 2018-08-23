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

import com.appdynamics.extensions.aws.config.Dimension;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.metrics.Metric;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by pradeep.nair on 8/19/18.
 */
public class KinesisAnalyticsMetricsProcessorTest {

    private final NamespaceMetricStatistics namespaceMetricStats = new NamespaceMetricStatistics();
    private KinesisAnalyticsMetricsProcessor kinesisAnalyticsMetricsProcessor;

    @Before
    public void setup() {
        // Populate dimension with their display name
        List<Dimension> dimensions = new ArrayList<>();
        Dimension dimension = new Dimension();
        dimension.setName("Flow");
        dimension.setDisplayName("Flow");
        dimensions.add(dimension);
        dimension = new Dimension();
        dimension.setName("Id");
        dimension.setDisplayName("Id");
        dimensions.add(dimension);
        dimension = new Dimension();
        dimension.setName("Application");
        dimension.setDisplayName("Application");
        dimensions.add(dimension);

        kinesisAnalyticsMetricsProcessor = new KinesisAnalyticsMetricsProcessor(new ArrayList<>(), dimensions);
        createNamespaceMetricStatsForTest();
    }

    private void createNamespaceMetricStatsForTest() {
        AccountMetricStatistics accountStats = new AccountMetricStatistics();
        accountStats.setAccountName("Appd");
        RegionMetricStatistics regionStats = new RegionMetricStatistics();
        regionStats.setRegion("us-west-2");
        IncludeMetric includeMetric = new IncludeMetric();
        includeMetric.setName("testMetric");

        List<com.amazonaws.services.cloudwatch.model.Dimension> dimensions = new ArrayList<>();
        com.amazonaws.services.cloudwatch.model.Dimension dimension = new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension.withName("Id").withValue("2.1");
        dimensions.add(dimension);
        dimension = new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension.withName("Flow").withValue("Input");
        dimensions.add(dimension);
        dimension = new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension.withName("Application").withValue("Sample");
        dimensions.add(dimension);

        com.amazonaws.services.cloudwatch.model.Metric metric = new com.amazonaws.services.cloudwatch.model.Metric();
        metric.setDimensions(dimensions);

        AWSMetric awsMetric = new AWSMetric();
        awsMetric.setIncludeMetric(includeMetric);
        awsMetric.setMetric(metric);

        MetricStatistic metricStatistic = new MetricStatistic();
        metricStatistic.setMetric(awsMetric);
        metricStatistic.setValue(new Random().nextDouble());
        metricStatistic.setUnit("testUnit");
        metricStatistic.setMetricPrefix("Custom Metrics|AWS Kinesis Analytics|");

        regionStats.addMetricStatistic(metricStatistic);
        accountStats.add(regionStats);
        namespaceMetricStats.add(accountStats);
    }

    @Test
    public void createMetricStatsMapAndCheckMetricPathHierarchyWithDimensionTest() {
        List<Metric> stats = kinesisAnalyticsMetricsProcessor
                .createMetricStatsMapForUpload(namespaceMetricStats);
        Metric metric = stats.get(0);
        String expectedMetricName = "Custom Metrics|AWS Kinesis Analytics|Appd|us-west-2|Application|Sample|Input|Id|2.1|testMetric";
        assertNotNull(metric);
        assertEquals(expectedMetricName, metric.getMetricPath());
    }
}
