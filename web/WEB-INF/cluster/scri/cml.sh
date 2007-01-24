#$ -l fq=true

#$ -N TPi-CML
#$ -cwd
#$ -j y
#$ -t 1-8

$JAVA -cp $TOPALi topali.cluster.jobs.cml.CodeMLAnalysis "$JOB_DIR/run$SGE_TASK_ID"