#ifndef OPENQKD_H
#define OPENQKD_H

int oqkd_get_new_key_url(char** new_key_url);
int oqkd_new_key(char* new_key_url, char**key, int* key_len, char** get_key_url);
int oqkd_get_key(char* get_key_url, char**key, int* key_len);
#endif