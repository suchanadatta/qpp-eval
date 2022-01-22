#!/bin/bash

if [ $# -lt 5 ]
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Num docs to retrieve (e.g. 100)";
    echo "2. Num-top docs for qpp-estimation (e.g. 50)";
    echo "3. Method (avgidf/nqc/wig/clarity/uef_nqc,/uef_wig/uef_clarity)";
    echo "4. Metric (r/rho/tau/qsim/qsim_strict/pairacc/class_acc/rmse)";
    echo "5. % of train query set";
    exit 1;
fi

METHOD=$3
METRIC=$4
SPLITS=$5
INDEXDIR=/store/index/trec678/
QRELS=/store/qrels/trec-robust.qrel

cat > qpp.properties << EOF1

index.dir=$INDEXDIR
query.file=/store/query/trec-robust.xml
qrels.file=$QRELS
res.file=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/outputs/lmdir.res
res.train=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/train.res
res.test=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/test.res
retrieve.num_wanted=$1
qpp.numtopdocs=$2
qpp.method=$METHOD
qpp.metric=$METRIC
qpp.splits=$SPLITS

EOF1

#mvn exec:java@method_metric_pair -Dexec.args="qpp.properties" > res.txt
#mvn exec:java@compute_all -Dexec.args="qpp.properties"
#mvn exec:java@across_metrics -Dexec.args="qpp.properties"
#mvn exec:java@across_models -Dexec.args="qpp.properties"
#mvn exec:java@linear_regressor -Dexec.args="qpp.properties"
mvn exec:java@poly_regressor -Dexec.args="qpp.properties"

echo "$METHOD $METRIC"
