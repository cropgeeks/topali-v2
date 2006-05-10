#$ -l fastq=true

#$ -N TPi-HMM
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.hmm.HMMAnalysis "$JOB_DIR"