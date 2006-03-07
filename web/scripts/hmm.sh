#$ -l fastq=true 


# Nothing after this point should need changed

#$ -N HMM
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.hmm.HMMAnalysis "$JOB_DIR"