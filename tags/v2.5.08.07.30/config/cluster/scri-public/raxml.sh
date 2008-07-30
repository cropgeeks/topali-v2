#$ -l pqp=true

#$ -N TPi-RX
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.raxml.analysis.RaxmlAnalysis "$JOB_DIR/run$SGE_TASK_ID"
