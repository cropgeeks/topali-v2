#$ -l pqs=true

#$ -N TPi-CW
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.codonw.CodonWAnalysis "$JOB_DIR"