#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-LRT
#$ -cwd
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.lrt.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"