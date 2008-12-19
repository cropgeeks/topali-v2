#$ -l fqp=true

#$ -N TPi-PDM
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.pdm.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"