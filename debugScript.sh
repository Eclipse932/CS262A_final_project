#!/bin/bash
cd src
java database/Replica  false replica5 5 128.32.44.161 true > trash.txt &

for i in `seq 1 4`;
do
     echo $i
     (java database/Replica  false replica$i 3 128.32.44.161 true > "replicaLog$i.txt") &
done
