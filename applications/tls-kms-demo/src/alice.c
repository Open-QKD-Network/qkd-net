#include "common.h"

#define PORT            1044
#define BOB_PORT        10446

//while true;do rm -f bobdemo ;./alice -p 10443 -b 10446 -i 129.97.41.204  -f Whale.mp3;sleep 1; done

CONF *config = NULL;
BIO *bio_err = NULL;
BIO *bio_out = NULL;

static char filename[64];
static short port;

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

static unsigned int psk_client_cb(SSL *ssl, const char *hint, char *identity,
                                  unsigned int max_identity_len, unsigned char *psk, unsigned int max_psk_len)
{
    int ret;
    (void)(ssl); //unused; prevent gcc warning;

    if (!hint) {
        log_info("NULL received PSK identity hint, continuing anyway\n");
    } else {
        if (strcmp(hint, "-1") != 0)
            log_info("    -- Received PSK identity hint '%s'\n", hint);
    }

    if (strcmp(hint, "-1") == 0) {
        printf("    -- Looks like KMS ran out of keys to send\n");
        return 0;
    }

    char keybuf[32];
#if defined(REST_KMS)
    sscanf(hint, "%1s %36s %d", gNC.peer_site_id, gNC.block_id, &gNC.index);
    fetch_qkd_key(&gNC);
    memcpy(keybuf, gNC.key, 32);
#else
    long dex = atol(hint);
    fetch_key_from_kms(psk_client_cb_kms, port, keybuf, &dex);
#endif
    ret = snprintf(identity, max_identity_len, "%s", psk_identity);
    if (ret < 0 || (unsigned int)ret > max_identity_len) {
        log_error("Error, psk_identify too long\n");
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

void print_usage() {
    printf("Usage: alice [-b port] [-i ip address] [-f filename] \n");
    printf("Description:\n");
    printf("    -b portnum    port number on which Bob is listening\n");
    printf("    -i ipaddr     ip address of Bob's machine\n");
    printf("    -f filename   file to be sent over to Bob\n");
}


int main(int argc, char *argv[])
{
    short bport = BOB_PORT;
    port = PORT;
    char host[16];
    strcpy(host, "127.0.0.1");
    host[strlen("127.0.0.1")] = '\0';
    strcpy(filename, "alicedemo");
    filename[strlen("alicedemo")] = '\0';
    int ind;

    const SSL_METHOD *meth = NULL;
    int s, num;
    SSL *con = NULL;
    BIO *sbio;
    SSL_CTX *ctx;

    const struct option longopts[] =
    {
        {"Bob's port",     optional_argument,  0, 'b'},
        {"Bob's host",     optional_argument,  0, 'i'},
        {"File name",     optional_argument,  0, 'f'},
        {0,0,0,0},
    };

#if defined(REST_KMS)
    atexit(fn);
    prepare_kms_access(&gNC);
#endif

    int iarg = 0;
    //turn off getopt error message
    opterr = 1;
    while(iarg != -1)
    {
        iarg = getopt_long(argc, argv, "hf:b:i:p:", longopts, &ind);

        switch (iarg)
        {
        case 'b':
            num = atoi(optarg);
            if (num != 0)
                bport = num;
            break;

        case 'i':
            if (optarg) {
                strcpy(host, optarg);
                host[strlen(optarg)] = '\0';
            }
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



    apps_startup();

    if (init_client(&s,host,bport,SOCK_STREAM) == 0) {
        log_error("connect:errno=%d\n",get_last_socket_error());
        return -1;
    }
    //log_info("TCP CONNECTED(%08X)\n",s);
    printf(" -- Successfully conected to Bob\n");
    meth=TLSv1_client_method();
    ctx = SSL_CTX_new(meth);
    if (ctx == NULL) {
        log_error(" SSL_CTX_new error\n");
        return -1;
    }

    FILE* fp = fopen(filename, "rb+" );
    SSL_CTX_set_psk_client_callback(ctx, psk_client_cb);
    SSL_CTX_set_cipher_list(ctx, cipher);
    con = SSL_new(ctx);
    SSL_CTX_set_cipher_list(ctx, cipher);
    sbio = BIO_new_socket(s, BIO_NOCLOSE);
    SSL_set_bio(con, sbio, sbio);
    SSL_set_connect_state(con);
//  SSL_connect(con);
    if (SSL_connect(con) <= 0) {
        //printf("Connextion error\n");
        goto err;
    }

#if 1
    char buf[128];
    int nbytes;
    int bytes_written;
    int ret = 0;

    while(!feof(fp)) {
        nbytes = fread(buf, 1,128, fp);
        //printf("read %d bytes from file.\n", nbytes);
        if(nbytes > 0) {
            //  buf[nbytes]='\0';
            // printf("%s\n", buf);
            for (;;) {
                bytes_written = SSL_write(con, buf, nbytes);

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
    }
#endif
#if 0
    //the initial write seems to finish the handshake
//	SSL_write(con,cbuf,0);
    //sprintf(cbuf,"123456\n12345678\n");
    //sprintf(cbuf,"10");
    int dex = 973210;
    SSL_write(con, &dex, 4);
    dex = 10;
    SSL_write(con, &dex, 4);
    dex = 97;
    SSL_write(con, &dex, 4);
#endif

#if 0
    int index = 0;
    printf("Waiting for data from server...\n");
    //int k=SSL_read(con,sbuf,BUFSIZZ );
    int k = SSL_read(con, &index, 4);
    k = SSL_read(con,sbuf, 32);

    //sbuf[k]='\0';
    //printf("got %d bytes: %s\n",k,sbuf);


    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';

    if (k == 0) {
        printf("-- Looks like KMS ran out of keys to send\n");
        goto end;
    }


    sha(sbuf, md);
    bytetohex(md, 20, (char*)mdhex);
    printf("-- SHA1 of the received key : %s\n", mdhex);
    printf("-- Index of the key %d\n", index);

#endif
err:
    SSL_shutdown(con);
    close(SSL_get_fd(con));
    if (con != NULL)  SSL_free(con);
    if (ctx != NULL) SSL_CTX_free(ctx);
    if (bio_out != NULL) {
        BIO_free(bio_out);
        bio_out=NULL;
    }
    if (fp)
        fclose(fp);
    return (ret);
}
