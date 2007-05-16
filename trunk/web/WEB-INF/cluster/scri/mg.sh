#$ -l fq=true

#$ -N TPi-MG
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.modelgenerator.MGAnalysis "$JOB_DIR"