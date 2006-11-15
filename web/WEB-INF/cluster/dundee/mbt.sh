#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-MBT
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.trees.MBTreeAnalysis "$JOB_DIR"