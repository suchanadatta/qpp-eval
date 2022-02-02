#!/bin/bash

if [ $# -lt 3 ]
then
    echo "Usage: " $0 " <following arguments in sequence>";
    echo "1. Method (avgidf/nqc/wig/clarity/uef_nqc,/uef_wig/uef_clarity)";
    echo "2. Retrieval Eval Metric (ap, p_10, recall, ndcg)";
    echo "3. QPP eval Metric (r/rho/tau/qsim/qsim_strict/pairacc/class_acc/rmse)";
    exit 1;
fi

#These are hyper-parameters... usually these values work well
NUMWANTED=100
NUMTOP=50
SPLITS=80

METHOD=$1
RETEVAL_METRIC=$2
METRIC=$3
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
retrieve.num_wanted=$NUMWANTED
reteval.metric=$RETEVAL_METRIC
qpp.numtopdocs=$NUMTOP
qpp.method=$METHOD
qpp.metric=$METRIC
qpp.splits=$SPLITS

EOF1

#mvn exec:java@method_metric_pair -Dexec.args="qpp.properties" > res.txt
#mvn exec:java@compute_all -Dexec.args="qpp.properties"
#mvn exec:java@across_metrics -Dexec.args="qpp.properties"
#mvn exec:java@across_models -Dexec.args="qpp.properties"
#mvn exec:java@linear_regressor -Dexec.args="qpp.properties"
#mvn exec:java@poly_regressor -Dexec.args="qpp.properties"

