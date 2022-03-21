package org.experiments;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.correlation.KendalCorrelation;
import org.evaluator.Evaluator;
import org.evaluator.Metric;
import org.evaluator.RetrievedResults;
import org.feedback.RelevanceModelConditional;
import org.feedback.RelevanceModelIId;
import org.qpp.PRFSpeciticity;
import org.qpp.UEFSpecificity;
import org.trec.TRECQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PRFSpecificityWorkflow extends NQCCalibrationWorkflow {
    PRFSpeciticity qppMethod;
    static final float ALPHA = 0.5f;
    static final int NUM_EXPANSION_TERMS = 10;

    PRFSpecificityWorkflow(String queryFile) throws Exception {
        super(queryFile);
        qppMethod = new PRFSpeciticity(Settings.getSearcher(), ALPHA);
    }

    TRECQuery expandQuery(TRECQuery q, TopDocs topDocs, int k) throws Exception {
        ScoreDoc[] sd = new ScoreDoc[k];
        System.arraycopy(topDocs.scoreDocs, 0, sd, 0, k);
        TopDocs subset = new TopDocs(topDocs.totalHits, sd);

        RelevanceModelIId rlm = null;
        try {
            rlm = new RelevanceModelConditional(
                    Settings.getSearcher(), q, subset, subset.scoreDocs.length);
        }
        catch (IOException ioex) { ioex.printStackTrace(); } catch (Exception ex) {
            Logger.getLogger(UEFSpecificity.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Post retrieval query expansion
        TRECQuery expandedQuery = rlm.expandQuery(q, NUM_EXPANSION_TERMS);
        System.out.println("Expanded qry: " + expandedQuery.getLuceneQueryObj());

        return expandedQuery;
    }

    public double computeCorrelation(List<TRECQuery> queries) throws Exception {
        final int qppTopK = Settings.getQppTopK();

        int numQueries = queries.size();
        double[] qppEstimates = new double[numQueries];
        double[] evaluatedMetricValues = new double[numQueries];

        List<TRECQuery> expandedQueries = new ArrayList<>(queries.size());

        for (TRECQuery query : queries) {
            TopDocs topDocs = topDocsMap.get(query.id);
            TRECQuery expandQuery = expandQuery(query, topDocs, qppTopK);
            System.out.println("Query: " + query + ", Expanded query: " + expandQuery);
            expandedQueries.add(expandQuery);
        }

        Map<String, TopDocs> postFdbkTopDocsMap = new HashMap<>();
        Evaluator postfdbk_evaluator = qppEvaluator.executeQueries(expandedQueries, sim,
                Settings.getNumWanted(), Settings.getQrelsFile(), Settings.RES_FILE, postFdbkTopDocsMap);

        int i = 0;
        for (TRECQuery query: expandedQueries) {
            evaluatedMetricValues[i] = evaluator.compute(query.id, Metric.AP);

            TopDocs postRLMTopDocs = postFdbkTopDocsMap.get(query.id);
            TopDocs topDocs = topDocsMap.get(query.id);

            qppEstimates[i] = (float)qppMethod.computeSpecificity(
                    query.getLuceneQueryObj(), postRLMTopDocs, topDocs, qppTopK);

            i++;
        }

        double corr = new KendalCorrelation().correlation(evaluatedMetricValues, qppEstimates);
        System.out.println(String.format("Kendall's = %.4f", corr));
        return corr;
    }

    public static void main(String[] args) {
        final String queryFile = "data/topics.401-450.xml";
        Settings.init("trec8.properties");

        try {
            PRFSpecificityWorkflow prfSpecificityWorkflow = new PRFSpecificityWorkflow(queryFile);
            System.out.println(String.format("Evaluating on %d queries", prfSpecificityWorkflow.queries.size()));
            prfSpecificityWorkflow.computeCorrelation(prfSpecificityWorkflow.queries);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
