package org.correlation;

import java.util.Map;

public interface QPPCorrelationMetric {
    public double correlation(double[] a, double[] b);
    public double correlation(Map<String, Double> a, Map<String, Double> b);
    public String name();
}
