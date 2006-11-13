#$ -q 32.bit-q
#$ -l www_parallel=1

#$ -N TPi-CML
#$ -cwd
#$ -j y
#$ -t 1-8

$JAVA -cp $TOPALi topali.cluster.cml.CodeMLAnalysis "$JOB_DIR/run$SGE_TASK_ID"