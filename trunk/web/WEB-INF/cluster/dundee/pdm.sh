#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-PDM
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.jobs.pdm.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"