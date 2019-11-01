KMS (Key Management Service) demo 1
======================================

Description
-----------

This KMS demo comprises 2 applications (*at the* **User** *layer in the 4-layer architecture*):

1. `alice`
2. `bob`

The QKD network topology for this demo assumes the two KMS peers connected to their respective QKD hardware, which are connected through a direct or indirect QKD link (i.e., **QLL** *in the 4-layer architecture*) to generate keys. In the case of an indirect QKD link, the **QNL** routing and key relay functions are indispensable.

The *KMS* serves keys to it's respective clients (`alice` and `bob`).  Note that there is no cross-site (or *intercite*) client-server relationship between KMS and its clients, i.e., only *intra-site* relationship exists between them.

The `alice` and `bob` programs are based on the client-server architecture.  For example, `alice` can
be assumed to be part of a browser and `bob`, part of a webserver.

`alice` and `bob` communicate over TLS using PSK-AES256-CBC-SHA cipher suite, and obtain their pre-shared keys from their respective KMS. 

The following sequence of steps take place between `alice` and `bob` for establising
a TLS connection:

1. `alice` initiates connection with `bob`.
2. `bob` accepts `alice`'s connection.
3. `bob` initiates a connection with its KMS, requesting for a fresh pre-shared
   secret key to be used to establish connection with `alice`.
4. `bob`'s KMS replies with a pre-shared secret key and its *index* within the key
   bitstream.
5. `bob` sends the index of the key as a *pre-shared key hint* to `alice`.
6. `alice` receives the pre-shared secret key hint as index into the key bitstream.
7. `alice` initiates a connection with its KMS, sending the index of the key
   received from `bob`, and consequently obtains the exact pre-shared key that `bob` has.
8. `alice` uses the obtained key to establish a TLS connection with `bob`.

Pre-requisites
--------------
```shell
> sudo apt-get install libjson-c-dev
> sudo apt-get install openssl libssl-dev
> sudo apt-get install libcurl4-openssl-dev
```

Build
-----

On command line cd to this tls-kms-demo folder and run the following command to build the above three applications:
```shell
make clean
make
```

Run
===

Configuration
-------------

Before running `alice` or `bob`, please make sure file `KMS.conf` is available under

`$HOME/.qkd/kms` folder

Also make sure id of the KMS is properly set.

It is specified on the last line in the file KMS.conf. 



Help
----

For program options supplying -h will give various options the program can run
with e.g.:

`./bob -h`

`alice`
-----

`alice` connecting to `bob` running on a specified port (e.g. 10446) and ip address 129.97.41.204, and sending a file Whale.mp3 to `bob`:


`./alice -b 10446 -i 129.97.41.204  -f Whale.mp3`


Output from `alice` looks like this:
```
-- Successfully conected to bob
    -- Received PSK identity hint '5'
    -- Successfully connected to KMS ...
    -- Received PSK identity hint 'KMS'
```

`bob`
---

For the same example, `bob` runs on port 10446 and talking to its KMS running on port 10445 (configurable in the KMS).
For the demo purposes it outputs the data in a file called `bobdemo`.

`./bob  -b 10446 -f bobdemo`

Output from `bob` when `alice` connects and sends data looks like this:

```
 -- bob is listening for incomming connections on port 10445 ...
 -- bob's KMS listening for incomming connections on port 10446 ...
    -- Successfully connected to KMS ...
    -- Received PSK identity hint 'KMS'
    -- SHA1 of the received key : A0862843C0180AFA3AED0A77A08D5D2D8A0B27BE
```

Simulate QKD hardware
---------------------

If the hardware is scarce or unavailable, then key generation can be simulated
using the `opensslrand` application, which emits out key bitstream in a file
called test.bin.

If opensslrand is running on one machine and KMS on the other then opensslrand
could be run in a loop.
ssh can be used to tail test.bin to produce KMS.bin which acts as bitstream for
KMS:


e.g. `opensslrand` running in a loop producing 32 bytes of key every 10 secs:

```shell
while true;do ./opensslrand; sleep 10; done
```

On KMS machine, the following can be done to access test.bin to produce KMS.bin:

`ssh user@{ip address of the host running opensslrand} tail -F -n +1 {location of test.bin file} >> KMS.bin `

 **TODO: One additional step needs to be done to convert `KMS.bin` into the key file format acceptable by the current version of KMS/QNL software in the master branch. **













