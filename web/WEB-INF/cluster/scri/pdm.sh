#$ -l fastq=true

#$ -N TPi-PDM
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.pdm.PDMAnalysis "$JOB_DIR/run$SGE_TASK_ID"