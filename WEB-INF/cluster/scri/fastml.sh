#$ -l fq=true

#$ -N TPi-FM
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.fastml.FastMLAnalysis "$JOB_DIR"