#!/bin/bash

if [ $# -lt 4 ]
then
    echo "Usage: " $0 " <following arguments in sequence>";
    echo "QPP Method (avgidf/nqc/wig/clarity/uef_nqc,/uef_wig/uef_clarity)";
    echo "Retrieval Eval Metric (ap, p_10, recall, ndcg)";
    echo "QPP eval Metric (r/rho/tau/qsim/qsim_strict/pairacc/class_acc/rmse)";
    echo "Apply regression-based score transformation (true/false)";
    exit 1;
fi

#These are hyper-parameters... usually these values work well
NUMWANTED=100
NUMTOP=50
SPLITS=80

METHOD=$1
RETEVAL_METRIC=$2
METRIC=$3
APPLY_REG=$4
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
transform_scores=$APPLY_REG

EOF1

mvn exec:java@method_metric_pair -Dexec.args="qpp.properties"

