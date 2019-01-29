#include "common.h"
#define PORT            10445
#define BOB_PORT        10446

BIO *bio_err = NULL;
BIO *bio_out = NULL;

static SSL_CTX *ctx = NULL;

static short port;
static short bport;

static char keybuf[32];
static char filename[64];

static FILE *fp;
static int file_counter = 0;

static unsigned int psk_server_cb(SSL * ssl, const char *identity,
                                  unsigned char *psk, unsigned int max_psk_len)
{

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
    if (32 >= (max_psk_len*2)) {
        log_error("Error, psk_key too long\n");
        return 0;
    }
    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';

    sha(keybuf, md);
    bytetohex(md, 20, (char*)mdhex);
    printf("    -- SHA1 of the received key : %s\n", mdhex);


    memcpy(psk, keybuf, 32);
    return 1;
}

#if !defined(REST_KMS)
static unsigned int psk_client_cb_kms(SSL *ssl, const char *hint, char *identity,
                                      unsigned int max_identity_len, unsigned char *psk, unsigned int max_psk_len)
{
    int ret;

    (void)(ssl); //unused; prevent gcc warning;

    if (!hint) {
        log_info("NULL received PSK identity hint, continuing anyway\n");
    } else {
        log_info("    -- Received PSK identity hint '%s'\n", hint);
    }

    ret = snprintf(identity, max_identity_len, "%s", psk_identity);
    if (ret < 0 || (unsigned int)ret > max_identity_len) {
        log_error("Error, psk_identify too long\n");
        return 0;
    }

    if (strlen(psk_key)>=(max_psk_len*2)) {
        log_error("Error, psk_key too long\n");
        return 0;
    }

    /* convert the PSK key to binary */
    ret = hex2bin(psk_key,psk);
    if (ret<=0) {
        log_error( "Error, Could not convert PSK key '%s' to binary key\n", psk_key);
        return 0;
    }
    return ret;
}
#endif


