#$ -l fastq=true 


# Nothing after this point should need changed

#$ -N DSS
#$ -cwd
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.dss.DSSAnalysis "$JOB_DIR/run$SGE_TASK_ID"


