#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <curl/curl.h>
#include <json-c/json.h>

#include "openqkd.h"

typedef struct MemoryStruct {
  char* memory;
  int size;
} MemoryStruct;

static int get_urls(char** new_key_url, char** get_key_url, char** site_id) {
  char *str = "/.qkd/kms/kms.conf";
  FILE *fp;
  char buffer[128];

  const char *homedir;
  homedir = getenv("HOME");
  char filestr[256];
  strcpy(filestr, homedir);
  strcpy(filestr+strlen(homedir), str);
  filestr[strlen(homedir) + strlen(str)] = '\0';

  fp = fopen(filestr, "r");
  if (!fp) {
    printf("**** Please check the configuration file %s\n", filestr);
    return (-1);
  }

  /*oauth*/
  fgets(buffer, sizeof buffer, fp);

  /*newkey*/
  fgets(buffer, sizeof buffer, fp);
  if (new_key_url != NULL) {
    /*buffer includes the \n*/
    buffer[strlen(buffer) - 1] = '\0';
    *new_key_url = malloc(strlen(buffer) + 1);
    memset(*new_key_url, 0, strlen(buffer) + 1);
    memcpy(*new_key_url, buffer, strlen(buffer));
  }

  /*getkey*/
  fgets(buffer, sizeof buffer, fp);
  if (get_key_url != NULL) {
    buffer[strlen(buffer) - 1] = '\0';
    *get_key_url = malloc(strlen(buffer) + 1);
    memset(*get_key_url, 0, strlen(buffer) + 1);
    memcpy(*get_key_url, buffer, strlen(buffer));
  }

  /*siteid*/
  fgets(buffer, sizeof buffer, fp);
  if (site_id != NULL) {
    buffer[strlen(buffer) - 1] = '\0';
    *site_id = malloc(strlen(buffer) + 1);
    memset(*site_id, 0, strlen(buffer) + 1);
    memcpy(*site_id, buffer, strlen(buffer));
  }

  fclose(fp);

  return 0;
}

