#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-LRT
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.lrt.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"