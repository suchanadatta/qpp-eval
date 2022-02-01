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

import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.correlation.MinMaxNormalizer;
import org.correlation.QPPCorrelationMetric;
import org.evaluator.Metric;
import org.qpp.QPPMethod;
import org.regressor.FitLinearRegressor;
import org.regressor.RegressionLearner;
import org.trec.TRECQuery;

/**
 *
 * @author suchana
 */

class CorrelationPair {
    float withoutTransformation;
    float withTransformation;

    CorrelationPair(float x, float y) {
        withoutTransformation = x;
        withTransformation = y;
    }
}

public class QPPLinearRegressor {
    static SettingsLoader       loader;
    Properties                  prop;
    int                         partition;
    static List<TRECQuery>      trainQueries;
    static List<TRECQuery>      testQueries;
    static String               qrelsFile;
    static String               queryFile;
    static String               resFileTrain;
    static String               resFileTest;
    RegressionLearner lr;

    static final int SEED = 314159; // first six digits of pi - a beautiful seed!
    static Random rnd = new Random(SEED);
    
    public QPPLinearRegressor(Properties prop, int partition) {
        this.prop = prop;
        this.partition = partition;
        trainQueries = new ArrayList<>();
        testQueries = new ArrayList<>();
        qrelsFile = prop.getProperty("qrels.file");
        queryFile = prop.getProperty("query.file");
        resFileTrain = prop.getProperty("res.train");
        resFileTest = prop.getProperty("res.test");
        lr = new RegressionLearner();
    }
    
    public void randomSplit(List<TRECQuery> queries) {
        int splitQuery = (int) Math.floor(queries.size() * partition/100);

        for (int i=0; i<splitQuery; i++) {
            trainQueries.add(queries.get(i));
        }
        for (int i=splitQuery; i<queries.size(); i++) {
            testQueries.add(queries.get(i));
        }
    }
    
    public void fit(QPPEvaluator qppEvaluator,
              QPPMethod qppMethod, Metric m,
              Similarity sim, int nwanted) throws Exception {

        double [] corrMeasure = qppEvaluator.evaluate(trainQueries, sim, m, nwanted, resFileTrain);
        double [] qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, trainQueries, corrMeasure, m, resFileTrain);

        FitLinearRegressor fr = new FitLinearRegressor();
        fr.fitLine(corrMeasure, qppEstimates);
        lr.setSlope(fr.getSlope());
        lr.setyIntercept(fr.getIntercept());
    }
    
    public CorrelationPair predict(QPPEvaluator qppEvaluator, QPPMethod qppMethod, Metric m,
                        Similarity sim, int nwanted) throws Exception {

        Map<String, Double> corrMeasure = qppEvaluator.evaluateMap(testQueries, sim, m, nwanted, resFileTest);
        Map<String, Double> corrMeasureNormalized = MinMaxNormalizer.normalize(corrMeasure);

        Map<String, Double> qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, testQueries, corrMeasure, m, resFileTest);
        float x = (float)qppEvaluator.measureCorrelation(corrMeasure, qppEstimates);

        Map<String, Double> qppEstimateWithRegressor = new HashMap<>();
        for (Map.Entry<String, Double> spec : qppEstimates.entrySet()) {
            qppEstimateWithRegressor.put(spec.getKey(), lr.getSlope() * spec.getValue() + lr.getyIntercept());
        }

        qppEstimateWithRegressor = MinMaxNormalizer.normalize(qppEstimateWithRegressor);
        float y = (float)qppEvaluator.measureCorrelation(corrMeasureNormalized, qppEstimateWithRegressor);

        return new CorrelationPair(x, y);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        loader = new SettingsLoader(args[0]);
        QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(),
                loader.getCorrelationMetric(), loader.getSearcher(),
                loader.getNumWanted());
        QPPLinearRegressor qppreg = new QPPLinearRegressor(loader.getProp(), loader.getTrainTestSplits());

        List<TRECQuery> queries = qppEvaluator.constructQueries();
        // create train:test splits
        Collections.shuffle(queries, rnd);
        qppreg.randomSplit(queries);

        QPPMethod [] qppMethods = qppEvaluator.qppMethods();
        Similarity sim = new LMDirichletSimilarity(1000);

        /* Experiment configurations */
        final int nwanted = loader.getNumWanted();
        final int qppTopK = loader.getQppTopK();
        QPPMethod method = loader.getQPPMethod();

        // learn individual regressor learning parameters for individual qpp estimators
        System.out.println(String.format("Fitting linear regression on %s scores", SettingsLoader.getRetEvalMetric()));
        qppreg.fit(qppEvaluator, method, SettingsLoader.getRetEvalMetric(), sim, nwanted);

        // predict test set values based on individual learning parameters
        CorrelationPair cp = qppreg.predict(qppEvaluator, method, SettingsLoader.getRetEvalMetric(), sim, nwanted);
        System.out.println(String.format("CORRELATION (%s): %.4f %.4f",
                loader.getCorrelationMetric().name(), cp.withoutTransformation, cp.withTransformation));
    }
}