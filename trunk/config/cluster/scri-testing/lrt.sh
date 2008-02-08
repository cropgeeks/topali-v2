#$ -l fq=true

#$ -N TPi-LRT
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.lrt.analysis.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"