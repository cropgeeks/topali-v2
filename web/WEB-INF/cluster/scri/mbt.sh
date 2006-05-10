#$ -l fastq=true

#$ -N TPi-MBT
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.trees.MBTreeAnalysis "$JOB_DIR"