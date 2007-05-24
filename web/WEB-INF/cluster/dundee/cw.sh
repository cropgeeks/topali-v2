#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-CW
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.codonw.CodonWAnalysis "$JOB_DIR"