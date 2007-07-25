CC = gcc

CCFLAGS = -03

LIBS = -lm # -lM

all: codeml


tools.o : paml.h tools.c
	$(CC) $(CFLAGS) -c tools.c

codeml.o : paml.h codeml.c treesub.c treespace.c
	$(CC) $(CFLAGS) -c codeml.c

codeml : codeml.o tools.o
	$(CC) $(CFLAGS) -o $@ codeml.o tools.o $(LIBS)


