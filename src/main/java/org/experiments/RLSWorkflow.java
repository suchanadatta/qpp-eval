package org.experiments;

import org.qpp.RLSSpecificity;

public class RLSWorkflow extends NQCCalibrationWorkflow {
    
    int num_fdbk;
    int num_sample;
    int top_refs;

    RLSWorkflow(String queryFile, int num_fdbk, int num_sample, int top_refs) throws Exception {
        super(queryFile);
        this.num_fdbk = num_fdbk;
        this.num_sample = num_sample;
        this.top_refs = top_refs;
        qppMethod = new RLSSpecificity(Settings.getSearcher(), num_fdbk, num_sample, top_refs);
    }

    public static void main(String[] args) {
        final String queryFile = "data/topics.robust.all";
        Settings.init("qpp.properties");
        final int[] NUM_FDBK_TOP_DOCS = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        final int[] NUM_SAMPLES = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};  // Number of reference lists, i.e. number of queries to form
        final int L; // number of reference lists for both pos and neg (corresponding to the lowest p values)

        try {
            for (int num_fdbk: NUM_FDBK_TOP_DOCS) {
                for (int num_sample: NUM_SAMPLES) {
                    for (int l = (int)Math.floor(num_sample/2); l>0; l--){
                        System.out.println(String.format("RUN STARTS FOR num_fdbk = %d num_sample = %d, top refs = %d", num_fdbk, num_sample, l));
                        RLSWorkflow rlsWorkflow = new RLSWorkflow(queryFile, num_fdbk, num_sample, l);
                        System.out.println(String.format("Evaluating on %d queries", rlsWorkflow.queries.size()));
                        rlsWorkflow.computeCorrelation(rlsWorkflow.queries, rlsWorkflow.qppMethod);
                    }
                }
            }            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
