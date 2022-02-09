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
        // in [0, 1]; no check to ensure that this may not be done twice!! but no harm in it!
        double[] n_pred = MinMaxNormalizer.normalize(pred);
        return rmse(gt, n_pred);
    }

    private double rmse(double[] truth, double[] pred) {
        double rmse = 0;
        for(int i=0; i < truth.length; i++) {
            rmse += Math.pow(pred[i] - truth[i], 2);
        }
        return (double)Math.sqrt(rmse/(double)truth.length);
    }
    
    @Override
    public String name() {
        return "rmse";
    }
}
