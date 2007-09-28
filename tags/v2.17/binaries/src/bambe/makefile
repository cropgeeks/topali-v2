CC = g++

# Possible flags are:
#   HASLGAMMA 	if the system has a built-in lgamma routine (check math.h).
#   WINDOWS	for a Windows(TM) system.
#   ALPHA       for a bug-fix for DEC Alphas.

CCFLAGS = -O3 -DHASLGAMMA

OBJS = util.o runsettings.o basedata.o sitedata.o site.o parameters.o files.o \
	matrixMath.o node.o tree.o main.o

all: 	bambe

bambe: ${OBJS}
	${CC} -o $@ ${CCFLAGS} ${OBJS}

main.o : util.h runsettings.h basedata.h sitedata.h site.h parameters.h \
	 files.h node.h tree.h main.C
	${CC} -c ${CCFLAGS} main.C

tree.o : util.h runsettings.h sitedata.h site.h parameters.h files.h node.h \
	tree.h tree.C
	${CC} -c ${CCFLAGS} tree.C

node.o : util.h runsettings.h sitedata.h site.h parameters.h files.h node.h \
	node.C
	${CC} -c ${CCFLAGS} node.C

matrixMath.o : matrixMath.h matrixMath.C
	${CC} -c ${CCFLAGS} matrixMath.C

files.o : runsettings.h files.C
	${CC} -c ${CCFLAGS} files.C

site.o : util.h site.h site.C
	${CC} -c ${CCFLAGS} site.C

sitedata.o : util.h runsettings.h sitedata.h site.h sitedata.C
	${CC} -c ${CCFLAGS} sitedata.C

parameters.o : util.h runsettings.h sitedata.h matrixMath.h parameters.h parameters.C
	${CC} -c ${CCFLAGS} parameters.C

basedata.o : util.h runsettings.h basedata.h basedata.C
	${CC} -c ${CCFLAGS} basedata.C

runsettings.o: util.h runsettings.h runsettings.C
	${CC} -c ${CCFLAGS} runsettings.C

util.o: util.h util.C
	${CC} -c ${CCFLAGS} util.C

clean:
	rm *.o bambe

