#$ -l fq=true

#$ -N TPi-CW
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.codonw.CodonWAnalysis "$JOB_DIR"