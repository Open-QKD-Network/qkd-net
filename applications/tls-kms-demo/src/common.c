#include "common.h"

char *cipher = "PSK-AES256-CBC-SHA";
char *psk_identity = "Client_identity";
char *psk_key = "1A1A1A1A1A1A1A1A1A1A1A1A1A1A1A1A";

#if defined(REST_KMS)
static const char *token_header = "authorization:Basic aHRtbDU6cGFzc3dvcmQ=";
static const char *token_post = "password=bot&client_secret=password&client=html5&username=pwebb&grant_type=password&scope=openid";
static const char *authorization_header = "Authorization: Bearer ";
static const char *query_str = "siteid=";

/* ETSI_api kms.conf*/
/*
http://localhost:9992/uaa/oauth/token
http://localhost:8095/api/v1/keys
http://localhost:8095/api/v1/keys
A
etsi_api
*/

/* UWaterloo api kms.conf*/
/*
http://localhost:9992/uaa/oauth/token
http://localhost:8095/api/newkey
http://localhost:8095/api/getkey
A
*/
void prepare_kms_access(struct Net_Crypto *m) {
  char *str = "/.qkd/kms/kms.conf";
  FILE *fp;
  char buffer[128] = { 0 };

  const char *homedir;
  homedir = getenv("HOME");
  char filestr[256] = { 0 };

  strncpy(filestr, homedir, 255);
  strncpy(filestr+strlen(homedir), str, 255 - strlen(homedir));

  fp = fopen(filestr, "r");
  if (!fp) {
    printf("**** Please check the file %s\n", filestr);
    exit (0);
  }

  fgets(buffer, sizeof(buffer), fp);
  buffer[strlen(buffer) - 1] = '\0';
  memset(m->uaa_url, 0, sizeof(m->uaa_url));
  strncpy(m->uaa_url, buffer, sizeof(m->uaa_url) - 1); // oauth

  fgets(buffer, sizeof(buffer), fp);
  buffer[strlen(buffer) - 1] = '\0';
  memset(m->newkey_url, 0, sizeof(m->newkey_url));
  strncpy(m->newkey_url, buffer, sizeof(m->newkey_url) - 1); // newkey

  fgets(buffer, sizeof(buffer), fp);
  buffer[strlen(buffer) - 1] = '\0';
  memset(m->getkey_url, 0, sizeof(m->getkey_url)); // getkey
  strncpy(m->getkey_url, buffer, sizeof(m->getkey_url) - 1);

  fgets(buffer, sizeof(buffer), fp);
  buffer[strlen(buffer) - 1] = '\0';
  memset(m->site_id, 0, sizeof(m->site_id)); // siteid
  strncpy(m->site_id, buffer, sizeof(m->site_id) - 1);

  memset(buffer, 0, sizeof(buffer));
  fgets(buffer, sizeof(buffer), fp);
  if (strlen(buffer) == 0)
      m->etsi_api = 0;
  else if (strncmp(buffer, "etsi_api", strlen("etsi_api")) == 0)
      m->etsi_api = 1;
  else m->etsi_api = 0;
  fclose(fp);

  curl_global_init(CURL_GLOBAL_ALL);
  m->curl_handle = gHandle = curl_easy_init();
}

#endif

static inline int cval(char c)
{
    if (c>='a') return c-'a'+0x0a;
    if (c>='A') return c-'A'+0x0a;
    return c-'0';
}

/* return value: number of bytes in out, <=0 if error */
int hex2bin(char *str, unsigned char *out)
{
    int i;
    int len = strlen(str);
    for(i = 0; i < len; i+=2) {
        if (!isxdigit(str[i])&& !isxdigit(str[i+1]))
            return -1;
        out[i/2] = (cval(str[i])<<4) + cval(str[i+1]);
    }
    return i/2;
}

