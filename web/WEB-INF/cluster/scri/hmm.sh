#$ -l fastq=true

#$ -N TPi-HMM
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.hmm.HMMAnalysis "$JOB_DIR"