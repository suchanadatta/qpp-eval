package org.experiments;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.evaluator.ResultTuple;
import org.evaluator.RetrievedResults;
import org.qpp.RLSSpecificity;
import org.trec.FieldConstants;

import java.io.*;
import java.util.*;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    RLSWorkflow(String queryFile, String resFile, int num_fdbk, int num_sample, int top_refs) throws Exception {
        super(queryFile, resFile);
        this.num_fdbk = num_fdbk;
        this.num_sample = num_sample;
        this.top_refs = top_refs;
        qppMethod = new RLSSpecificity(Settings.getSearcher(), num_fdbk, num_sample, top_refs);
    }

    public static void main(String[] args) {
        //final String queryFile = "data/topics.robust.all";
        final String queryFile = "data/topics.401-450.xml";
        final String RES_FILE = "res.txt";

        Settings.init("qpp.properties");
        //final int[] NUM_FDBK_TOP_DOCS = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
        //final int[] NUM_SAMPLES = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};  // Number of reference lists, i.e. number of queries to form
        final int[] NUM_FDBK_TOP_DOCS = {10};
        final int[] NUM_SAMPLES = {30};  // Number of reference lists, i.e. number of queries to form
        final int L = 5; // number of reference lists for both pos and neg (corresponding to the lowest p values)

        try {
            for (int num_fdbk: NUM_FDBK_TOP_DOCS) {
                for (int num_sample: NUM_SAMPLES) {
                    //for (int l = (int)Math.floor(num_sample/2); l>0; l--) {
                        int l = L;
                        System.out.println(String.format("RUN STARTS FOR num_fdbk = %d num_sample = %d, top refs = %d", num_fdbk, num_sample, l));
                        RLSWorkflow rlsWorkflow = new RLSWorkflow(queryFile, RES_FILE, num_fdbk, num_sample, l);

                        System.out.println(String.format("Evaluating on %d queries", rlsWorkflow.queries.size()));
                        rlsWorkflow.computeCorrelation(rlsWorkflow.queries, rlsWorkflow.qppMethod);
                    //}
                }
            }            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
