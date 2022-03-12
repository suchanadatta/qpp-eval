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
import org.qpp.RLSSpecificity;
import org.trec.TRECQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RLSWorkflow extends NQCCalibrationWorkflow {

    RLSWorkflow(String queryFile) throws Exception {
        super(queryFile);
        qppMethod = new RLSSpecificity(Settings.getSearcher());
    }

    public static void main(String[] args) {
        final String queryFile = "data/topics.401-450.xml";
        Settings.init("trec8.properties");

        try {
            RLSWorkflow rlsWorkflow = new RLSWorkflow(queryFile);
            System.out.println(String.format("Evaluating on %d queries", rlsWorkflow.queries.size()));
            rlsWorkflow.computeCorrelation(rlsWorkflow.queries, rlsWorkflow.qppMethod);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