static int get_remote_siteagent_url(char** get_key_url, char* remotesiteId) {
  // Reads AND modifies get_key_url from value in kms.conf

  int malformatted_url = 0;
  char* addrend = strchr(*get_key_url, ':');
  if (addrend == NULL) {
      malformatted_url = 1;
  } else {
    addrend = strchr(addrend + 1, ':');
    if (addrend == NULL) {
        malformatted_url = 1;
    }
  }
  if (malformatted_url) {
      printf("get_key_url: %s is malformatted\n", *get_key_url);
      return -1;
  }
  //Copy path portion of url from get_key_url
  char* url_path = malloc(addrend - *get_key_url);
  strcpy(url_path, addrend);
  printf("URL path: %s\n", url_path);

  char *str = "/.qkd/mapping.log";
  FILE *fp;

  const char *homedir;
  homedir = getenv("HOME");
  char filestr[256];
  strcpy(filestr, homedir);
  strcpy(filestr+strlen(homedir), str);
  filestr[strlen(homedir) + strlen(str)] = '\0';

  fp = fopen(filestr, "r");
  if (!fp) {
    printf("Could not open the file %s\n", filestr);
    return (-1);
  }

  fseek(fp, 0, SEEK_END);
  long fsize = ftell(fp);
  fseek(fp, 0, SEEK_SET);

  char *jsonString = malloc(fsize + 1);
  fread(jsonString, 1, fsize, fp);
  fclose(fp);
  jsonString[fsize] = 0;
 
  json_object *mappingobj = json_tokener_parse(jsonString);
  free(jsonString);
  json_object_object_foreach (mappingobj, key, val) {
    const char* site = json_object_get_string(val);
    if (strcmp(remotesiteId, site) == 0) {
        const char* remotesiteaddress = key;
        free(*get_key_url);
        char* scheme = "http://";
        int scheme_len = strlen(scheme);
        int address_len = strlen(remotesiteaddress);
        *get_key_url = malloc(scheme_len + address_len + sizeof(url_path));
        strcpy(*get_key_url, scheme);
        memcpy(*get_key_url + scheme_len, remotesiteaddress, address_len);
        strcpy(*get_key_url + scheme_len + address_len, url_path);

        json_object_put(mappingobj); 
        free(url_path);

        return 0;
    }
  }
  
  json_object_put(mappingobj);
  free(url_path);

  printf("Could find site %s in %s\n", remotesiteId, filestr);
  return -1;
}

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp)
{
  size_t realsize = size * nmemb;
  MemoryStruct *mem = (MemoryStruct *)userp;

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

static int fetch(MemoryStruct *chunk, char* url, const char*post) {
  CURLcode res;
  CURL *handle;
  int ret = 0;

  curl_global_init(CURL_GLOBAL_ALL);
  handle = curl_easy_init();
  printf("HTTP-FETCH-REQUEST, url:%s,post content:%s\n\n", url, post);
  curl_easy_setopt(handle, CURLOPT_POST, 1L);
  curl_easy_setopt(handle, CURLOPT_URL, url);
  curl_easy_setopt(handle, CURLOPT_POSTFIELDS, post);
  curl_easy_setopt(handle, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
  curl_easy_setopt(handle, CURLOPT_VERBOSE, 0L);
  curl_easy_setopt(handle, CURLOPT_WRITEDATA, (void *)chunk);
  curl_easy_setopt(handle, CURLOPT_NOPROGRESS, 1L);
  res = curl_easy_perform(handle);
 
  if (res != CURLE_OK) {
    printf("curl_easy_perform failed:%s\n", curl_easy_strerror(res));
    ret = -1;
  }

  curl_easy_cleanup(handle);
  curl_global_cleanup();
  return ret;
}

int oqkd_get_new_key_url(char** new_key_url) {
  char *new_key = NULL, *site_id = NULL;
  int ret = 0;
  int len = 0;

  if ((ret = get_urls(&new_key, NULL, &site_id)) != 0) {
    printf("Fails to get key url\n");
    return -1;
  }
  printf("new_key:%s, site_id:%s\n", new_key, site_id);
  // newkey
  len = strlen(new_key) + strlen("&siteid=") + 1;
  *new_key_url = malloc(len);
  memset(*new_key_url, 0, len);
  sprintf(*new_key_url, "%s&siteid=%s", new_key, site_id);

  free(new_key);
  free(site_id);
  return 0;
}

int oqkd_new_key(char* new_key_url, char**key, int* key_len, char** get_key_url) {
  char *new_key = NULL, *get_key = NULL;
  int ret = 0;
  long index = 0;
  char hex[65] = {0};
  char block_id[37] = {0};
  char dex[sizeof(int)*8 +1];
  int len = 0;
  char* mySiteId = NULL;
  char* siteId = NULL;
  MemoryStruct chunk;
  chunk.memory = malloc(1);
  chunk.size = 0;

  printf("QKD_NEW_KEY from %s\n", new_key_url);
  ret = get_urls(NULL, &get_key, &mySiteId);
  if (ret != 0) {
    printf("Fails to get key urls\n");
    return -1;
  }
  /*assume client C makes connection to server B, 
  on B side, the new_key_url is http://localhost:8089/api/newkey&siteid=C*/
  siteId = strchr(new_key_url, '&');
  if (siteId == NULL) {
    printf("No siteid in new key url:%s\n", new_key_url);
    return -1;
  }
  char* remotesiteId = strchr(siteId, '=') + 1; 
  ret = get_remote_siteagent_url(&get_key, remotesiteId);
  if (ret != 0) {
    printf("Remote siteagent could not be found (mapping.log misconfigured; check routes.json)");
  }
  new_key = malloc(siteId - new_key_url + 1);
  memset(new_key, 0, siteId - new_key_url + 1);
  memcpy(new_key, new_key_url, siteId - new_key_url);
  siteId++; // move &

  ret = fetch(&chunk, new_key, siteId);
  if (ret != 0) {
    printf("Fails to get new key\n");
    return -1;
  }
  json_object *keyobj = json_tokener_parse(chunk.memory);
  printf("HTTP-FETCH-RESPONSE:%*.*s\n\n", 0, chunk.size, (unsigned char *)chunk.memory);

  json_object_object_foreach(keyobj, key1, val1) {
    if (strcmp("error", key1) == 0 ) {
      ret = -1;
      break;
    } else {
      if(strcmp("index", key1) == 0 ) {
        char *ptr;
        index = strtol((char *)json_object_get_string(val1), &ptr, 10);
        printf("index: %ld\n", index);
        sprintf(dex, "%ld", index);
      } else if(strcmp("hexKey", key1) == 0 ) {
        memcpy(hex, (char *)json_object_get_string(val1), 64);
        hex[64] = '\0';
        printf("key: %s \n", hex);
        *key = malloc(64);
        memcpy(*key, hex, 64);
        *key_len = 64;
      } else if(strcmp("blockId", key1) == 0 ) {
        strcpy(block_id, (char *)json_object_get_string(val1));
        printf("blockid: %.*s \n", 36, block_id);
      }
    }
  }
  json_object_put(keyobj);

  len = strlen(get_key) + strlen("&siteid=") + strlen(mySiteId) +
      strlen("&index=") + strlen(dex) + strlen("&blockid=") + strlen(block_id) + 1;
  *get_key_url = malloc(len);
  memset(*get_key_url, 0, len);
  sprintf(*get_key_url, "%s&siteid=%s&index=%s&blockid=%s", get_key, mySiteId, dex, block_id);

  printf("Return get_key_url:%s\n", *get_key_url);
  return ret;
}

int oqkd_get_key(char* get_key_url, char**key, int* key_len) {
  int ret = 0;
  char* getKey = NULL;
  char* siteId = NULL;
  char* post = NULL;
  char *temp = get_key_url;
  int len = 0;
  long index = 0;
  char hex[65] = {0};
  char block_id[37] = {0};
  char dex[sizeof(int)*8 +1];
  MemoryStruct chunk;
  chunk.memory = malloc(1);
  chunk.size = 0;

  printf("QKD_GET_KEY from %s\n", get_key_url);
  /*http://localhost:8095/api/getkey&siteid=B&index=0&blockid=sadfsdf*/
  if (get_key_url == NULL) {
    printf("get_key_url is NULL\n");
    return -1;
  }
  siteId = strchr(temp, '&');
  if (siteId == NULL) {
    printf("siteid is not in get_key_url:%s\n", get_key_url);
    return -1;
  }
  len = siteId - temp + 1;
  getKey = malloc(len);
  memset(getKey, 0, len);
  memcpy(getKey, temp, len - 1); // get key

  temp += len; // move over key temp = siteid=B&index=0&blockid=sadfdf
  len = strlen(temp) + 1;
  post = malloc(len);
  memset(post, 0, len);
  memcpy(post, temp, len - 1);

  ret = fetch(&chunk, getKey, post);
  if (ret != 0) {
    printf("Fails to get key with url:%s\n", get_key_url);
    return -1;
  }
  json_object *keyobj = json_tokener_parse(chunk.memory);
  printf("HTTP-FETCH-RESPONSE:%*.*s\n\n", 0, chunk.size, (unsigned char *)chunk.memory);

  json_object_object_foreach(keyobj, key1, val1) {
    if (strcmp("error", key1) == 0 ) {
      ret = -1;
      break;
    } else {
      if(strcmp("index", key1) == 0 ) {
        char *ptr;
        index = strtol((char *)json_object_get_string(val1), &ptr, 10);
        printf("index: %ld\n", index);
        sprintf(dex, "%ld", index);
      } else if(strcmp("hexKey", key1) == 0 ) {
        memcpy(hex, (char *)json_object_get_string(val1), 64);
        hex[64] = '\0';
        printf("key: %s \n", hex);
        *key = malloc(64);
        memcpy(*key, hex, 64);
        *key_len = 64;
      } else if(strcmp("blockId", key1) == 0 ) {
        strcpy(block_id, (char *)json_object_get_string(val1));
        printf("blockid: %.*s \n", 36, block_id);
      }
    }
  }
  json_object_put(keyobj); 
  return ret;
}
