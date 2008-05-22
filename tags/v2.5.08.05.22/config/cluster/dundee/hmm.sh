#$ -q 64bit-pri.q
#$ -l www_server=1
#$ -l qname=64bit-pri.q
#$ -l h_vmem=4000M
#$ -l mem_free=1500M
#$ -l h_cpu=01:00:00

#$ -N TPi-HMM
#$ -cwd
#$ -j y

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.hmm.HMMAnalysis "$JOB_DIR"