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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.appdynamics.extensions.aws.config.Dimension;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessorHelper;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by pradeep.nair on 8/3/18.
 */
public class KinesisAnalyticsMetricsProcessor implements MetricsProcessor {

    private static final Logger LOGGER = Logger.getLogger(KinesisAnalyticsMetricsProcessor.class);
    private static final String NAMESPACE = "AWS/KinesisAnalytics";
    private List<IncludeMetric> includeMetrics;
    private List<Dimension> dimensions;

    KinesisAnalyticsMetricsProcessor(List<IncludeMetric> includeMetrics, List<Dimension> dimensions) {
        this.includeMetrics = includeMetrics;
        this.dimensions = dimensions;
    }

    @Override
    public List<AWSMetric> getMetrics(AmazonCloudWatch awsCloudWatch, String accountName, LongAdder awsRequestsCounter) {
        List<DimensionFilter> dimensionFilters = getDimensionFilters();
        MultiDimensionPredicate multiDimensionPredicate = new MultiDimensionPredicate(dimensions);
        return  MetricsProcessorHelper.getFilteredMetrics(awsCloudWatch, awsRequestsCounter, NAMESPACE,
                includeMetrics, dimensionFilters, multiDimensionPredicate);
    }

    @Override
    public StatisticType getStatisticType(AWSMetric metric) {
        return MetricsProcessorHelper.getStatisticType(metric.getIncludeMetric(), includeMetrics);
    }

    @Override
    public List<Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats) {
        Map<String, String> dimensionToMetricPathNameDictionary = new HashMap<>();
        for (Dimension dimension : dimensions) {
            dimensionToMetricPathNameDictionary.put(dimension.getName(), dimension.getDisplayName());
        }
        return MetricsProcessorHelper.createMetricStatsMapForUpload(namespaceMetricStats,
                dimensionToMetricPathNameDictionary, false);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    private List<DimensionFilter> getDimensionFilters() {
        List<DimensionFilter> dimensionFilters = new ArrayList<>();
        for (Dimension dimension : dimensions) {
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.withName(dimension.getName());
            dimensionFilters.add(dimensionFilter);
        }
        return dimensionFilters;
    }
}
