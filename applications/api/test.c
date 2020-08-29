#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

#include "openqkd.h"

int main(int argc, char * argv[]) {

  int ret = 0;
  char *new_key_url = NULL;
  char *key = NULL, *get_key_url = NULL;
  int len = 0;

#if 0
  ret = oqkd_get_new_key_url(&new_key_url);
  if (ret == 0) {
      printf("new_key_url:%s\n", new_key_url);
  } else {
      printf("Fails to get new_key_url\n");
      return ret;
  }
  ret = oqkd_new_key(new_key_url, &key, &len, &get_key_url);
  if (ret ==0) {
    printf("new key succeeds, key len:%d, get_key_url:%s\n", len, get_key_url);
    free(new_key_url);
    free(get_key_url);
    free(key);
  } else {
    printf("Fails to new key\n");
  }
#else
  ret = oqkd_get_key("http://localhost:8095/api/getkey&siteid=B&index=0&blockid=f2febd3d-fcb4-4410-a522-66c672227d4c",
      &key, &len);
  if (ret == 0) {
    printf("oqkd_get_key succeed: len=%d\n", len);
    return 0;
  } else {
    return ret;
  }
#endif
  return ret;
}
