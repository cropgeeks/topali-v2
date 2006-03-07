#$ -l fastq=true 


# Nothing after this point should need changed

#$ -N MBT
#$ -cwd

$JAVA -cp $TOPALi topali.cluster.trees.MBTreeAnalysis "$JOB_DIR"