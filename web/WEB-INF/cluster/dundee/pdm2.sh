#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-PDM2
#$ -cwd
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.pdm2.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"