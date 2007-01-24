#$ -l fq=true

#$ -N TPi-DSS
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.jobs.dss.DSSAnalysis "$JOB_DIR/run$SGE_TASK_ID"