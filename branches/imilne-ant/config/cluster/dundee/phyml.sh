#$ -q 32.bit-q
#$ -l www_service=1

#$ -N TPi-PhyML
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.phyml.PhymlAnalysis "$JOB_DIR"