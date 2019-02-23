KMS (Key Management Service) demo
=================================

Description
-----------

The tls-kms-demo comprises 2 user applications to demo a file transfer over a TLS channel using a pre-shared-key (PSK) obtained from KMS via API calls as described in [kms](https://github.com/open-qkd-net/qkd-net/kms):

1. *alice* - the client connecting to a file server to upload a file
2. *bob* - the file server accepting a file upload

Network topology for the demo assumes the two KMS's connected to their respective QKD hardware, which is connected through a direct QKD link, for getting keys. This simple setup avoids key routing as would be needed in a real world setup.

kms application serves keys to it's clients.

*alice* and *bob* programs are based on client-server architecture. *alice* can
be assumed to be a browser and *bob*, a webserver.

*alice* and *bob* communicate over TLS using PSK-AES256-CBC-SHA cipher suite.

*alice* and *bob* obtain their pre-shared keys from their respective kms. 

Following sequence of steps take place between *alice* and *bob* for establising
a TLS connection:


1. *alice* initiates connection with *bob*.
2. *bob* accepts *alice*'s connection.
3. *bob* initiates a connection with its kms for a fresh pre-shared
   secret key to be used to establish connection with *alice*.
4. *bob*'s kms replies with a pre-shared secret key and its index within the key
   bitstream.
5. *bob* sends the index of the key as a pre-shared key hint to *alice*.
6. *alice* receives the pre-shared secret key hint as index into the key bitstream.
7. *alice* initiates a connection with its kms, sending the index of the key
   received from *bob*, obtaining the exact pre-shared key that *bob* has.
8. *alice* uses the obtained key to establish a TLS connection with *bob*.

Pre-requisites
--------------
```
1. sudo apt-get install libjson-c-dev
2. sudo apt-get install openssl libssl-dev
3. sudo apt-get install libcurl4-openssl-dev
```
**Important note:** currently the site id of alice and bob is hard coded in the *site_id* funciton in *src/bob.c*.  You'll need to change the hardcoded IP addresses in that function to reflect your specific lab setup to make the demo work.

Build
-----

On command line cd to *qkd-net/applications/tls-kms-demo* folder and run the following command to build the above three applications:

```
make
```

Run
===

Configuration
-------------

Before running *alice* or *bob*, please make sure file *kms.conf* is available under

$HOME/.kms folder

Also make sure the *site id* of the KMS is properly set in the following files:
- $HOME/.qkd/kms/site.properties
- $HOME/.qkd/qnl/config.yaml
- $HOME/.kms/kms.conf

Help
----

For program options supplying -h will give various options the program can run
with e.g.:
```
./bob -h
```

*alice*
-----

*alice* connecting to *bob* running on port 10446 and ip address 129.97.41.204, and sending a file Whale.mp3 to *bob*:

```
./alice -b 10446 -i 129.97.41.204  -f data/Whale.mp3 
```

Output from *alice*:
```
-- Successfully conected to Bob
    -- Received PSK identity hint '5'
    -- Successfully connected to KMS ...
    -- Received PSK identity hint 'KMS'
```
*bob*
---

*bob* runs on port 10446 and talks to its kms.
For the demo purposes it outputs the data in a file called *bobdemo*.
```
./bob  -b 10446 -f bobdemo
```

Output from *bob* when *alice* connects and sends data:
```
 -- Bob is listening for incomming connections on port 10445 ...
 -- Bob's KMS listening for incomming connections on port 10446 ...
    -- Successfully connected to KMS ...
    -- Received PSK identity hint 'KMS'
    -- SHA1 of the received key : A0862843C0180AFA3AED0A77A08D5D2D8A0B27BE
```

Simulate QKD hardware
---------------------

If the hardware is scarce or unavailable, then key generation can be simulated
using opensslrand application. opensslrand emits out key bitstream in a file
called test.bin.

If opensslrand is running on one machine and kms on the other then opensslrand
could be run in a loop.
ssh can be used to tail test.bin to produce kms.bin which acts as bitstream for
kms:


e.g.

opensslrand running in a loop producing 32 bytes of key every 10 secs:


while true;do ./opensslrand; sleep 10; done

On kms machine following can be done to access test.bin to produce kms.bin:

ssh user@<ip address of the host running opensslrand> tail -F -n +1 <location of test.bin file> >> kms.bin 

 













