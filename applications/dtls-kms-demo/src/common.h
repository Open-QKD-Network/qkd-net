#ifndef _COMMON_H
#define _COMMON_H

#include <assert.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/bio.h>

#include <openssl/sha.h>
#include <getopt.h>

#include <netinet/in.h>
#include <arpa/inet.h>


#define no_argument 0
#define required_argument 1 
#define optional_argument 2

#define INVALID_SOCKET	(-1)
#define get_last_socket_error()	errno

#if defined(REST_KMS)
#include <curl/curl.h>
#include <json-c/json.h>

extern CURL *gHandle;

struct Net_Crypto {
    //All the char arrays are NULL terminated strings
    char getkey_url[128];
    char newkey_url[128];
    char uaa_url[128];
    char site_id[4];
    char block_id[37];
    bool auth;
    unsigned char key[32]; //shared key from KMS
    char peer_site_id[4];
    int index;
    CURL *curl_handle;
};

extern struct Net_Crypto gNC;
#endif 

extern BIO *bio_err;
extern BIO *bio_out;
extern char *cipher;
extern char *psk_identity;
extern char *psk_key;

typedef unsigned int (*client_cb_kms)(SSL *ssl, const char *hint, char *identity,
	unsigned int max_identity_len, unsigned char *psk, unsigned int max_psk_len);

#define log_info(args...) BIO_printf(bio_out, args);
#define log_error(args...) BIO_printf(bio_err , args)

int hex2bin(char *str, unsigned char *out);
int init_client(int *sock, char *host, int port, int type);
void apps_startup();
void bytetohex(uint8_t *byte, int bytelen, char *hex);
void sha(char *ibuf, unsigned char obuf[20]);
int fetch_key_from_kms(client_cb_kms, short, char buf[32], long *index);
#if defined(REST_KMS)
int get_key(struct Net_Crypto *nc, char *token, int is_new);
int get_token(struct Net_Crypto *nc, char** token);
void prepare_kms_access(struct Net_Crypto *m);
void fetch_new_qkd_key(struct Net_Crypto *nc);
void fetch_qkd_key(struct Net_Crypto *nc);
void fn(void);
#endif

#endif
