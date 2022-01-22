/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.regressor;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.correlation.MinMaxNormalizer;

/**
 *
 * @author suchana
 */

public class FitLinearRegressor {
    SimpleRegression re;
    
    public FitLinearRegressor() {
        re = new SimpleRegression();
    }   
    
    public void fitLine (double [] gt, double [] pred) {
        double[] n_gt = MinMaxNormalizer.normalize(gt); // in [0, 1]
        double[] n_pred = MinMaxNormalizer.normalize(pred); // in [0, 1]
        for (int i=0; i<gt.length; i++) {
            re.addData(n_pred[i], n_gt[i]);
        }
    }
    
    public double learnSlope() {
        System.out.println("SLOPE : " + re.getSlope());
        return re.getSlope();
    }
    
    public double learnIntercept() {
        System.out.println("INTERCEPT : " + re.getIntercept());
        return re.getIntercept();
    }
}
