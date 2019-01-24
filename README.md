qkd-net
=======

Overview
--------

qkd-net project aims to provide a robust quantum key distribution network infrastructure.
A very flexible layered approach has been adopted adhering to SOLID principles,  
https://en.wikipedia.org/wiki/SOLID_(object-oriented_design).

Microservice based architecture approach has been taken for software design and
and implementation. One benefit of it is to have flexibility in production deployment such as being cloud-ready.

Layers
------

1. Key Management System (KMS)
2. Quantum Network Layer (QNL)
3. Quantum Link Layer (QLL)

The fourth layer is the User (or Host) Layer, which belongs to the client of this system, and thus is beyond the scope of this project.

###  Key Management System (KMS)

KMS layer provides an application interface for fetching keys for symmetric
cryptography.

KMS layer comprises following microservices:

1. Configuration service
2. Registration Service
3. Authorization Service
4. KMS Service
5. KMS API Gateway Service
6. KMS QNL Service

### Quantum Network Layer (QNL)

QNL is the core compoent to extend QKD technology from point-to-point to network level. It is the middle layer sandwiched between KMS layer and QLL (Quantum Link Layer).
QNL provides key blocks to KMS after fetching it from QLL. QNL, thus, keeps KMS
layer hardware agnostic and at the same time allowing itself the flexibility to
generate key bocks through alternative mechanisms should QLL fail to meet the
needs of KMS layer.  

QNL currently consists of a single microservice:

1. Key Routing Service

Key routing service not only pushes key blocks to KMS QNL service for KMS
service but also responds to pull requests for key blocks directly from KMS service.

### QKD Link Layer (QLL)

Ideally, QLL consists of working QKD devices. A QLL simulator is provided in this project for situations where real QKD devices based links are unavailable, e.g., a planned satellite based QKD link between two nodes that are geographically far away.

#### Prerequisites (Required)

Install following before proceeding.
Make sure the executables are in system path.

1. Java SDK 8 or later - http://www.oracle.com/technetwork/java/javase/downloads/index.html
2. Maven - https://maven.apache.org/ or apt-get, if on Ubuntu
3. screen - apt-get, if on Ubuntu
4. git - apt-get, if on Ubuntu

#### Prerequisites for testing (Optional)

On a Linux(Ubuntu) system apt-get can be used to install the programs.

1. Curl
2. jq

#### Configuration Files

##### kms.conf

Applications like qTox and tls-demo require kms.conf to be present under
$HOME/.kms.

This file can be manually created by puting following entries:

http://localhost:9992/uaa/oauth/token<br/>
http://localhost:8095/api/newkey<br/>
http://localhost:8095/api/getkey<br/>
C<br/>


Depending on where the KMS node is, localhost is replaced by the IP address of
the host. Last line represents the KMS site id. It should be updated with the
correct site id. Above is just an example.

##### config-repo

config-repo directory under $HOME/ contains all the configuration property
files required by all the microservices.

All the files reside under <top level directory>/qkd-net/kms/config-repo from where
they are copied to $HOME/config-repo and checked in local git repository.

##### site.properties

This file is located under <top level directory>/qkd-net/kms/kms-service/src/main/resources/.

Explanation of site specific configuration properties used by KMS service

//Number of keys per block<br/>
kms.keys.blocksize=1024<br/>
//Size of a key in bytes<br/>
kms.keys.bytesize=32<br/>
//Top level location for locating key blocks<br/>
kms.keys.dir=${HOME}/.qkd/kms/pools/<br/>
//IP address of the host where key routing service is running<br/>
qnl.ip=localhost<br/>
//Port on which key routing service is listening for key block requests<br/>
qnl.port=9292<br/>

##### config.yaml (KMS QNL Service)


This file is copied from <top level directory>/qkd-net/kms/kms-qnl-service/src/main/resources/
to $HOME/.qkd/kms/qnl from where it is used by KMS QNL service


Expalantion of  configuration properties used by KMS QNL service.

//Port on which KMS QNL service is listening<br/>
port: 9393<br/>
//Size of a key in bytes<br/>
keyByteSz: 32<br/>
//Number of keys in a block. Key routing service provides KMS keys in blocks of size<br/>   
//keyBlockSz<br/>
keyBlockSz: 1024<br/>
//Location where KMS QNL service puts the pushed key blocks from key<br/>
//routing service<br/>
poolLoc: kms/pools<br/>

##### config.yaml and routes.json (Key Routing Service)

###### routes.json:

This file is copied from <top level directory>/qkd-net/qnl/conf/ to
$HOME/.qkd/qnl.

Topology information is contained in route.json.
Currently this information is populated manually.
There are two sections here, one mentioning nodes which are adjacent
and the other, non-adjacent. Adjacent nodes section contains the name
of the node as key and it's IP address as the value.
Non-adjacent nodes section contians name of the node as key and the name of
the node it's reachable from as value. Key's value here will always be one
of the nodes mentioned in the adjacent nodes section.   