static int init_client_ip(int *sock, unsigned char ip[4], int port, int type) {
    unsigned long addr;
    struct sockaddr_in them;
    int s,i;

    memset((char *)&them,0,sizeof(them));
    them.sin_family=AF_INET;
    them.sin_port=htons((unsigned short)port);
    addr=(unsigned long)
         ((unsigned long)ip[0]<<24L)|
         ((unsigned long)ip[1]<<16L)|
         ((unsigned long)ip[2]<< 8L)|
         ((unsigned long)ip[3]);
    them.sin_addr.s_addr=htonl(addr);

    s=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);

    if (s == INVALID_SOCKET) {
        perror("socket");
        return(0);
    }

    if (type == SOCK_STREAM)
    {
        i=0;
        i=setsockopt(s,SOL_SOCKET,SO_KEEPALIVE,(char *)&i,sizeof(i));
        if (i < 0) {
            perror("keepalive");
            return(0);
        }
    }

    if (connect(s,(struct sockaddr *)&them,sizeof(them)) == -1)
    {
        close(s);
        perror("connect");
        return(0);
    }
    *sock=s;
    return(1);
}

static int host_ip(char *str, unsigned char ip[4])
{
    unsigned int in[4];
    int i;

    if (sscanf(str,"%u.%u.%u.%u",&(in[0]),&(in[1]),&(in[2]),&(in[3])) == 4)
    {
        for (i=0; i<4; i++)
            if (in[i] > 255)
            {
                log_error("invalid IP address\n");
                goto err;
            }
        ip[0]=in[0];
        ip[1]=in[1];
        ip[2]=in[2];
        ip[3]=in[3];
    }

    return(1);
err:
    return(0);
}

int init_client(int *sock, char *host, int port, int type)
{
    unsigned char ip[4];

    memset(ip, '\0', sizeof ip);
    if (!host_ip(host,&(ip[0])))
        return 0;
    return init_client_ip(sock,ip,port,type);
}

void sha(char *ibuf, unsigned char obuf[20])
{
    SHA1((unsigned char*)ibuf, 32, obuf);
}

void bytetohex(uint8_t *byte, int bytelen, char *hex)
{
    int j;
    for(j = 0; j < bytelen; j++)
        sprintf(&hex[2*j], "%02X", byte[j]);
}

void apps_startup()
{
    ERR_load_crypto_strings();
    ERR_load_SSL_strings();
    OpenSSL_add_all_algorithms();
    SSL_library_init();
    bio_err = BIO_new_fp(stderr, BIO_NOCLOSE);
    bio_out = BIO_new_fp(stdout, BIO_NOCLOSE);
}

int fetch_key_from_kms(client_cb_kms kcb, short port, char buf[32], long *index) {
    const SSL_METHOD *meth = NULL;
    char *host="127.0.0.1";
    int s;
    SSL *con = NULL;
    BIO *sbio;
    SSL_CTX *ctx;


    if (init_client(&s,host,port,SOCK_STREAM) == 0) {
        log_error("connect:errno=%d\n",get_last_socket_error());
        return -1;
    }
    //log_info("TCP CONNECTED(%08X)\n",s);
    printf("    -- Successfully connected to KMS ...\n");

    meth = TLSv1_client_method();
    ctx = SSL_CTX_new(meth);
    if (ctx == NULL) {
        log_error(" SSL_CTX_new error\n");
        return -1;
    }

    SSL_CTX_set_psk_client_callback(ctx, kcb);
    SSL_CTX_set_cipher_list(ctx, cipher);

    con = SSL_new(ctx);
    SSL_CTX_set_cipher_list(ctx, cipher);
    sbio = BIO_new_socket(s, BIO_NOCLOSE);
    SSL_set_bio(con, sbio, sbio);
    SSL_set_connect_state(con);
    SSL_connect(con);

    SSL_write(con, index, 4);
    int k = SSL_read(con, index, 4);
    k = SSL_read(con, buf, 32);
#if 0
    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';
#endif
    if (k <= 0) {
        printf("    -- Looks like KMS ran out of keys to send\n");
        goto end;
    }
#if 0
    sha(buf, md);
    bytetohex(md, 20, (char*)mdhex);
    printf("    -- SHA1 of the received key at index %d : %s\n", *index, mdhex);
#endif
    SSL_shutdown(con);
    close(SSL_get_fd(con));

end:
    if (con != NULL)  SSL_free(con);
    if (ctx != NULL) SSL_CTX_free(ctx);
    if (bio_out != NULL) {
        BIO_free(bio_out);
        bio_out=NULL;
    }

    close(s);

    return 0;
}

