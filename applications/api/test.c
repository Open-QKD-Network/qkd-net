#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

#include "openqkd.h"

int main(int argc, char * argv[]) {

  int ret = 0;
  char *new_key_url = NULL;
  char *key = NULL, *get_key_url = NULL;
  int len = 0;

  if (argc != 3) {
    printf("Usage:use the following two options to run\n");
    printf("newkey newkeyurl\n");
    printf("getkey getkeyurl\n");
    return -1;
  }
  if (!strcmp(argv[1], "newkey")) {
    ret = oqkd_new_key(argv[2], &key, &len, &get_key_url);
    if (ret != 0) {
      printf("Fails to new key\n");
      return -1;
    } else {
      printf("Key is:%s\n", key);
      printf("GetkeyUrl:%s\n", get_key_url);
      free(key);
      free(get_key_url);
      return 0;
    }
  } else if (!strcmp(argv[1], "getkey")) {
    ret = oqkd_get_key(argv[2], &key, &len);
    if (ret != 0) {
      printf("Fails to get key\n");
      return -1;
    } else {
      printf("Key is:%s\n", key);
      free(key);
      return 0;
    }
  } else {
    printf("Usage:use the following two options to run\n");
    printf("newkey newkeyurl\n");
    printf("getkey getkeyurl\n");
    return -1;
  }
}
