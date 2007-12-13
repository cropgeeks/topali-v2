#$ -l fq=true

#$ -N TPi-MT
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.modeltest.analysis.ModelTestAnalysis "$JOB_DIR/run$SGE_TASK_ID"
