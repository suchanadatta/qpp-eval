#!/bin/bash

if [ $# -lt 6 ]
then
    echo "Usage: " $0 " <following arguments in sequence>";
    echo "1. Num docs to retrieve (e.g. 100)";
    echo "2. Num-top docs for qpp-estimation (e.g. 50)";
    echo "3. Method (avgidf/nqc/wig/clarity/uef_nqc,/uef_wig/uef_clarity)";
    echo "4. Retrieval Eval Metric (ap, p_10, recall, ndcg)";
    echo "5. QPP eval Metric (r/rho/tau/qsim/qsim_strict/pairacc/class_acc/rmse)";
    echo "6. % of train query set (an integer between [0, 100])";
    exit 1;
fi

METHOD=$3
RETEVAL_METRIC=$4
METRIC=$5
SPLITS=$6
INDEXDIR=/Users/debasis/research/common/trecd45/index/
QRELS=data/qrels.robust.all
QUERYFILE=data/topics.robust.all

cat > qpp.properties << EOF1

index.dir=$INDEXDIR
query.file=$QUERYFILE
qrels.file=$QRELS
res.file=outputs/lmdir.res
res.train=outputs/train.res
res.test=outputs/test.res
retrieve.num_wanted=$1
reteval.metric=$RETEVAL_METRIC
qpp.numtopdocs=$2
qpp.method=$METHOD
qpp.metric=$METRIC
qpp.splits=$SPLITS

EOF1

#mvn exec:java@method_metric_pair -Dexec.args="qpp.properties" > res.txt
#mvn exec:java@compute_all -Dexec.args="qpp.properties"
#mvn exec:java@across_metrics -Dexec.args="qpp.properties"
#mvn exec:java@across_models -Dexec.args="qpp.properties"
mvn exec:java@linear_regressor -Dexec.args="qpp.properties"
#mvn exec:java@poly_regressor -Dexec.args="qpp.properties"

