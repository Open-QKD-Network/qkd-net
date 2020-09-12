CFLAGS=-I. -g -Wall -Wextra
ADD_LIBS=-lcurl -ljson-c

CFLAGS+=${ADD_CFLAGS}

all: test
test: test.o libopenqkd.a
	gcc $(CFLAGS) -o $@ test.o -L. -lopenqkd ${ADD_LIBS}
test.o: test.c
	gcc $(CFLAGS) -c -o $@ $^
libopenqkd.a: openqkd.c
	gcc $(CFLAGS) -c -o oqkd.o $^ ${ADD_LIBS}
	ar rcs $@ oqkd.o
clean:
	rm -f *.a *.o test
