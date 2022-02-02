package org.experiments;

import org.trec.TRECQuery;
import java.util.List;

public class CorrelationAcrossModels {
    static SettingsLoader loader;
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        try {
            loader.init(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(),
                    loader.getCorrelationMetric(), loader.getSearcher(), loader.getNumWanted());
            List<TRECQuery> queries = qppEvaluator.constructQueries();
            qppEvaluator.relativeSystemRanksAcrossSims(queries);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
