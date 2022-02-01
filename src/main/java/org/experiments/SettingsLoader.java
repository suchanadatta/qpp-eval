package org.experiments;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.correlation.*;
import org.evaluator.Metric;
import org.qpp.*;


public class SettingsLoader {

    static Map<String, QPPCorrelationMetric> corrMetrics;
    static Map<String, QPPMethod>            qppMethods;
    static Map<String, Metric>            retEvalMetrics;

    static int                               qppTopK;
    boolean                                  boolIndexExists;
    static Properties                               prop;
    IndexReader                              reader;
    IndexSearcher                            searcher;
    int                                      numWanted;
    String                                   queryFile;
    String                                   resFile;
    String                                   qrelsFile;
    static boolean initialized = false;
    
    static void init(Properties prop, IndexSearcher searcher) {
        if (initialized)
            return;

        SettingsLoader.prop = prop;

        corrMetrics = new HashMap<>();
        corrMetrics.put("r", new PearsonCorrelation());
        corrMetrics.put("rho", new SpearmanCorrelation());
        corrMetrics.put("tau", new KendalCorrelation());
        corrMetrics.put("qsim", new QuantizedSimCorrelation(Integer.parseInt(prop.getProperty("qsim.numintervals", "5"))));
        corrMetrics.put("qsim_strict", new QuantizedStrictMatchCorrelation(Integer.parseInt(prop.getProperty("qsim.numintervals", "5"))));
        corrMetrics.put("pairacc", new PairwiseAccuracyMetric());
        corrMetrics.put("classacc", new QuantizedClassAccuracy(Integer.parseInt(prop.getProperty("qsim.numintervals", "5"))));
        corrMetrics.put("rmse", new RmseCorrelation());

        retEvalMetrics = new HashMap<>();
        retEvalMetrics.put("ap", Metric.AP);
        retEvalMetrics.put("p_10", Metric.AP);
        retEvalMetrics.put("recall", Metric.AP);
        retEvalMetrics.put("ndcg", Metric.AP);

        qppMethods = new HashMap<>();
        qppMethods.put("avgidf", new AvgIDFSpecificity(searcher));
        qppMethods.put("nqc", new NQCSpecificity(searcher));
        qppMethods.put("wig", new WIGSpecificity(searcher));
        qppMethods.put("clarity", new ClaritySpecificity(searcher));
        qppMethods.put("uef_nqc", new UEFSpecificity(new NQCSpecificity(searcher)));
        qppMethods.put("uef_wig", new UEFSpecificity(new WIGSpecificity(searcher)));
        qppMethods.put("uef_clarity", new UEFSpecificity(new ClaritySpecificity(searcher)));
        
        qppTopK = Integer.parseInt(prop.getProperty("qpp.numtopdocs"));

        initialized = true;
    }

    public SettingsLoader(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));

        File indexDir = new File(prop.getProperty("index.dir"));
        System.out.println("Running queries against index: " + indexDir.getPath());

        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        numWanted = Integer.parseInt(prop.getProperty("retrieve.num_wanted", "100"));

        init(prop, searcher);
    }

    public Properties getProp() { return prop; }

    public int getNumWanted() { return numWanted; }
    public int getQppTopK() { return qppTopK; }

    public QPPCorrelationMetric getCorrelationMetric() {
        String key = prop.getProperty("qpp.metric");
        return corrMetrics.get(key);
    }

    public IndexSearcher getSearcher() { return searcher; }

    public QPPMethod getQPPMethod() {
        String key = prop.getProperty("qpp.method");
        return qppMethods.get(key);
    }
    
    public int getTrainTestSplits() {
        int splits = Integer.parseInt(prop.getProperty("qpp.splits"));
        return splits;
    }

    public static Metric getRetEvalMetric() {
        return retEvalMetrics.get(prop.getProperty("reteval.metric"));
    }
}