#if defined(REST_KMS)

CURL *gHandle;
struct Net_Crypto gNC;

struct MemoryStruct {
    char *memory;
    int size;
};

struct Key {
    long index;
    unsigned char key[65];
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp)
{
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *)userp;

    mem->memory = realloc(mem->memory, mem->size + realsize + 1);
    if(mem->memory == NULL) {
        /* out of memory! */
        printf("not enough memory (realloc returned NULL)\n");
        return 0;
    }

    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

static CURLcode fetch_token(struct Net_Crypto *nc, struct MemoryStruct *chunk, const char *url,
                      const char *header, const char *post) {
    CURLcode res;
    struct curl_slist *list = NULL;
    CURL *handle;

    printf("HTTP-FETCH-REQUEST, url:%s,header:%s,post:%s\n\n", url, header, post);
    handle = nc->curl_handle;
    curl_easy_setopt(handle, CURLOPT_POST, 1L);
    curl_easy_setopt(handle, CURLOPT_URL, url);
    list = curl_slist_append(NULL, header);
    curl_easy_setopt(handle, CURLOPT_POSTFIELDS, post);
    curl_easy_setopt(handle, CURLOPT_HTTPHEADER, list);
    curl_easy_setopt(handle, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
    curl_easy_setopt(handle, CURLOPT_VERBOSE, 1L);
    curl_easy_setopt(handle, CURLOPT_WRITEDATA, (void *)chunk);
    curl_easy_setopt(handle, CURLOPT_NOPROGRESS, 1L);
    res = curl_easy_perform(handle);
    curl_slist_free_all(list);
    return res;
}

static CURLcode fetch(struct Net_Crypto *nc, struct MemoryStruct *chunk, const char *url,
                      const char *header, const char *post) {
    CURLcode res;
    struct curl_slist *list = NULL;
    CURL *handle;
    char* url2;

    printf("HTTP-FETCH-REQUEST, url:%s,header:%s,post:%s\n\n", url, header, post);
    handle = nc->curl_handle;
    if (nc->etsi_api == 1) {
      int url_len = strlen(url) + strlen("/") + strlen(post) + 1;
      url2 = (char *) malloc(url_len);
      memset(url2, 0, sizeof(url_len));
      sprintf(url2, "%s/%s", url, post);
      printf("ETSI-GET-URL:%s\n", url2);
      curl_easy_setopt(handle, CURLOPT_HTTPGET, 1L);
      curl_easy_setopt(handle, CURLOPT_URL, url2);
    } else {
      curl_easy_setopt(handle, CURLOPT_POST, 1L);
      curl_easy_setopt(handle, CURLOPT_URL, url);
      curl_easy_setopt(handle, CURLOPT_POSTFIELDS, post);
    }
    list = curl_slist_append(NULL, header);
    curl_easy_setopt(handle, CURLOPT_HTTPHEADER, list);
    curl_easy_setopt(handle, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
    curl_easy_setopt(handle, CURLOPT_VERBOSE, 1L);
    curl_easy_setopt(handle, CURLOPT_WRITEDATA, (void *)chunk);
    curl_easy_setopt(handle, CURLOPT_NOPROGRESS, 1L);
    res = curl_easy_perform(handle);
    curl_slist_free_all(list);
    if (nc->etsi_api == 1)
        free(url2);
    return res;
}

int get_key(struct Net_Crypto *nc, char *token, int is_new) {

    CURLcode res;
    struct MemoryStruct chunk;
    int err = 0;
    chunk.memory = malloc(1);
    chunk.size = 0;
    char hex[65];

    int len = strlen(authorization_header) + strlen(token) + 1;
    char *buf = (char*)malloc(len);
    strcpy(buf, authorization_header);
    strcpy(buf+strlen(authorization_header), token);
    buf[len] = '\0';
    if (is_new) {
      if (nc->etsi_api == 0) {
         int len_post = strlen(nc->peer_site_id) + strlen(query_str);
         char *post = (char*)malloc(len_post+1);

        strcpy(post, query_str);
        strcpy(post + strlen(query_str), nc->peer_site_id);
        post[len_post] = '\0';
        printf("key_post : %s\n", post);
        res = fetch(nc, &chunk, nc->newkey_url, buf, post);
      } else {
        // etsi api
        int geturl_parameter_len = strlen(nc->peer_site_id) + strlen("/enc_keys");
        char* geturl_parameter = (char *) malloc(geturl_parameter_len + 1);
        memset(geturl_parameter, 0, geturl_parameter_len + 1);
        sprintf(geturl_parameter, "%s/enc_keys", nc->peer_site_id);
        printf("etsi get url_parameter:%s\n", geturl_parameter);
        res = fetch(nc, &chunk, nc->newkey_url, buf, geturl_parameter);
        free(geturl_parameter);
      }
    } else {
      if (nc->etsi_api == 0) {
        char dex [sizeof(int)*8+1];
        sprintf (dex, "%d", nc->index);
        int len_post =  strlen(query_str) + strlen(nc->peer_site_id) + strlen("&index=") + strlen(dex) + strlen("&blockid=") + strlen(nc->block_id);
        char *post = (char*)malloc(len_post+1);
        strcpy(post, query_str);
        strcpy(post + strlen(query_str), nc->peer_site_id);
        strcpy(post + strlen(query_str) + strlen(nc->peer_site_id), "&index=");
        strcpy(post + strlen(query_str) + strlen(nc->peer_site_id) + strlen("&index="), dex);
        strcpy(post + strlen(query_str) + strlen(nc->peer_site_id) + strlen("&index=") + strlen(dex), "&blockid=");
        strcpy(post + strlen(query_str) + strlen(nc->peer_site_id) + strlen("&index=") + strlen(dex) + strlen("&blockid="), nc->block_id);
        post[len_post] = '\0';

        printf("post String %s\n", post);

        res = fetch(nc, &chunk, nc->getkey_url, buf, post);
        free(post);
      } else {
        // etsi api
        char dex [sizeof(int)*8+1];
        memset(dex, 0, sizeof(dex));
        sprintf (dex, "%d", nc->index);
        int geturl_parameter_len = strlen(nc->peer_site_id) + strlen("/dec_keys?key_ID=") + strlen(dex) + strlen("-") + strlen(nc->block_id);
        char *geturl_parameter = (char*) malloc(geturl_parameter_len + 1);
        memset(geturl_parameter, 0, geturl_parameter_len + 1);
        sprintf(geturl_parameter, "%s/dec_keys?key_ID=%d-%s", nc->peer_site_id, nc->index, nc->block_id);
        printf("etsi get url_parameter:%s\n", geturl_parameter);
        res = fetch(nc, &chunk, nc->getkey_url, buf, geturl_parameter);
        free(geturl_parameter);
      }
    }

    if(res != CURLE_OK) {
        fprintf(stderr, "curl_easy_perform() failed: %s\n",
                curl_easy_strerror(res));
    } else {
        json_object *keyobj = json_tokener_parse(chunk.memory);
        json_object *saved = keyobj;
        printf("HTTP-FETCH-RESPONSE:%*.*s\n\n", 0, chunk.size, (unsigned char *)chunk.memory);
        if (nc->etsi_api) {
            json_object *key_array;

            if (json_object_object_get_ex(keyobj, "keys", &key_array)) {
                int length = json_object_array_length(key_array);
                if (length == 0) {
                    printf("Empty key array!!!\n");
                    json_object_put(saved);
                    err = 1;
                    free(buf);
                    return err;
                }
                keyobj = json_object_array_get_idx(key_array, 0);
            } else {
                printf("No keys!!!\n");
                json_object_put(saved);
                err = 1;
                free(buf);
                return err;
            }
        }

        json_object_object_foreach(keyobj, key1, val1) {

            if (strcmp("error", key1) == 0 ) {
                err = 1;
                break;
            } else {
                if(strcmp("index", key1) == 0 ) {
                    if (val1) {
                        char *ptr;
                        nc->index = strtol((char *)json_object_get_string(val1), &ptr, 10);
                        printf("index: %d\n", nc->index);
                        if (nc->index < 0) {
                            printf("index is negative!\n");
                            err =1;
                        }
                    } else {
                        printf("index is empty!\n");
                        err = 1;
                    }
                } else if(strcmp("hexKey", key1) == 0 ) {
                    if (val1) {
                        memcpy(hex, (char *)json_object_get_string(val1), 64);
                        hex[64] = '\0';
                        printf("key: %s \n", hex);
                        hex2bin((char*)hex, (unsigned char*)nc->key);
                    } else {
                        printf("Key is empty!\n");
                        err = 1;
                    }
                } else if(strcmp("blockId", key1) == 0 ) {
                    if (val1) {
                        strcpy(nc->block_id, (char *)json_object_get_string(val1));
                        printf("blockId: %.*s \n", 36, nc->block_id);
                    } else {
                        printf("BlockdId is empty!\n");
                        err = 1;
                    }
                }
            }
        }
        json_object_put(saved);
    }
    free(buf);
    if (err) {
        if (is_new)
            printf("FAILED TO GET NEW KEY!!!\n");
        else
            printf("FAILED TO GET KEY!!!\n");
        exit(0);
    }
    return err;
}

int get_token(struct Net_Crypto *nc, char** token) {

    CURLcode res;
    struct MemoryStruct chunk;
    int err = 0;

    chunk.memory = malloc(1);
    chunk.size = 0;
    res = fetch_token(nc, &chunk, nc->uaa_url, token_header, token_post);

    if(res != CURLE_OK) {
        fprintf(stderr, "curl_easy_perform() failed: %s\n",
                curl_easy_strerror(res));
        err = 1;
    } else {
        json_object *jobj = json_tokener_parse(chunk.memory);
        printf("HTTP-FETCH-RESPONSE:%*.*s\n\n", 0, chunk.size, (unsigned char *)chunk.memory);
        json_object_object_foreach(jobj, key, val) {
            if (strcmp("error", key) == 0 ) {
                err =1;
                printf("key: %s, type of val: %s\n", key, (char *)json_object_get_string(val));
                break;
            } else if (strcmp("access_token", key) == 0 ) {
                int len = strlen((char *)json_object_get_string(val));
                *token = (char*)malloc(len+1);
                strcpy(*token, (char *)json_object_get_string(val));
                (*token)[len] = '\0';
            }
        }
        json_object_put(jobj);
    }

    free(chunk.memory);
    return err;
}

void fetch_new_qkd_key(struct Net_Crypto *nc) {
  char *token;
  get_token(nc, &token);
  get_key(nc, token, 1);
  free(token);
}

void fetch_qkd_key(struct Net_Crypto *nc) {
  char *token;
  get_token(nc, &token);
  get_key(nc, token, 0);
  free(token);
}

void fn(void) {
    curl_easy_cleanup(gHandle);
    curl_global_cleanup();
}
#endif
