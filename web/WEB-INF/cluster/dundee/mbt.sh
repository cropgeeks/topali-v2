#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-MBT
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.trees.MBTreeAnalysis "$JOB_DIR"