package org.qpp;

import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.evaluator.RetrievedResults;
import org.experiments.QPPEvaluator;
import org.experiments.Settings;
import org.feedback.RelevanceModelConditional;
import org.feedback.RelevanceModelIId;
import org.feedback.RetrievedDocTermInfo;
import org.feedback.RetrievedDocsTermStats;
import org.trec.TRECQuery;
import org.evaluator.Evaluator;

import java.io.IOException;
import java.sql.Ref;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.inference.TTest;

class QueryAndTopDocs {
    Query q;
    TopDocs topDocs;

    QueryAndTopDocs(Query q, TopDocs topDocs) {
        this.q = q;
        this.topDocs = topDocs;
    }
}

class RefListInfo {
    List<QueryAndTopDocs> posList;  // {q: NQC(q) > NQC(q_0) and scores are significantly different}
    List<QueryAndTopDocs> negList;  // // {q: NQC(q) <= NQC(q_0) and scores are significantly different}

    RefListInfo() {
        posList = new ArrayList<>();
        negList = new ArrayList<>();
    }
}

public class RLSSpecificity implements QPPMethod {
    NQCSpecificity nqcSpecificity;
    IndexSearcher searcher;
    static final int NUM_FDBK_TOP_DOCS = 10;
    static final int NUM_SAMPLES = 20;  // Number of reference lists, i.e. number of queries to form
    static final int L = 3; // number of reference lists for both pos and neg (corresponding to the lowest p values)
    static float ALPHA = 0.05f; // threshold for rejection of null hypothesis

    static Random rnd = new Random(Settings.SEED);

    public RLSSpecificity(IndexSearcher searcher) {
        this.searcher = searcher;
        nqcSpecificity = new NQCSpecificity(searcher);
    }

    // Different to UEF, the different ranked lists are not to be sampled as subsets
    // of the initial list but are to be constructed by retrieving on augmented queries
    Set<RetrievedDocTermInfo> topFdbkTerms(TRECQuery q, TopDocs topDocs) throws Exception {
        RelevanceModelIId rlm = new RelevanceModelConditional(searcher, q, topDocs, NUM_FDBK_TOP_DOCS);
        rlm.computeFdbkWeights();
        RetrievedDocsTermStats stats = rlm.getRetrievedDocsTermStats();
        List<RetrievedDocTermInfo> termWts =
                stats.getTermStats()
                .values().stream()
                .sorted(RetrievedDocTermInfo::compareTo)
                .collect(Collectors.toList());
        return new HashSet<>(termWts.subList(0, NUM_SAMPLES));
    }

    // Returns q + t as a new query
    Query createAugmented(TRECQuery q, String newTerm) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        Set<Term> terms = q.getQueryTerms(searcher);
        String fieldName = null;
        TermQuery tq;

        for (Term term: terms) {
            fieldName = term.field();
            tq = new TermQuery(term);
            qb.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
        }
        tq = new TermQuery(new Term(fieldName, newTerm));
        qb.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
        return qb.build();
    }

    List<Query> generateAugmentedQueries(TRECQuery q, TopDocs topDocs) throws Exception {
        Set<RetrievedDocTermInfo> topTermWts = topFdbkTerms(q, topDocs);
        List<Query> augmented_queries = new ArrayList<>();

        for (RetrievedDocTermInfo termInfo: topTermWts) {
            String term = termInfo.getTerm();
            Query augmented_query = createAugmented(q, term);
            augmented_queries.add(augmented_query);
        }
        return augmented_queries;
    }

    List<QueryAndTopDocs> getReferenceLists(Query q, TopDocs topDocs, int numWanted) throws Exception {
        List<Query> augmented_queries = generateAugmentedQueries(new TRECQuery(q), topDocs);
        List<QueryAndTopDocs> refLists = new ArrayList<>();
        for (Query q_augmented: augmented_queries) {
            QueryAndTopDocs queryAndTopDocs = new QueryAndTopDocs(q_augmented, searcher.search(q_augmented, numWanted));
            refLists.add(queryAndTopDocs);
        }
        return refLists;
    }

    void computeNQCEstimates(List<TRECQuery> queries) throws Exception {
        QPPEvaluator qppEvaluator = new QPPEvaluator(
                Settings.getProp(),
                Settings.getCorrelationMetric(), Settings.getSearcher(), Settings.getNumWanted());
        Evaluator evaluator = qppEvaluator.executeQueries(queries,
                searcher.getSimilarity(), Settings.getNumWanted(), Settings.getQrelsFile(),
                Settings.RES_FILE, null);

    }

    // return 1 if to be inserted in +ve list
    boolean compareSpecificities(Query orig_query, TopDocs orig_q_topDocs,
                                 Query augmented_query, TopDocs augmented_q_topDocs) {
        double nqc_orig_query = nqcSpecificity.computeNQC(orig_query, orig_q_topDocs, Settings.getQppTopK());
        double nqc_augmented_query = nqcSpecificity.computeNQC(augmented_query, augmented_q_topDocs, Settings.getQppTopK());
        return nqc_augmented_query > nqc_orig_query;
    }

    // retList is the list for the original query
    RefListInfo makeRefLists(Query orig_q, TopDocs retList,
                           List<QueryAndTopDocs> refLists) {
        RefListInfo refListInfo = new RefListInfo();

        double [] original_query_rsv, augmented_query_rsv;
        original_query_rsv = Arrays.stream(retList.scoreDocs)
                .map(scoreDoc -> scoreDoc.score)
                .mapToDouble(d -> d)
                .toArray();

        for (QueryAndTopDocs queryAndTopDocs: refLists) {
            Query aug_query = queryAndTopDocs.q;
            augmented_query_rsv = Arrays.stream(queryAndTopDocs.topDocs.scoreDocs)
                    .map(scoreDoc -> scoreDoc.score)
                    .mapToDouble(d -> d)
                    .toArray();

            boolean rejected = new TTest().pairedTTest(original_query_rsv, augmented_query_rsv, ALPHA);
            if (rejected) { // scores come from different means; hence they are different significantly
                boolean pos = compareSpecificities(orig_q, retList, aug_query, queryAndTopDocs.topDocs);
                if (pos) {
                    refListInfo.posList.add(new QueryAndTopDocs(aug_query, queryAndTopDocs.topDocs));
                }
                else {
                    refListInfo.negList.add(new QueryAndTopDocs(aug_query, queryAndTopDocs.topDocs));
                }
            }
        }
        return refListInfo;
    }

    @Override
    public double computeSpecificity(Query q, RetrievedResults retInfo, TopDocs topDocs, int k) {
        try {
            List<QueryAndTopDocs> refLists = getReferenceLists(q, topDocs, topDocs.scoreDocs.length);
            RefListInfo refListInfo = makeRefLists(q, topDocs, refLists);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public String name() {
        return "rls";
    }
}
