#$ -N TOPALi-PDM2
#$ -cwd
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.pdm2.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"