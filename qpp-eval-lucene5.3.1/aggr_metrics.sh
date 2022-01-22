#!/bin/bash

if [ $# -lt 3 ]
then
	echo "usage: $0 <num docs to retrieve (e.g. 100)> <num-top docs for qpp-estimation (e.g. 50) <metric (r/rho/tau/qsim/qsim_strict/pairacc)>"
	exit
fi

METRIC=$3

#Change this path to index/ (committed on git) after downloading the Lucene indexed
#TREC disks 4/5 index from https://rsgqglln.tkhcloudstorage.com/item/c59086c6b00d41e79d53c58ad66bc21f
INDEXDIR=/Users/debasis/research/common/trecd45/index/

QRELS=data/qrels.trec8.adhoc

cat > qpp.properties << EOF1

index.dir=$INDEXDIR
query.file=data/topics.401-450.xml
res.file=res_
qrels.file=$QRELS
retrieve.num_wanted=$1
qpp.numtopdocs=$2
qpp.metric=$METRIC

EOF1

mvn exec:java@across_metrics -Dexec.args="qpp.properties" > across_metrics.$METRIC.txt

rm qpp.properties

#echo "$METHOD $METRIC"
#echo "BM25 (k=0.5, b=1) BM25 (k=1.5, b=0.75) LM-Dir (1000) LM-JM (0.6)"
#grep -w $METRIC across_metrics.txt | grep "QPP-method: $METHOD" | awk '{print $NF}' | awk '{s=$1" "s; if (NR%3==0) {print s; s=""}}' 
