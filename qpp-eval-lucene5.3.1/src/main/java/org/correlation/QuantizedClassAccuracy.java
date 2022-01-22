package org.correlation;

import java.util.HashMap;
import java.util.Map;

public class QuantizedClassAccuracy implements QPPCorrelationMetric {
    
    int numIntervals;
    double delta;
    
    public QuantizedClassAccuracy(int numIntervals) {
        this.numIntervals = numIntervals;
        delta = 1/(double)numIntervals;
    }
    
    @Override
    public double correlation(Map<String, Double> gt, Map<String, Double> pred) {
        Map<String, Integer> q_a = quantizeInUnitInterval(MinMaxNormalizer.normalize(gt)); // in [0, 1]
//        System.out.println("After quant : " + q_a);
        Map<String, Integer> q_b = quantizeInUnitInterval(MinMaxNormalizer.normalize(pred));
//        System.out.println("After quant : " + q_b);

        return quantizedAccuracy(q_a, q_b);
    }
    
    Map<String, Integer> quantizeInUnitInterval(Map<String, Double> x) {        
        Map<String, Integer> quantMap = new HashMap<>();
        
        x.entrySet().stream().forEach((entry) -> {
            quantMap.put(entry.getKey(), (int)(entry.getValue()/delta));
        });
        
        return quantMap;
    }
    
    public double quantizedAccuracy(Map<String, Integer> truth, Map<String, Integer> pred) {         
        double acc = 0;        
        for (String qid : truth.keySet()) {
           int pred_label = pred.get(qid);
           int ref_label = truth.get(qid);
           
           acc += pred_label == ref_label? 1:0;
        }
        return acc/(double)truth.size();        
    }
    
    @Override
    public double correlation(double[] a, double[] b) {
        return 0;
    }

    @Override
    public String name() {
        return "classacc";
    }
}
