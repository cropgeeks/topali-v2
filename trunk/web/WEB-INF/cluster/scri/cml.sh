#$ -l fq=true

#$ -N TPi-CML
#$ -cwd
#$ -j y
#$ -t 1-$RUNS

$JAVA -cp $TOPALi $CLASS "$JOB_DIR/run$SGE_TASK_ID"