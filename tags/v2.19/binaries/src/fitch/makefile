CC = g++

CCFLAGS = -O3 -DHASLGAMMA

OBJS = fitch.o dist.o phylip.o

all: 	fitch

fitch.o:        fitch.c dist.h phylip.h
	$(CC) -c fitch.c

fitch:        fitch.o dist.o phylip.o fitch.c dist.c phylip.c dist.h phylip.h
	$(CC) fitch.o dist.o phylip.o $(LIBS) -o fitch

clean:
	rm *.o fitch