static int init_server(int *sock, int bobport, char *ip, int type)
{
    int ret = 0;
    struct sockaddr_in server;
    int s = -1;
    int j = 1;

    memset((char *)&server, 0, sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons((unsigned short)bobport);
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

static char* site_id(char* ip) {

	if (strcmp(ip, "192.168.9.120") == 0)
		return "A";
	else if (strcmp(ip, "192.168.9.121") == 0)
		return "B";
	else
		return "C";
} 

static int do_accept(int acc_sock, int *sock, char *siteid)
{
    int ret;
    static struct sockaddr_in from;
    int len;
    char siteip[16]; 

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
    
    char *addr = inet_ntoa(from.sin_addr); 
    strcpy(siteip, addr);
    siteip[strlen(addr)] = '\0';
    char * id = site_id(siteip);
    strcpy(siteid, id);
    siteid[1] = '\0';
    *sock = ret;
    return (1);
}



int server_body(int s, char *site_id)
{
    fd_set readfds;
    int ret = 1, width;
    int bytes_read;
    SSL *con = NULL;
    BIO *sbio;
    int read_blocked = 0;
    char hint[128];
    int i;
    char buf[128];
	char bufint[10];
	
	sprintf(bufint, "%d", file_counter);
	strcpy(filename, "bobdemo");
	strcpy(filename+strlen("bobdemo"), bufint);	
	filename[strlen("bobdemo") + strlen(bufint)] = '\0';
    fp = fopen(filename, "wb");
	++file_counter;
#if defined(REST_KMS)
    strcpy(gNC.peer_site_id, site_id);
    gNC.peer_site_id[1] = '\0';
    fetch_new_qkd_key(&gNC);
    snprintf(hint, 128, "%s %s %d", gNC.site_id, gNC.block_id, gNC.index);
    SSL_CTX_use_psk_identity_hint(ctx, hint);
    memcpy(keybuf, gNC.key, 32);
#else
    long dex = -1;
    fetch_key_from_kms(psk_client_cb_kms, port, keybuf, &dex);
    snprintf(hint, 64, "%ld", dex);
    SSL_CTX_use_psk_identity_hint(ctx, hint);
#endif
    SSL_CTX_set_psk_server_callback(ctx, psk_server_cb);
    con = SSL_new(ctx);
    SSL_clear(con);
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
#if 0
                bytes_read = SSL_read(con, &dex, 4);
                if (bytes_read != 0)
                    printf("Received %d from alice, bytes read %d \n", dex, bytes_read);
#endif
                bytes_read = SSL_read(con, buf, 128);
                if (bytes_read > 0) {
                    // printf("Received  bytes read %d \n",  bytes_read);
                    fwrite(buf, 1, bytes_read, fp);
                }
                //check SSL errors
                switch(SSL_get_error(con, bytes_read)) {
                case SSL_ERROR_NONE:
                    break;

                case SSL_ERROR_ZERO_RETURN:
                    //connection closed by client, clean up
                    log_info(" -- TLS Closed\n");
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
    }

err:
    if (con != NULL) {
        SSL_set_shutdown(con, SSL_SENT_SHUTDOWN | SSL_RECEIVED_SHUTDOWN);
        SSL_free(con);
        log_info(" -- Connection Closed\n");
    }

    if (ret >= 0)
        log_info(" -- Accept Connection\n");
    close(s);
    if (fp)
        fclose(fp);
    return (ret);
}


void print_usage() {
    printf("Usage: bob [-b port] [-f filename] \n");
    printf("Description:\n");
    printf("    -b portnum    port number on which Bob is listening\n");
    printf("    -f filename   Output file name\n");

}

static int serve() {
    int off = SSL_OP_NO_SSLv2;
    const SSL_METHOD *meth = NULL;
    int i, sock;
    int accept_socket = 0;
    char siteid[2];

    apps_startup();

    meth = TLSv1_server_method();
    ctx = SSL_CTX_new(meth);
    if (ctx == NULL) {
        log_error(" SSL_CTX_new error\n");
        return -1;
    }

    SSL_CTX_set_quiet_shutdown(ctx, 1);
    SSL_CTX_set_options(ctx, off);
    if (!SSL_CTX_set_cipher_list(ctx, cipher)) {
        ERR_print_errors(bio_err);
        return -2;
    }

    if (!init_server(&accept_socket, bport, NULL, SOCK_STREAM)) {
        log_error("Init server error\n");
        return (-3);
    }

    while(1) {
        if (do_accept(accept_socket, &sock, siteid) == 0) {
            close(accept_socket);
            return (0);
        }
        i = server_body(sock, siteid);
        if (i < 0) {
            close(accept_socket);
            return i;
        }
        close(sock);
    }
    return 0;
}

int main(int argc, char * argv[])
{

    const struct option longopts[] =
    {
        {"Bob's port",     optional_argument,  0, 'b'},
        {"File name",     optional_argument,  0, 'f'},

        {0,0,0,0},
    };

    short num = 0;
    int index;
    int iarg = 0;
    //turn off getopt error message
    opterr = 1;
    port = PORT;
    bport = BOB_PORT;

    //strcpy(filename, "bobdemo");
    //filename[strlen("bobdemo")] = '\0';

#if defined(REST_KMS)
    atexit(fn);
    prepare_kms_access(&gNC);
#endif

    while(iarg != -1)
    {
        iarg = getopt_long(argc, argv, "hf:b:p:", longopts, &index);

        switch (iarg)
        {
        case 'b':
            num = atoi(optarg);
            if (num != 0)
                bport = num;
            break;

        case 'f':
            if (optarg) {
                strcpy(filename, optarg);
                filename[strlen(optarg)] = '\0';
            }
            break;

        case 'h':
            print_usage();
            return -1;
        }
    }

    printf(" -- Bob is listening for incomming connections on port %d ...\n", bport);
#if !defined(REST_KMS)
    printf(" -- Bob's KMS listening for incomming connections on port %d ...\n", port);
#endif
    return serve();
}


