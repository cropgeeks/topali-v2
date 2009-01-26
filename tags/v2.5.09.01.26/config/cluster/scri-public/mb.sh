#$ -l pqs=true

#$ -N TPi-MB
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.mrbayes.MrBayesAnalysis "$JOB_DIR"