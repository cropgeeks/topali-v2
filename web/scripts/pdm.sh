#$ -l fastq=true 


# Nothing after this point should need changed

#$ -N PDM
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.pdm.PDMAnalysis "$JOB_DIR"