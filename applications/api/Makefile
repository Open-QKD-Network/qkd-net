CFLAGS=-I. -g -Wall -Wextra
ADD_LIBS=-lcurl -ljson-c

CFLAGS+=${ADD_CFLAGS}

all: test
test: test.o liboqkd.a
	gcc $(CFLAGS) -o $@ test.o -L. -loqkd ${ADD_LIBS}
test.o: test.c
	gcc $(CFLAGS) -c -o $@ $^
liboqkd.a: openqkd.c
	gcc $(CFLAGS) -c -o oqkd.o $^ ${ADD_LIBS}
	ar rcs $@ oqkd.o
clean:
	rm -f *.a *.o