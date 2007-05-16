#$ -l fq=true

#$ -N TPi-MB
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.mrbayes.MrBayesAnalysis "$JOB_DIR"