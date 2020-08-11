#include <assert.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ctype.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/bio.h>

#include <openssl/sha.h>



#define PORT            10443

#define INVALID_SOCKET	(-1)

#define log_info(args...) BIO_printf(bio_s_out, args);
#define log_error(args...) BIO_printf(bio_err , args)

BIO *bio_err = NULL;

static char *cipher = "PSK-AES256-CBC-SHA";
static SSL_CTX *ctx = NULL;
static BIO *bio_s_out = NULL;
static char *psk_identity = "Client_identity";
char *psk_key = "1A1A1A1A1A1A1A1A1A1A1A1A1A1A1A1A";

short port;

char keyfilename[64];

static inline int cval(char c)
{
    if (c>='a') return c-'a'+0x0a;
    if (c>='A') return c-'A'+0x0a;
    return c-'0';
}

/* return value: number of bytes in out, <=0 if error */
static int hex2bin(char *str, unsigned char *out)
{
    int i;
    for(i = 0; str[i] && str[i+1]; i+=2) {
        if (!isxdigit(str[i])&& !isxdigit(str[i+1]))
            return -1;
        out[i/2] = (cval(str[i])<<4) + cval(str[i+1]);
    }
    return i/2;
}

static unsigned int psk_server_cb(SSL * ssl, const char *identity,
                                  unsigned char *psk, unsigned int max_psk_len)
{
    int ret = 0;
    (void)(ssl);

    if (!identity) {
        log_error("Error: client did not send PSK identity\n");
        return 0;
    }

    if (strcmp(identity, psk_identity) != 0) {
        log_info("PSK error: (got '%s' expected '%s')\n",
                 identity, psk_identity);
        return 0;
    }
    if (strlen(psk_key)>=(max_psk_len*2)) {
        log_error("Error, psk_key too long\n");
        return 0;
    }

    /* convert the PSK key to binary */
    ret = hex2bin(psk_key,psk);
    if (ret<=0) {
        //log_error( "Could not convert PSK key '%s' to binary key\n", psk_key);
        //	return 0;
    }
    return ret;
}

