#CC = CC
CC = g++ 

#CCFLAGS = -fast
CCFLAGS = -O3 -Wno-deprecated

OBJS = basics.o data.o treemanip.o treemanip2.o mathlib.o \
	ChooseOptions.o prog.o 

all:    barce

barce: ${OBJS}
	${CC} -o $@ ${CCFLAGS} ${OBJS}

prog.o : basics.h ChooseOptions.h data.h treemanip.h \
         mathlib.h Random.h prog.cc
	${CC} -c ${CCFLAGS} prog.cc

ChooseOptions.o : ChooseOptions.h basics.h ChooseOptions.cc
	${CC} -c ${CCFLAGS} ChooseOptions.cc

mathlib.o : mathlib.h Random.h mathlib.cc
	${CC} -c ${CCFLAGS} mathlib.cc

treemanip2.o : basics.h data.h mathlib.h treemanip2.cc
	${CC} -c ${CCFLAGS} treemanip2.cc

treemanip.o : basics.h data.h mathlib.h treemanip.cc
	${CC} -c ${CCFLAGS} treemanip.cc

data.o : basics.h data.h data.cc
	${CC} -c ${CCFLAGS} data.cc

basics.o : basics.h basics.cc
	${CC} -c ${CCFLAGS} basics.cc
