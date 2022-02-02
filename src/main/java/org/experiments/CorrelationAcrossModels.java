package org.experiments;

import org.trec.TRECQuery;
import java.util.List;

public class CorrelationAcrossModels {

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        try {
            Settings.init(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(Settings.getProp(),
                    Settings.getCorrelationMetric(), Settings.getSearcher(), Settings.getNumWanted());
            List<TRECQuery> queries = qppEvaluator.constructQueries();
            qppEvaluator.relativeSystemRanksAcrossSims(queries);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
