#!/bin/bash

numReplicas=5
cd src
javac database/Responder.java 
javac database/Replica.java
javac database/ClientAppGenerator.java
javac database/clearRR.java
javac database/ClientApp.java

java database/clearRR 
java database/ClientAppGenerator 0 1000-100000 50 10 3 10
java database/Responder 128.32.44.161 responder0 true &
java database/Replica true replica0 $numReplicas 128.32.44.161 true > "replicaLeader.txt" &

k=`expr $numReplicas - 1`
for i in `seq 1 $k`;
do
     echo $i 
     java database/Replica  false replica$i $numReplicas 128.32.44.161 true > "replicaLog$i.txt" &
done  

for i in `seq 1 10`;
do
    echo $i
    java database/ClientApp  responder0 ../test-file/client$i/filename.txt > "clientAppLog$i.txt"  &
done

