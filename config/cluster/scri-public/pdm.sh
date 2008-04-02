#$ -l pqp=true

#$ -N TPi-PDM
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.pdm.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"