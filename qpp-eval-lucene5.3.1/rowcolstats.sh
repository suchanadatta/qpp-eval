#!/bin/bash

if [ $# -lt 1 ]
then
	echo "usage: $0 <data>" 
	exit
fi

cat $1 | awk '{for (i=1;i<=NF;i++) v[NR"_"i] = $i; } END { \
for (i=1;i<=NR;i++) { m=0; s=0; for (j=1;j<=NF;j++) { m+=v[i"_"j]; } m=m/NF; for (j=1;j<=NF;j++) {s+= (v[i"_"j]-m)*(v[i"_"j]-m); } printf("%.4f\n", sqrt(s/NF)); } printf("\n"); \
for (j=1;j<=NF;j++) { m=0; s=0; for (i=1;i<=NR;i++) { m+=v[i"_"j]; } m=m/NR; for (i=1;i<=NR;i++) {s+= (v[i"_"j]-m)*(v[i"_"j]-m); } printf("%.4f ", sqrt(s/NR)); } printf("\n"); \
}'
