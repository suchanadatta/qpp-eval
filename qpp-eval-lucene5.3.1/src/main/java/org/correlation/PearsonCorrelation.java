package org.correlation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

public class PearsonCorrelation implements QPPCorrelationMetric {
    @Override
    public double correlation(double[] gt, double[] pred) {
        return new PearsonsCorrelation().correlation(gt, pred);
    }
    
    @Override
    public double correlation(Map<String, Double> gt, Map<String, Double> pred) {
        List<Double> gtValues = new ArrayList<>(gt.values());
        double[] gtArr = gtValues.stream().mapToDouble(Double::doubleValue).toArray();
        
        List<Double> predValues = new ArrayList<>(pred.values());
        double[] predArr = predValues.stream().mapToDouble(Double::doubleValue).toArray();
        
        return new PearsonsCorrelation().correlation(gtArr, predArr);
    }

    @Override
    public String name() {
        return "r";
    }
}
