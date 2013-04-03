#$ -l pqs=true

#$ -N TPi-MG
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.modelgenerator.MGAnalysis "$JOB_DIR"