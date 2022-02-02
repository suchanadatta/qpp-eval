package org.experiments;

import org.trec.TRECQuery;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CorrelationAcrossMetrics {
    static final int SEED = 31416;

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        final int cutOff = 100;
        try {
            Settings.init(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(
                    Settings.getProp(), Settings.getCorrelationMetric(),
                    Settings.getSearcher(), Settings.getNumWanted());

            List<TRECQuery> queries = qppEvaluator.constructQueries();

            boolean toTransform = Settings.getCorrelationMetric().name().equals("rmse") &&
                    Boolean.parseBoolean(Settings.getProp().getProperty("transform_scores", "false"));

            if (toTransform) {
                Collections.shuffle(queries, new Random(SEED));
                int splitIndex = (int) (queries.size() * Settings.getTrainPercentage() / 100);
                List<TRECQuery> trainQueries = queries.subList(0, splitIndex);
                List<TRECQuery> testQueries = queries.subList(splitIndex, queries.size());

                // for a single metric if want to use different cutoff (e.g. ap@10/ap@100...)
                qppEvaluator.relativeSystemRanksAcrossMetrics(Settings.getRetModel(), trainQueries, testQueries, cutOff);
            }
            else {
                qppEvaluator.relativeSystemRanksAcrossMetrics(Settings.getRetModel(), queries, cutOff);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
