/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.correlation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author suchana
 */

public class RmseCorrelation implements QPPCorrelationMetric {
    
    @Override
    public double correlation(double[] gt, double[] pred) {
        double[] n_gt = MinMaxNormalizer.normalize(gt); // in [0, 1]
        double[] n_pred = MinMaxNormalizer.normalize(pred); // in [0, 1]

        return rmse(n_gt, n_pred);
    }

    public double rmse(double[] truth, double[] pred) {
        double rmse = 0;        
        for(int i=0; i < truth.length; i++) {
            rmse += Math.pow(pred[i] - truth[i], 2);
        }        
        return (double)Math.sqrt(rmse/(double)truth.length);
    }
    
    @Override
    public double correlation(Map<String, Double> gt, Map<String, Double> pred) {
        List<Double> gtValues = new ArrayList<>(gt.values());
        double[] gtArr = gtValues.stream().mapToDouble(Double::doubleValue).toArray();
        
        List<Double> predValues = new ArrayList<>(pred.values());
        double[] predArr = predValues.stream().mapToDouble(Double::doubleValue).toArray();
        
        return rmse(gtArr, predArr);
    }

    @Override
    public String name() {
        return "rmse";
    }
}
