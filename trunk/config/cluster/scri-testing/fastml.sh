#$ -l fqs=true

#$ -N TPi-FM
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.fastml.FastMLAnalysis "$JOB_DIR"