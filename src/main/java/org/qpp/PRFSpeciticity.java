package org.qpp;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.correlation.OverlapStats;
import org.evaluator.RetrievedResults;
import org.feedback.*;
import org.trec.TRECQuery;

import java.util.HashMap;
import java.util.Map;

public class PRFSpeciticity {
    IndexSearcher searcher;
    NQCSpecificity qppMethod;
    float alpha;

    public PRFSpeciticity(IndexSearcher searcher,
                   float alpha /* the linear combination parameter (to be tuned) */) {
        this.searcher = searcher;
        this.qppMethod = new NQCSpecificity(searcher);
        this.alpha = alpha;
    }

    private float crossEntropy(TopDocs topDocs, RelevanceModelIId rlm) {
        float ce = 0;
        int i = 0;
        for (ScoreDoc sd: topDocs.scoreDocs) {
            ce += crossEntropy(topDocs, rlm, i);
            i++;
        }
        return ce/(float)topDocs.scoreDocs.length;
    }

    private float entropy_of_mean_doc_lm(TopDocs topDocs, RelevanceModelIId rlm) {
        RetrievedDocsTermStats retrievedDocsTermStats = rlm.getRetrievedDocsTermStats();
        PerDocTermVector mu = new PerDocTermVector(0); // docid is unused here

        for (RetrievedDocTermInfo retDocInfo : retrievedDocsTermStats.getTermStats().values()) { // for vocab of top
            String w = retDocInfo.getTerm();

            for (int i=0; i < topDocs.scoreDocs.length; i++) {
                PerDocTermVector docVector = retrievedDocsTermStats.getDocTermVecs(i);
                int tf = docVector.getTf(w);  /// tf(w)
                mu.addTermWt(w, tf);
            }
        }

        // normalize
        HashMap<String, RetrievedDocTermInfo> termStats = mu.getTermStats();
        int sum_tf = termStats.values().stream().map(x->x.getTf()).reduce(0, (a, b) -> a + b);

        float entropy = 0;
        for (RetrievedDocTermInfo tinfo: termStats.values()) {
            float p_x_i = tinfo.getTf()/(float)sum_tf;
            float log_p_x_i = (float)Math.log(1 + p_x_i);
            entropy += log_p_x_i;
        }

        // compute entropy
        return entropy/(float)termStats.values().size();
    }

    private float crossEntropy(TopDocs topDocs, RelevanceModelIId rlm, int i) {
        float klDiv = 0;
        float p_w_D;    // P(w|D) for this doc D
        final float EPSILON = 0.0001f;

        RetrievedDocsTermStats retrievedDocsTermStats = rlm.getRetrievedDocsTermStats();
        PerDocTermVector docVector = retrievedDocsTermStats.getDocTermVecs(i);

        // For each v \in V (vocab of top ranked documents)
        for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.getTermStats().entrySet()) {
            RetrievedDocTermInfo w = e.getValue();

            float ntf = docVector.getNormalizedTf(w.getTerm());
            if (ntf == 0)
                ntf = EPSILON; // avoid 0 error
            p_w_D = ntf;
            float p_w_C = w.getDf()/retrievedDocsTermStats.getSumDf();
            klDiv += w.getWeight() * (Math.log(w.getWeight()/p_w_D) - Math.log(w.getWeight()/p_w_C));
        }
        return klDiv;
    }

    public double computeSpecificity(Query q,
                         TopDocs topDocs,
                         TopDocs firstStepTopDocs,
                         int k) {
        // prob_d_second
        double prob_D_second = qppMethod.computeNQC(q, topDocs, k);

        try {
            // Now estimate a relevance model from the top-docs of D_init
            RelevanceModelIId rlm = new RelevanceModelConditional(searcher, new TRECQuery(q), firstStepTopDocs, k);
            rlm.computeFdbkWeights();

            float sim_D_second_Dinit = (float)OverlapStats.computeRBO(firstStepTopDocs, topDocs);
            float p_d_r_Dinit = crossEntropy(firstStepTopDocs, rlm);
            float r_Dinit = entropy_of_mean_doc_lm(firstStepTopDocs, rlm);

            float p_Dsecond_Dinit = 0;
            for (int i=0; i < topDocs.scoreDocs.length; i++) {
                p_Dsecond_Dinit += p_d_r_Dinit/r_Dinit;

            }

            return alpha*prob_D_second + (1-alpha)*sim_D_second_Dinit*p_Dsecond_Dinit;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
}
