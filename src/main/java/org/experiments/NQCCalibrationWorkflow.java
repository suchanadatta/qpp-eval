package org.experiments;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.correlation.KendalCorrelation;
import org.evaluator.Evaluator;
import org.evaluator.Metric;
import org.evaluator.RetrievedResults;
import org.qpp.NQCSpecificityCalibrated;
import org.qpp.QPPMethod;
import org.trec.TRECQuery;

import java.util.*;

class TrainTestInfo {
    List<TRECQuery> train;
    List<TRECQuery> test;
    static final int SEED = 31415;
    static Random r = new Random(SEED);

    TrainTestInfo(List<TRECQuery> parent, float trainRatio) {
        List<TRECQuery> listToShuffle = new ArrayList<>(parent);
        Collections.shuffle(listToShuffle); // shuffle the copy!

        int splitPoint = (int)(trainRatio * listToShuffle.size());
        train = listToShuffle.subList(0, splitPoint);
        test = listToShuffle.subList(splitPoint, listToShuffle.size());
    }

    List<TRECQuery> getTrain() { return train; }
    List<TRECQuery> getTest() { return test; }
}

public class NQCCalibrationWorkflow {
    Similarity sim = new LMDirichletSimilarity(1000);
    Map<String, TopDocs> topDocsMap = new HashMap<>();
    QPPEvaluator qppEvaluator;
    Evaluator evaluator;
    QPPMethod qppMethod;
    List<TRECQuery> queries;

    NQCCalibrationWorkflow(String queryFile) throws Exception {
        qppEvaluator = new QPPEvaluator(
                Settings.getProp(),
                Settings.getCorrelationMetric(), Settings.getSearcher(), Settings.getNumWanted());
        queries = qppEvaluator.constructQueries(queryFile);
        evaluator = qppEvaluator.executeQueries(queries, sim, Settings.getNumWanted(), Settings.getQrelsFile(), Settings.RES_FILE, topDocsMap);
    }

    public double computeCorrelation(List<TRECQuery> queries, QPPMethod qppMethod) {
        final int qppTopK = Settings.getQppTopK();
        int numQueries = queries.size();
        double[] qppEstimates = new double[numQueries]; // stores qpp estimates for the list of input queries
        double[] evaluatedMetricValues = new double[numQueries]; // stores GTs (AP/nDCG etc.) for the list of input queries

        int i = 0;
        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.id);
            evaluatedMetricValues[i] = evaluator.compute(query.id, Metric.AP);
            qppEstimates[i] = (float)qppMethod.computeSpecificity(
                    query.getLuceneQueryObj(), rr, topDocs, qppTopK);
            i++;
        }

        double corr = new KendalCorrelation().correlation(evaluatedMetricValues, qppEstimates);
        System.out.println(String.format("Kendall's = %.4f", corr));
        return corr;
    }

    public float[] calibrateParams(List<TRECQuery> trainQueries) {
        final int qppTopK = Settings.getQppTopK();
        final float[] alpha_choices = {/*0.25f, 0.5f, 1.0f,*/ 1.5f, /*2.0f*/};
        final float[] beta_choices = {0.25f, /*0.5f, 1.0f, 1.5f, 2.0f*/};
        final float[] gamma_choices = {/*0.25f,*/ 0.5f /*, 1.0f, 1.5f, 2.0f*/};
        float[] best_choice = new float[3]; // best (alpha, beta, gamma)
        double max_corr = 0;

        for (float alpha: alpha_choices) {
            for (float beta: beta_choices) {
                for (float gamma: gamma_choices) {
                    qppMethod = new NQCSpecificityCalibrated(Settings.getSearcher(), alpha, beta, gamma);
                    System.out.println(String.format("Executing NQC (%.2f, %.2f, %.2f)", alpha, beta, gamma));
                    double corr = computeCorrelation(trainQueries, qppMethod);
                    if (corr > max_corr) {
                        max_corr = corr;
                        best_choice[0] = alpha;
                        best_choice[1] = beta;
                        best_choice[2] = gamma;
                    }
                }
            }
        }
        return best_choice;
    }

    public double epoch() {
        final float TRAIN_RATIO = 0.5f;
        TrainTestInfo trainTestInfo = new TrainTestInfo(queries, TRAIN_RATIO);
        float[] tuned_params = calibrateParams(trainTestInfo.getTrain());
        QPPMethod qppMethod = new NQCSpecificityCalibrated(
                            Settings.getSearcher(),
                            tuned_params[0], tuned_params[1], tuned_params[2]);

        return computeCorrelation(queries, qppMethod);
    }

    public void averageAcrossEpochs() {
        final int NUM_EPOCHS = 2; // change it to 30!
        double avg = 0;
        for (int i=1; i <= NUM_EPOCHS; i++) {
            System.out.println("Random split: " + i);
            avg += epoch();
        }
        System.out.println(String.format("Result over %d runs of tuned 50:50 splits = %.4f", NUM_EPOCHS, avg/NUM_EPOCHS));
    }

    public static void main(String[] args) {
        final String queryFile = "data/topics.robust.all";
        Settings.init("init.properties");

        try {
            NQCCalibrationWorkflow nqcCalibrationWorkflow = new NQCCalibrationWorkflow(queryFile);
            nqcCalibrationWorkflow.averageAcrossEpochs();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
