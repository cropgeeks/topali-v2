#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-CML
#$ -cwd
#$ -j y
#$ -t 1-$RUNS

hostname
$JAVA -Xmx256m -cp $TOPALi $CLASS "$JOB_DIR/run$SGE_TASK_ID"