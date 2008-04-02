#$ -l pqs=true

#$ -N TPi-HMM
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.hmm.HMMAnalysis "$JOB_DIR"