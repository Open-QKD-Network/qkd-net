#include <inttypes.h>
#include <stdio.h>

#include <openssl/err.h>
#include <openssl/rand.h>
#include <openssl/sha.h>




void sha(char *ibuf, char obuf[20])
{
    SHA1(ibuf, 32, obuf);
}

static void bytetohex(uint8_t *byte, int bytelen, char *hex)
{
    int j;
    for(j = 0; j < bytelen; j++)
        sprintf(&hex[2*j], "%02X", byte[j]);
}

static void hex2byte(const char *hex, uint8_t *byte)
{
    while (*hex) {
        sscanf(hex, "%2hhx", byte++);
        hex += 2;
    }
}
//gcc  opensslrand.c -o opensslrand -lcrypto
//gcc -I/opt/local/include -L/opt/local/lib  opensslrand.c -o opensslrand -lcrypto -lssl
//while true;do ./opensslrand; sleep 10; done
// "tail -f ~/workspace/tls-psk-server-client-example/test.bin" >> b
int
main()
{
    //uint32_t v;
    unsigned char v[32];
    unsigned char hex[65];
    hex[64] = '\0';

    if (RAND_bytes(v, 32) == 0) {
        ERR_print_errors_fp(stderr);
        return 1;
    }
    bytetohex(v, 32, hex);
    printf("%s\n",hex);

    FILE *out;
    out = fopen("keys","a");  // a for append, b for binary
    fwrite(hex, 64, 1, out); // write 32 bytes from our buffer
    fwrite("\n", 1, 1, out);
    fclose(out);

    unsigned char md[20];
    unsigned char mdhex[41];
    mdhex[40] = '\0';

    sha(v, md);
    bytetohex(md, 20, mdhex);
    printf("%s\n",mdhex);

    unsigned char buffer[32];
    unsigned char outhex[65];

#if 0
    FILE *in;
    in = fopen("test.bin","rb");  // r for read, b for binary
    fread(buffer, sizeof(buffer), 1, in);
    fclose(in);

    bytetohex(buffer, 32, outhex);
    printf("%s\n",outhex);
#endif

    return 0;
}