###### config.yaml:

This file is copied from <top level directory>/qkd-net/qnl/conf/ to
$HOME/.qkd/qnl.

Contains various configuration parameters for the key routing service
running as a network layer.

Brief explanation of various properties:

Example file:

//Base location for finding other paths and configuration files.<br/>
base: .qkd/qnl<br/>
//Route configuration file name.<br/>
routeConfigLoc: routes.json<br/>
//Location where QLL puts the key blocks for QNL to carve out key blocks for KMS.<br/>
qnlSiteKeyLoc: qll/keys<br/>
//Each site has an identifier uniquely identifying that site.<br/>
siteId: A<br/>
//Port on which key routing service is listening for key block requests.<br/>
port: 9292<br/>
//Size of a key in bytes.<br/>
keyBytesSz: 32<br/>
//Number of keys in a block. Key routing service provides KMS keys in blocks of size.<br/>   
//keyBlockSz<br/>
keyBlockSz: 1024<br/>
//Key routing service expects QLL to provide keys in blocks of qllBlockSz.<br/>
qllBlockSz: 4096<br/>
//IP address of the host running KMS QNL service.<br/>
kmsIP: localhost<br/>
//Port on which KMS QNL service is listening.<br/>
kmsPort: 9393<br/>
//One Time Key configuration.<br/>
OTPConfig:<br/>
 //Same as above.<br/>
 keyBlockSz: 1024<br/>
 //Location of the OTP key block.<br/>
 keyLoc: otp/keys<br/>


#### How to build and install services

For building the services,

cd <top level director>/qkd-net/kms<br/>
./scripts/build

For running the services

cd <top level director>/qkd-net/kms<br/>
./scripts/run

#### Checking registration service

  http://localhost:8761/

#### Testing KMS service

Since OAuth is enabled, first access token is fetched:

curl -X POST -H"authorization:Basic aHRtbDU6cGFzc3dvcmQ=" -F"password=bot" -F"client_secret=password" -F"client=html5" -F"username=pwebb" -F"grant_type=password" -F"scope=openid"  http://localhost:9992/uaa/oauth/token | jq

E.g. output:

{
  "access_token": "291a94d5-d624-4269-a2bb-07db62130bb3",
  "token_type": "bearer",
  "refresh_token": "99d4cdd9-641e-4290-ac4b-865b1d7068a6",
  "expires_in": 43017,
  "scope": "openid"
}

Once the access token is available different endpoints can be accessed.

Assuming we have 2 sites setup with siteid as A and B repsectively.
Some application from site A initiates a connection to an application on
site B.
Pre-shared key fetching can be simulated using the two calls below.  


There are two endpoints:

1. New Key

Application at site B makes a newkey call.

Request

  Method :      POST
  URL path :    /api/newkey
  URL params:   siteid=[alhpanumeric] e.g. siteid=A

Response
  Format JSON

{
  index: Index of he key
  hexKey: Key in hexadecimal format
  blockId: Id of the block containing the key
}

e.g. request-response:

curl 'http://localhost:8095/api/newkey?siteid=A' -H"Authorization: Bearer 291a94d5-d624-4269-a2bb-07db62130bb3" | jq

{
  "index": 28,
  "hexKey": "a2e1ff3429ff841f5d469893b9c28cbcb586d55c8ecf98c83c704824e889fc43"
  "blockId": ""
}

2. Get Key

Application on site A makes the getkey call by using the response from the
newkey call above.

NOTE:
Since the sites are different hence OAuth tokens will be different. Which
means a new OAuth token like above is fetched first.

Request

  Method :      POST
  URL path :    /api/getkey
  URL params:   siteid=[alhpanumeric] e.g. siteid=B
                blockid=
                index=[Integer]
Response
  Format JSON

  {
    index: Index of he key
    hexKey: Key in hexadecimal format
    blockId: Id of the block containing the key
  }

e.g. request-response:

curl 'http://localhost:8095/api/getkey?siteid=B&index=1&blockid=' -H"Authorization: Bearer abcdef12-d624-4269-a2bb-07db62130bb3" | jq

{
  "index": 1,
  "hexKey": "4544a432fb045f4940e1e2fe005470e1a35d85ede55f78927ca80f46a0a4b045"
  "blocId": ""
}

TEAM
----

The Open QKD Network project is led by Professors [Michele Mosca](http://faculty.iqc.uwaterloo.ca/mmosca/) and [Norbert Lutkenhaus[(http://services.iqc.uwaterloo.ca/people/profile/nlutkenh) at Institute for Quantum Computing (IQC) of the University of Waterloo.

### Contributors

Contributors to this master branch of qkd-net include:

- Shravan Mishra (University of Waterloo)
- Xinhua Ling (University of Waterloo)
