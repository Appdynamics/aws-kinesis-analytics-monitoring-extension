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
import com.appdynamics.extensions.aws.metric.*;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessorHelper;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.aws.kinesis.analytics.util.Constants.NAMESPACE;

/**
 * Created by pradeep.nair on 8/3/18.
 */
public class KinesisAnalyticsMetricsProcessor implements MetricsProcessor {

    private static final Logger LOGGER = Logger.getLogger(KinesisAnalyticsMetricsProcessor.class);
    private List<IncludeMetric> includeMetrics;
    private List<Dimension> dimensions;

    KinesisAnalyticsMetricsProcessor(List<IncludeMetric> includeMetrics, List<Dimension> dimensions) {
        this.includeMetrics = includeMetrics;
        this.dimensions = dimensions;
    }

    @Override
    public List<AWSMetric> getMetrics(AmazonCloudWatch awsCloudWatch, String accountName, LongAdder
            awsRequestsCounter) {
        List<DimensionFilter> dimensionFilters = getDimensionFilters();
        MultiDimensionPredicate multiDimensionPredicate = new MultiDimensionPredicate(dimensions);
        return MetricsProcessorHelper.getFilteredMetrics(awsCloudWatch, awsRequestsCounter, NAMESPACE,
                includeMetrics, dimensionFilters, multiDimensionPredicate);
    }

    @Override
    public StatisticType getStatisticType(AWSMetric metric) {
        return MetricsProcessorHelper.getStatisticType(metric.getIncludeMetric(), includeMetrics);
    }

    @Override
    public List<Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats) {
        List<Metric> stats = new ArrayList<>();
        Map<String, String> dimensionDisplayNameMap = new HashMap<>();
        for (Dimension dimension : dimensions)
            dimensionDisplayNameMap.put(dimension.getName(), dimension.getDisplayName());
        if (namespaceMetricStats != null) {
            for (AccountMetricStatistics accountMetricStats : namespaceMetricStats.getAccountMetricStatisticsList()) {
                String accountPrefix = accountMetricStats.getAccountName();
                for (RegionMetricStatistics regionMetricStats : accountMetricStats.getRegionMetricStatisticsList()) {
                    String regionPrefix = regionMetricStats.getRegion();
                    for (MetricStatistic metricStats : regionMetricStats.getMetricStatisticsList()) {
                        Map<String, String> dimensionValueMap = new HashMap<>();
                        for (com.amazonaws.services.cloudwatch.model.Dimension dimension :
                                metricStats.getMetric().getMetric().getDimensions())
                            dimensionValueMap.put(dimension.getName(), dimension.getValue());
                        StringBuilder partialMetricPath = new StringBuilder();
                        buildMetricPath(partialMetricPath, true, accountPrefix, regionPrefix);
                        // reconstruct the metric hierarchy
                        arrangeMetricPathHierarchy(partialMetricPath, dimensionDisplayNameMap, dimensionValueMap);
                        String awsMetricName = metricStats.getMetric().getIncludeMetric().getName();
                        buildMetricPath(partialMetricPath, false, awsMetricName);
                        String fullMetricPath = metricStats.getMetricPrefix() + partialMetricPath;
                        if (metricStats.getValue() != null) {
                            Map<String, Object> metricProperties = new HashMap<>();
                            IncludeMetric metricWithConfig = metricStats.getMetric().getIncludeMetric();
                            metricProperties.put("alias", metricWithConfig.getAlias());
                            metricProperties.put("multiplier", metricWithConfig.getMultiplier());
                            metricProperties.put("aggregationType", metricWithConfig.getAggregationType());
                            metricProperties.put("timeRollUpType", metricWithConfig.getTimeRollUpType());
                            metricProperties.put("clusterRollUpType", metricWithConfig.getClusterRollUpType());
                            metricProperties.put("delta", metricWithConfig.isDelta());
                            Metric metric = new Metric(awsMetricName, Double.toString(metricStats.getValue()),
                                    fullMetricPath, metricProperties);
                            stats.add(metric);
                        } else {
                            LOGGER.debug(String.format("Ignoring metric [ %s ] which has value null", fullMetricPath));
                        }
                    }
                }
            }
        }
        return stats;
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

    /**
     * Builds the metric path, if {@code addSeparator} is {@code true} then the metric separator will be inseted
     * after each suffix.
     *
     * @param partialMetricPath Current {@code partialMetricPath}
     * @param addSeparator         Add metric addSeparator between each suffixes if {@code true}
     * @param suffixes            Value to be appended to {@code partialMetricPath}
     */
    private static void buildMetricPath(StringBuilder partialMetricPath, boolean addSeparator, String... suffixes) {
        for (String suffix : suffixes) {
            partialMetricPath.append(suffix);
            if (addSeparator) partialMetricPath.append(METRIC_PATH_SEPARATOR);
        }
    }

    /**
     * Arrange the metric path hierarchy to look like
     * {@code <Account>|<Region>|Application|<application_name>|<flow_type>|Id|<id>}
     * @param partialMetricPath         current metric path
     * @param dimensionDisplayNameMap   Dimension display name Map
     * @param dimensionValueMap         Dimension value Map
     */
    private static void arrangeMetricPathHierarchy(StringBuilder partialMetricPath, Map<String, String> dimensionDisplayNameMap,
                                                   Map<String, String> dimensionValueMap) {
        String applicationDimensionName = "Application";
        String applicationDimensionDispName = dimensionDisplayNameMap.get(applicationDimensionName);
        String flowDimensionName = "Flow";
        String idDimensionName = "Id";
        String idDimensionDispName = dimensionDisplayNameMap.get(idDimensionName);
        if (!StringUtils.hasText(applicationDimensionDispName)) applicationDimensionDispName = applicationDimensionName;
        if (!StringUtils.hasText(idDimensionDispName)) idDimensionDispName = idDimensionName;
        // <Account>|<Region>|Application|<application_name>
        buildMetricPath(partialMetricPath, true, applicationDimensionDispName,
                dimensionValueMap.get(applicationDimensionName));
        // <Account>|<Region>|Application|<application_name>|<flow_type>
        if (dimensionValueMap.get(flowDimensionName) != null)
            buildMetricPath(partialMetricPath, true, dimensionValueMap.get(flowDimensionName));
        // <Account>|<Region>|Application|<application_name>|<flow_type>|Id|<id>
        if (dimensionValueMap.get(idDimensionName) != null)
            buildMetricPath(partialMetricPath, true, idDimensionDispName, dimensionValueMap.get(idDimensionName));
    }
}