static int init_server(int *sock, int port, char *ip, int type)
{
    int ret = 0;
    struct sockaddr_in server;
    int s = -1;
    int j = 1;

    memset((char *)&server, 0, sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons((unsigned short)port);
    if (ip == NULL)
        server.sin_addr.s_addr = INADDR_ANY;
    else
        memcpy(&server.sin_addr.s_addr, ip, 4);

    s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (s == INVALID_SOCKET)
        goto err;
    setsockopt(s, SOL_SOCKET, SO_REUSEADDR, (void *)&j, sizeof j);

    if (bind(s, (struct sockaddr *)&server, sizeof(server)) == -1) {
        perror("bind");
        goto err;
    }

    if (type == SOCK_STREAM && listen(s, 128) == -1)
        goto err;

    *sock = s;
    ret = 1;

err:
    if ((ret == 0) && (s != -1)) {
        close(s);
    }
    return (ret);
}

static int do_accept(int acc_sock, int *sock)
{
    int ret;
    static struct sockaddr_in from;
    int len;

    memset((char *)&from, 0, sizeof(from));
    len = sizeof(from);

    ret = accept(acc_sock, (struct sockaddr *)&from, (void *)&len);
    if (ret == INVALID_SOCKET) {
        if (errno == EINTR) {
            /*check_timeout(); */
            printf("accept interrupted\n");
        }
        fprintf(stderr, "errno=%d ", errno);
        return (0);
    }

    *sock = ret;
    return (1);
}

void sha(char *ibuf, unsigned char obuf[20])
{
    SHA1((unsigned char*)ibuf, 32, obuf);
}

static void bytetohex(uint8_t *byte, int bytelen, char *hex)
{
    int j;
    for(j = 0; j < bytelen; j++)
        sprintf(&hex[2*j], "%02X", byte[j]);
}

int server_body(int s, FILE *file)
{
    char keybuf[32];
    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';

//	char *buf = NULL;
    fd_set readfds;
    int ret = 1, width;
    int bytes_read, bytes_written;
    SSL *con = NULL;
    BIO *sbio;
    int read_blocked = 0;
    int index = 0, i;
    size_t n = 0;



    SSL_CTX_set_psk_server_callback(ctx, psk_server_cb);
    SSL_CTX_use_psk_identity_hint(ctx, "KMS");

    con = SSL_new(ctx);
// SSL_clear(con);
    sbio = BIO_new_socket(s, BIO_NOCLOSE);
    SSL_set_bio(con, sbio, sbio);
    SSL_set_accept_state(con);

    if (!SSL_is_init_finished(con)) {
        i = SSL_accept(con);
        if (i <= 0) {
            //log_error("Handshake Error\n");
            goto err;
        }
    }

    width = s + 1;
    for (;;) {
        FD_ZERO(&readfds);
        FD_SET(s, &readfds);

        i = select(width, (void *)&readfds, NULL, NULL,NULL);
        if (i <= 0)
            continue;

        if (FD_ISSET(s, &readfds)) {
            //read data
            do {
                read_blocked = 0;
                bytes_read = SSL_read(con, &index, 4);;

                //check SSL errors
                switch(SSL_get_error(con, bytes_read)) {
                case SSL_ERROR_NONE:
                    break;

                case SSL_ERROR_ZERO_RETURN:
                    //connection closed by client, clean up
                    log_info("    -- TLS Closed\n");
                    ret = 1;
                    goto err;

                case SSL_ERROR_WANT_READ:
                    //the operation did not complete, block the read
                    read_blocked = 1;
                    break;

                case SSL_ERROR_WANT_WRITE:
                    //the operation did not complete
                    break;

                case SSL_ERROR_SYSCALL:
                    //some I/O error occured (could be caused by false start in Chrome for instance), disconnect the client and clean up
                    ret = 1;
                    goto err;

                default:
                    //some other error, clean up
                    ret = 1;
                    goto err;
                }

            } while (SSL_pending(con) && !read_blocked);
        }

        if (index == -1)
            printf("    -- Bob requested a new key\n");
        else
            printf("    -- Alice requested a key at index %d\n", index);


        if (index == -1) {
            int len = ftell(file);
            n = fread(keybuf, 32, 1, file);
            index = len/32;
        } else {
            int s = fseek(file, index*32, SEEK_SET );
            if (!s)
                n = fread(keybuf, 32, 1, file);
        }
        if (n == 0) {
            printf("    -- No more keys left to send. \n");
            break;
        } else {
            sha(keybuf, md);
            bytetohex(md, 20, (char*)mdhex);
            printf("    -- SHA1 of the sent key at index %d : %s\n", index, mdhex);
        }

        if (n == 0) {
            ret = 1;
            goto err;
        }

        for (;;) {
            bytes_written = SSL_write(con, &index, 4);
            bytes_written = SSL_write(con, keybuf, 32);

            switch (SSL_get_error(con, bytes_written)) {
            case SSL_ERROR_NONE:
                break;
            case SSL_ERROR_WANT_WRITE:
            case SSL_ERROR_WANT_READ:
            case SSL_ERROR_WANT_X509_LOOKUP:
                log_info("Write BLOCK\n");
                break;
            case SSL_ERROR_SYSCALL:
            case SSL_ERROR_SSL:
                log_info("ERROR write\n");
                ERR_print_errors(bio_err);
                ret = 1;
                goto err;
            /* break; */
            case SSL_ERROR_ZERO_RETURN:
                log_info("DONE\n");
                ret = 1;
                goto err;
            }
            break;
        }
    }

err:
    if (con != NULL) {
        SSL_set_shutdown(con, SSL_SENT_SHUTDOWN | SSL_RECEIVED_SHUTDOWN);
        SSL_free(con);
        log_info("    -- Connection closed\n");
    }

    if (ret >= 0)
        log_info(" -- Accept connection\n");
    close(s);

    return (ret);
}

#include <getopt.h>

#define no_argument 0
#define required_argument 1
#define optional_argument 2

void print_usage() {
    printf("Usage: kms [-l] [-p port] [-k keyfile] \n");
}

static void list_keys() {
    char keybuf[32];
    char cmd[32];
    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';
    int index = 0;

    fd_set readfds;
    int i, width = 1;

    FILE *keysfile = fopen(keyfilename,"rb+");
#if 0
    while (1) {
        size_t n = fread(keybuf, 32, 1, keysfile);
        if (n > 0) {
            sha(keybuf, md);
            bytetohex(md, 20, (char*)mdhex);
            printf("-- SHA1 of key at index %u : %s\n", index++, mdhex);

        }
        else {
            printf("-- No more keys left to print. \n");

        }

        sleep(1);

    }

#endif

#if 1
    for (;;) {
        FD_ZERO(&readfds);
        FD_SET(fileno(stdin), &readfds);
        i = select(width, (void *)&readfds, NULL, NULL,NULL);
        if (i <= 0)
            continue;

        if (FD_ISSET(fileno(stdin), &readfds)) {
            if (fgets(cmd, 64, stdin) == NULL) {
                printf("fgets from stdin error\n");
            }

            if (strcmp(cmd, "end\n") == 0)
                break;

            size_t n = fread(keybuf, 1, 32, keysfile);
            if (n == 0) {
                printf("-- No more keys left to print. \n");
            } else {
                sha(keybuf, md);
                bytetohex(md, 20, (char*)mdhex);
                printf("-- SHA1 of key at index %u : %s\n", index++, mdhex);
            }
        }
    }
#endif
    fclose(keysfile);
}

static int serve_keys() {
    int off = SSL_OP_NO_SSLv2;
    const SSL_METHOD *meth = NULL;
    int i, sock;
    int accept_socket = 0;

    ERR_load_crypto_strings();
    SSL_library_init();
    bio_err = BIO_new_fp(stderr, BIO_NOCLOSE);
    bio_s_out = BIO_new_fp(stdout, BIO_NOCLOSE);

    meth = TLSv1_server_method();
    ctx = SSL_CTX_new(meth);
    if (ctx == NULL) {
        log_error(" SSL_CTX_new error\n");
        return -1;
    }

    SSL_CTX_set_quiet_shutdown(ctx, 1);
    SSL_CTX_set_options(ctx, off);
    //SSL_CTX_set_psk_server_callback(ctx, psk_server_cb);
    if (!SSL_CTX_set_cipher_list(ctx, cipher)) {
        ERR_print_errors(bio_err);
        return -2;
    }

    if (!init_server(&accept_socket, port, NULL, SOCK_STREAM)) {
        log_error("Init server error\n");
        return (-3);
    }

    FILE* psk_file = fopen(keyfilename,"rb+");

    while(1) {
        // SSL_CTX_set_psk_server_callback(ctx, psk_server_cb);
        if (do_accept(accept_socket, &sock) == 0) {
            close(accept_socket);
            fclose(psk_file);
            return (0);
        }
        i = server_body(sock, psk_file);
        if (i < 0) {
            close(accept_socket);
            fclose(psk_file);
            return i;
        }
    }
    fclose(psk_file);
    return 0;
}

int main(int argc, char * argv[])
{

    const struct option longopts[] =
    {
        {"list key one by one on hitting return",   no_argument,        0, 'l'},
        {"port",     optional_argument,  0, 'p'},
        {"key file",     optional_argument,  0, 'k'},
        {0,0,0,0},
    };

    strcpy(keyfilename, "kms.bin");
    keyfilename[strlen("kms.bin")] = '\0';


    short num = 0;
    int index;
    int iarg = 0;
    //turn off getopt error message
    opterr = 1;
    int list_mode = 0;
    port = PORT;
    while(iarg != -1)
    {
        iarg = getopt_long(argc, argv, "lhk:p:", longopts, &index);

        switch (iarg)
        {
        case 'l':
            list_mode = 1;
            break;

        case 'p':
            num = atoi(optarg);
            if (num != 0)
                port = num;
            break;

        case 'k':
            if (optarg) {
                strcpy(keyfilename, optarg);
                keyfilename[strlen(optarg)] = '\0';
            }
            break;


        case 'h':
            print_usage();
            return -1;
        }
    }

    if (list_mode == 0) {
        printf(" -- Starting KMS in key serving mode, listening on port %d ... \n", port);
        serve_keys();
    } else {
        printf(" -- Starting KMS in key listing mode, listening on port %d ... \n", port);
        list_keys();
    }

#if 0

    int off = SSL_OP_NO_SSLv2;
    const SSL_METHOD *meth = NULL;
    int sock;
    int accept_socket = 0;
    int i;

    ERR_load_crypto_strings();
    SSL_library_init();
    bio_err = BIO_new_fp(stderr, BIO_NOCLOSE);
    bio_s_out = BIO_new_fp(stdout, BIO_NOCLOSE);

    meth = TLSv1_server_method();
    ctx = SSL_CTX_new(meth);
    if (ctx == NULL) {
        log_error(" SSL_CTX_new error\n");
        return -1;
    }

    SSL_CTX_set_quiet_shutdown(ctx, 1);
    SSL_CTX_set_options(ctx, off);
    SSL_CTX_set_psk_server_callback(ctx, psk_server_cb);
    if (!SSL_CTX_set_cipher_list(ctx, cipher)) {
        ERR_print_errors(bio_err);
        return -2;
    }

    if (!init_server(&accept_socket, port, NULL, SOCK_STREAM))
        return (-3);

    while(1) {
        if (start_skm) {
            if (do_accept(accept_socket, &sock) == 0) {
                close(accept_socket);
                return (0);
            }
        }
        i = server_body(sock);
        if (i < 0) {
            close(accept_socket);
            return i;
        }
    }
    return 0;
#endif
}

