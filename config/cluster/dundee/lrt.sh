#$ -q 64bit-pri.q
#$ -l www_pserv=1
#$ -l qname=64bit-pri.q
#$ -l h_vmem=4000M
#$ -l mem_free=1500M
#$ -l h_cpu=01:00:00

#$ -N TPi-LRT
#$ -cwd
#$ -j y
#$ -t 1-$RUN_COUNT

hostname
$JAVA -Xmx256m -cp $TOPALi topali.cluster.jobs.lrt.LRTAnalysis "$JOB_DIR/run$SGE_TASK_ID"