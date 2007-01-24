#$ -l fq=true

#$ -N TPi-LRT
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.jobs.lrt.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"