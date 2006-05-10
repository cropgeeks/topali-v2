#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-PDM
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.pdm.PDMAnalysis "$JOB_DIR"