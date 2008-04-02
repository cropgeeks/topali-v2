#$ -l fqs=true

#$ -N TPi-MG
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.modelgenerator.MGAnalysis "$JOB_DIR"