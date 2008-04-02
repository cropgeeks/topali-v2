#$ -l fqs=true

#$ -N TPi-MB
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.mrbayes.MrBayesAnalysis "$JOB_DIR"