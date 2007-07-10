#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-HMM
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.hmm.HMMAnalysis "$JOB_DIR"