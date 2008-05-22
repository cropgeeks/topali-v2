#$ -l fqp=true

#$ -N TPi-DSS
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.dss.analysis.DSSAnalysis "$JOB_DIR/run$SGE_TASK_ID"