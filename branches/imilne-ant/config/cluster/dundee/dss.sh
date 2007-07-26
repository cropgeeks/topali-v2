#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-DSS
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.dss.DSSAnalysis "$JOB_DIR/run$SGE_TASK_ID"