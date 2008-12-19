#$ -l pqp=true

#$ -N TPi-DSS
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.dss.DSSAnalysis "$JOB_DIR/run$SGE_TASK_ID"