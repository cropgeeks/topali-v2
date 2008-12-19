#$ -l pqp=true

#$ -N TPi-CML
#$ -cwd
#$ -j y
#$ -t 1-$RUNS

hostname
$JAVA -Xmx512m -cp $TOPALi $CLASS "$JOB_DIR/run$SGE_TASK_ID"