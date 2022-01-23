/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.correlation.MinMaxNormalizer;
import org.correlation.QPPCorrelationMetric;
import org.evaluator.Metric;
import org.qpp.QPPMethod;
import org.regressor.FitPolyRegressor;
import org.regressor.LearnRegressor;
import org.trec.TRECQuery;

/**
 *
 * @author suchana
 */

public class QPPPolynomialRegressor {
    static SettingsLoader       loader;
    Properties                  prop;
    int                         partition;
    int                         degree;
    static List<TRECQuery>      trainQueries;
    static List<TRECQuery>      testQueries;
    static String               qrelsFile;
    static String               queryFile;
    static String               resFileTrain;
    static String               resFileTest;
    static List<LearnRegressor> learnRegressor;
    static QPPCorrelationMetric correlationMetric;
    
    static final int SEED = 314159; // first six digits of pi - a beautiful seed!
    static Random rnd = new Random(SEED);
    
    public QPPPolynomialRegressor(Properties prop, int partition) {
        this.prop = prop;
        this.partition = partition;
        trainQueries = new ArrayList<>();
        testQueries = new ArrayList<>();
        learnRegressor = new ArrayList<>();
        qrelsFile = prop.getProperty("qrels.file");
        queryFile = prop.getProperty("query.file");
        resFileTrain = prop.getProperty("res.train");
        resFileTest = prop.getProperty("res.test");
        degree = 2;
    }
    
    public void randomSplit(List<TRECQuery> queries) {
        int splitQuery = (int) Math.floor(queries.size() * partition/100);
        
        for (int i=0; i<splitQuery; i++) {
            trainQueries.add(queries.get(i));
        }
        for (int i=splitQuery; i<queries.size(); i++) {
            testQueries.add(queries.get(i));
        }
        System.out.println("train : " + trainQueries.size() + "\t" + trainQueries.get(0).id);
        System.out.println("test : " + testQueries.size() + "\t" + testQueries.get(0).id);
    }
    
    public void fitRegressorTrainSetIndividualSetting(QPPMethod [] qppMethods, QPPEvaluator qppEvaluator,
            Similarity sim, int nwanted) throws Exception {
        for (QPPMethod qppMethod: qppMethods) { 
            System.out.println("\nQPP method : " + qppMethod.name());
            for (Metric m : Metric.values()){
                System.out.println("METRIC : " + m.name());
                LearnRegressor lr = new LearnRegressor();
                lr.setQppMethod(qppMethod.name());
                lr.setMetric(m.name());

                double [] corrMeasure = qppEvaluator.evaluate(trainQueries, sim, m, nwanted, resFileTrain);
                double [] qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, trainQueries, corrMeasure, m, resFileTrain);

                FitPolyRegressor fpr = new FitPolyRegressor(degree);
                double[] coeff = fpr.fitCurve(corrMeasure, qppEstimates);
                lr.setCoeff(coeff);
                learnRegressor.add(lr);
            }
        } 
        System.out.println("No. of regressors trained : " + learnRegressor.size());
    }
    
    public void predictCorrelationTestSetIndividual(QPPMethod [] qppMethods, QPPEvaluator qppEvaluator,
            Similarity sim, int nwanted) throws Exception {
        for (QPPMethod qppMethod: qppMethods) { 
            System.out.println("\nQPP method : " + qppMethod.name());
            for (Metric m : Metric.values()){
                System.out.println("METRIC : " + m.name());
                
                Map<String, Double> corrMeasure = qppEvaluator.evaluateMap(testQueries, sim, m, nwanted, resFileTest);
                corrMeasure = MinMaxNormalizer.normalize(corrMeasure);
                
                Map<String, Double> qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, testQueries, corrMeasure, m, resFileTest);
                Map<String, Double> qppEstimateWithRegressor = new HashMap<>();
                
                for (LearnRegressor lr : learnRegressor) {
                    if (lr.getQppMethod().equalsIgnoreCase(qppMethod.name()) && 
                            lr.getMetric().equalsIgnoreCase(m.name())) {
                        System.out.println("======= " + lr.getMetric() + "\t" + lr.qppMethod + "=======");
                        for (Map.Entry<String, Double> spec : qppEstimates.entrySet()) {
                            qppEstimateWithRegressor.put(spec.getKey(), lr.getSlope() * spec.getValue() + lr.getyIntercept());
                            qppEstimateWithRegressor.put(spec.getKey(), lr.getCoeff()[degree] + 
                                    lr.getCoeff()[degree-1] * Math.pow(spec.getValue(), degree-1) +
                                    lr.getCoeff()[degree-2] * Math.pow(spec.getValue(), degree-2));
                        } 
                    }
                }
                
                qppEstimateWithRegressor = MinMaxNormalizer.normalize(qppEstimateWithRegressor);  
                double correlation = qppEvaluator.measureCorrelation(corrMeasure, qppEstimateWithRegressor);
                System.out.println("CORRELATION : " + correlation);
            }
        }  
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        try {
            loader = new SettingsLoader(args[0]);
            QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(), 
                    loader.getCorrelationMetric(), loader.getSearcher(), 
                    loader.getNumWanted());
            QPPPolynomialRegressor polreg = new QPPPolynomialRegressor(loader.getProp(), loader.getTrainTestSplits());

            List<TRECQuery> queries = qppEvaluator.constructQueries();
            System.out.println("QUERIES : " + queries.size() + "\t" + queries.get(0).id);
            
            // create train:test splits
            Collections.shuffle(queries, rnd);
            System.out.println("SHUFFLED : " + queries.size() + "\t" + queries.get(0).id);
            polreg.randomSplit(queries);
            
            QPPMethod [] qppMethods = qppEvaluator.qppMethods();
            System.out.println("QPPMETHODS : " + qppMethods.length);
            
            Similarity sim = new LMDirichletSimilarity(1000);
//            Similarity sim = new LMJelinekMercerSimilarity(0.6f);
//            Similarity sim = new BM25Similarity(1.5f, 0.75f);
//            Similarity sim = new BM25Similarity(0.7f, 0.3f);
            
            final int nwanted = loader.getNumWanted();
            final int qppTopK = loader.getQppTopK();
            
            // learn individual regressor learning parameters for individual qpp estimators
            polreg.fitRegressorTrainSetIndividualSetting(qppMethods, qppEvaluator, sim, nwanted);
            
            // predict test set values based on individual learning parameters 
            polreg.predictCorrelationTestSetIndividual(qppMethods, qppEvaluator, sim, nwanted);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }   
}