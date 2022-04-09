## Use case
Application A runs on OpenQKDNetwork node A and application B runs on OpenqkdNetwork node B, application A and B want to use OpenQKDNetwork API to get key.
### Usage 1, TLS-PSK (Pre Shared Key)
Application A works as a client and makes a TLS connection to Application B with TLS PSK.
1. Application A knows its OpenQKDNetwork node id from configuration
2. Application B knows A's OpenQKDNetwork node id based on the A's IP address (there is mapping from IP address to OpenQKDNetwork ID)
3. Application B invokes REST new key API and puts the PSK hint (B's node id, key block id and key index id) into SSL server hello
4. Application A invoked REST get key API based on the PSK hint from SSL server hello from B
### Usage 2, qTOX
1. qTox A puts its own node id A into the key exchange message
2. qTox B invokes REST new key API and puts the key block id, key index id and B's node id into the key exchange message
3. qTox A invokes REST get key API with the key block id, key index id and B's node id from the key exchange message
### Usage 3, OpenSSL/libOQS/OpenQKDNetwork
Application A works as a client and makes a TLS connection to Application B with triple key exchange (OpenSSL key, libOQS key, OpenQKDNetwork key)
1. Application A puts new key url into SSL client hello message, the new key URL has A's node id
2. Application B invokes the REST new key API using the new key URL in SSL client hello message (Application B knows its own Node ID based on configuration)
3. Application B puts the getkey URL including B's node ID, key index id and block id into SSL server hello message
4. Application A invokes the REST get key API using the information from SSL server hello message
