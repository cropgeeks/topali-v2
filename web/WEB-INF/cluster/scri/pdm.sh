#$ -l fastq=true

#$ -N TPi-PDM
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.pdm.PDMAnalysis "$JOB_DIR"