package org.experiments;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.lucene.search.similarities.Similarity;
import org.correlation.MinMaxNormalizer;
import org.evaluator.Metric;
import org.qpp.QPPMethod;
import org.trec.TRECQuery;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CorrelationAcrossMetrics {
    static final int SEED = 31416;
    static SettingsLoader loader;

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        final int cutOff = 100;
        try {
            loader.init(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(
                    loader.getProp(), loader.getCorrelationMetric(), 
                    loader.getSearcher(), loader.getNumWanted());

            List<TRECQuery> queries = qppEvaluator.constructQueries();

            boolean toTransform = loader.getCorrelationMetric().equals("rmse") &&
                    Boolean.parseBoolean(loader.getProp().getProperty("regression", "false"));

            if (toTransform) {
                Collections.shuffle(queries, new Random(SEED));
                int splitIndex = (int) (queries.size() * SettingsLoader.getTrainPercentage() / 100);
                List<TRECQuery> trainQueries = queries.subList(0, splitIndex);
                List<TRECQuery> testQueries = queries.subList(splitIndex, queries.size());

                // for a single metric if want to use different cutoff (e.g. ap@10/ap@100...)
                qppEvaluator.relativeSystemRanksAcrossMetrics(SettingsLoader.getRetModel(), trainQueries, testQueries, cutOff);
            }
            else {
                qppEvaluator.relativeSystemRanksAcrossMetrics(SettingsLoader.getRetModel(), queries, cutOff);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
