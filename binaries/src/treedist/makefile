CC = gcc

CCFLAGS = -O3 -DHASLGAMMA

LIBS    = -lm

OBJS = phylip.o cons.o treedist.o

all: 	treedist

phylip.o:     phylip.c phylip.h
	$(CC) -c phylip.c

cons.o:          cons.c cons.h phylip.h 
	$(CC) -c cons.c

treedist.o:     treedist.c cons.h phylip.h
	$(CC) -c treedist.c

treedist:     treedist.o phylip.o cons.o
	$(CC) treedist.o cons.o phylip.o $(LIBS) -o treedist


clean:
	rm *.o treedist

