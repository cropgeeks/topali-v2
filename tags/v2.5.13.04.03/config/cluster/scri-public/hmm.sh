#$ -l pqs=true

#$ -N TPi-HMM
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.hmm.HMMAnalysis "$JOB_DIR"