#!/bin/bash

if [ $# -lt 3 ]
then
	echo "usage: $0 <model [bm25/lmdir/lmjm> <metric (r/rho/tau/qsim/qsim_strict/pairacc/rmse)> <ret-eval specific regression (only for rmse; [true/false])>"
	exit
fi

MODEL=$1
METRIC=$2
APPLY_REG=$3

#Change this path to index/ (committed on git) after downloading the Lucene indexed
#TREC disks 4/5 index from https://rsgqglln.tkhcloudstorage.com/item/c59086c6b00d41e79d53c58ad66bc21f
INDEXDIR=/Users/debasis/research/common/trecd45/index/

cat > qpp.properties << EOF1
ret.model=$MODEL
index.dir=$INDEXDIR
query.file=data/topics.robust.all
res.file=/tmp/res
qrels.file=data/qrels.robust.all
retrieve.num_wanted=100
qpp.numtopdocs=50
qpp.metric=$METRIC
transform_scores=$APPLY_REG
qpp.splits=80

EOF1

mvn exec:java@across_metrics -Dexec.args="qpp.properties" > across_metrics.$METRIC.txt

#echo "$METHOD $METRIC"
#echo "BM25 (k=0.5, b=1) BM25 (k=1.5, b=0.75) LM-Dir (1000) LM-JM (0.6)"
#grep -w $METRIC across_metrics.txt | grep "QPP-method: $METHOD" | awk '{print $NF}' | awk '{s=$1" "s; if (NR%3==0) {print s; s=""}}' 
