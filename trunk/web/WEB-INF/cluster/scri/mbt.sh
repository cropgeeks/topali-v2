#$ -l fastq=true

#$ -N TPi-MBT
#$ -cwd
#$ -j y

$JAVA -cp $TOPALi topali.cluster.jobs.trees.MBTreeAnalysis "$JOB_DIR"