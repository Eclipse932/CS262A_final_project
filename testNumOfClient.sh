#!/bin/bash

numReplicas=5
cd src
#javac database/Responder.java 
#javac database/Replica.java
#javac database/ClientAppGenerator.java
#javac database/clearRR.java
#javac database/ClientApp.java

#java database/clearRR 
#java database/ClientAppGenerator 0 1000-100000 500 10 4 10
#java database/Responder 128.32.44.161 responder0 true &
#java database/Replica true replica0 $numReplicas 128.32.44.161 true > "../result/replica/replicaLeader.txt" &

#k=`expr $numReplicas - 1`
#for i in `seq 1 $k`;
#do
     #echo $i 
     #java database/Replica  false replica$i $numReplicas 128.32.44.161 true > "../result/replica/replicaLog$i.txt" &
#done  
#java database/Replica false replica1 3 192.168.1.103 false &
for j in 10 20 30 40 50 60 70 80 90 100
do
  for i in `seq 1 $j`;
  do
    if [[ `expr $i % 2` -eq 0 ]]
      then
      echo $i
      java database/ClientApp  responder0 ../hotspot-test-file/client$i/filename.txt > "../result/clientApp/$j/3DclientAppLog$i.txt"  &
      else
      java database/ClientApp  responder1 ../hotspot-test-file/client$i/filename.txt > "../result/clientApp/$j/3DclientAppLog$i.txt"  &
    fi
  done
  read
done

