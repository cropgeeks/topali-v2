#$ -l pqs=true

#$ -N TPi-CW
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.codonw.CodonWAnalysis "$JOB_DIR"