#$ -l fastq=true

#$ -N TPi-PDM2
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.pdm2.PDMPostAnalysis "$JOB_DIR"