#$ -l fastq=true

#$ -N TPi-PDM2
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.jobs.pdm2.PDMAnalysis "$JOB_DIR/nodes/run$SGE_TASK_ID"