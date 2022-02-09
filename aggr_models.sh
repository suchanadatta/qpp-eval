#!/bin/bash

if [ $# -lt 3 ]
then
	echo "usage: $0 <IR metric (ap,p_10,recall,ndcg)> <metric (r/rho/tau/qsim/qsim_strict/pairacc/rmse)> <ret-eval specific regression (only for rmse; [true/false])>"
	exit
fi

IR_METRIC=$1
METRIC=$2
APPLY_REG=$3

PROP_FILE=qpp_across_models.properties
NUMDOCS=100
K=50

#Change this path to index/ (committed on git) after downloading the Lucene indexed
#TREC disks 4/5 index from https://rsgqglln.tkhcloudstorage.com/item/c59086c6b00d41e79d53c58ad66bc21f
INDEXDIR=/Users/debasis/research/common/trecd45/index/
QRELS=data/qrels.robust.all

cat > $PROP_FILE << EOF1
index.dir=$INDEXDIR
query.file=data/topics.robust.all
qrels.file=$QRELS
retrieve.num_wanted=$NUMDOCS
qpp.numtopdocs=$K
qpp.metric=$METRIC
res.file=/tmp/res
transform_scores=$APPLY_REG
qpp.splits=80
reteval.metric=$IR_METRIC

EOF1

mvn exec:java@across_models -Dexec.args=$PROP_FILE > across_models.$IR_METRIC.$METRIC.$APPLY_REG.txt
#rm $PROP_FILE

#echo "$METHOD $METRIC"
#echo "BM25 (k=0.5, b=1) BM25 (k=1.5, b=0.75) LM-Dir (1000) LM-JM (0.6)"
#grep -w $METRIC across_metrics.txt | grep "QPP-method: $METHOD" | awk '{print $NF}' | awk '{s=$1" "s; if (NR%3==0) {print s; s=""}}' 
