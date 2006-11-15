#$ -l fastq=true

#$ -N TPi-HMM
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.hmm.HMMAnalysis "$JOB_DIR"