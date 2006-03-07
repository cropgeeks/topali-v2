#$ -l fastq=true 


# Nothing after this point should need changed

#$ -N LRT
#$ -cwd
#$ -t 1-$RUN_COUNT

$JAVA -cp $TOPALi topali.cluster.lrt.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"

