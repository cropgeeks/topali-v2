#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-MG
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.modelgenerator.MGAnalysis "$JOB_DIR"