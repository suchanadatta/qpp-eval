package org.experiments;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.correlation.KendalCorrelation;
import org.evaluator.Metric;
import org.evaluator.RetrievedResults;
import org.feedback.RelevanceModelConditional;
import org.feedback.RelevanceModelIId;
import org.qpp.PRFSpeciticity;
import org.qpp.UEFSpecificity;
import org.trec.TRECQuery;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PRFSpecificityWorkflow extends NQCCalibrationWorkflow {
    PRFSpeciticity qppMethod;
    final float ALPHA = 0.5f;

    PRFSpecificityWorkflow(String queryFile) throws Exception {
        super(queryFile);
        qppMethod = new PRFSpeciticity(Settings.getSearcher(), ALPHA);
    }

    TopDocs makeFbkDocs(Query q, TopDocs topDocs, int k) {
        ScoreDoc[] sd = new ScoreDoc[k];
        System.arraycopy(topDocs.scoreDocs, 0, sd, 0, k);
        TopDocs subset = new TopDocs(topDocs.totalHits, sd);

        RelevanceModelIId rlm = null;
        try {
            rlm = new RelevanceModelConditional(
                    Settings.getSearcher(), new TRECQuery(q), subset, subset.scoreDocs.length);
            rlm.computeFdbkWeights();
        }
        catch (IOException ioex) { ioex.printStackTrace(); } catch (Exception ex) {
            Logger.getLogger(UEFSpecificity.class.getName()).log(Level.SEVERE, null, ex);
        }

        return rlm.rerankDocs();
    }

    public double computeCorrelation(List<TRECQuery> queries) {
        final int qppTopK = Settings.getQppTopK();

        int numQueries = queries.size();
        double[] qppEstimates = new double[numQueries];
        double[] evaluatedMetricValues = new double[numQueries];

        int i = 0;
        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.id);
            TopDocs postRLMTopDocs = makeFbkDocs(query.luceneQuery, topDocs, qppTopK);

            evaluatedMetricValues[i] = evaluator.compute(query.id, Metric.AP);
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
