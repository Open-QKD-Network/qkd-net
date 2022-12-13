CFLAGS= -I. -I/home/ubuntu/openquantumsafe/curl-install/include -L/home/ubuntu/openquantumsafe/curl-install/lib -g -Wall -Wextra
ADD_LIBS= -ljson-c -lcurl

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
