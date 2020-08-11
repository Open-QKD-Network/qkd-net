CFLAGS=-I. -g -DOPENSSL_NO_ENGINE -Wall -Wextra
LIBS=-lcrypto -lssl

ADD_LIBS=-lcurl -ljson-c
ADD_CFLAGS=-DREST_KMS

CFLAGS+=${ADD_CFLAGS}

all: bob alice opensslrand
bob:src/common.c src/bob.c
	gcc $(CFLAGS) -o $@ $^ ${LIBS} ${ADD_LIBS}
alice:src/common.c src/alice.c
	gcc $(CFLAGS) -o $@ $^ ${LIBS} ${ADD_LIBS}
opensslrand:src/opensslrand.c
	gcc  -o $@ $^ ${LIBS} 
clean:
	rm -f bob alice opensslrand bobdemo
