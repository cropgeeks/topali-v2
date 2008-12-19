#$ -l fqs=true

#$ -N TPi-PhyML
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx512m -cp $TOPALi topali.cluster.jobs.phyml.analysis.PhymlAnalysis "$JOB_DIR"