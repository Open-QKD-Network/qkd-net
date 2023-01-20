### Environment
1. Ubuntu 22.04
2. Java SDK 11

https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-22-04

3. GRPC
```sh
root@ip-172-31-8-20:/home/ubuntu# apt list --installed | grep grpc

WARNING: apt does not have a stable CLI interface. Use with caution in scripts.

libgrpc++-dev/jammy,now 1.30.2-3build6 amd64 [installed]
libgrpc++1/jammy,now 1.30.2-3build6 amd64 [installed,automatic]
libgrpc-dev/jammy,now 1.30.2-3build6 amd64 [installed,automatic]
libgrpc10/jammy,now 1.30.2-3build6 amd64 [installed,automatic]
protobuf-compiler-grpc/jammy,now 1.30.2-3build6 amd64 [installed]
```
4. maven

5. screen
```sh
apt-get install screen
```

### Install BouncyCastle jar files
```sh
git clone https://github.com/Open-QKD-Network/bc-jars.git
cd bc-jars
./install.sh
```

### Build OpenQkdNetwork
```sh
git clone https://github.com/Open-QKD-Network/qkd-net.git
cd qkd-net
git checkout issue-34
cd qkd-net
cd kms
./script/build
```

### Remove old BouncyCastle jars
```sh
cd kms-api-gateway
mkdir test
cd test
jar xf ../target/kms-api-gateway-0.0.1-SNAPSHOT.jar
rm BOOT-INF/lib/bcpkix-jdk15on-1.60.jar
rm BOOT-INF/lib/bcprov-jdk15on-1.60.jar

#Create new jar
jar -cfM0 new.jar BOOT-INF/ META-INF/ org/
cp new.jar ../target/kms-api-gateway-0.0.1-SNAPSHOT.jar
```

### Build CQPToolKit
```sh
git clone --recurse-submodules https://github.com/Open-QKD-Network/cqptoolkit.git
cd cqptoolkit
git checkout issue-27
cd src/QKDInterfaces/proto/
git checkout issue-27
cd ../../../..
mkdir build-cqptoolkit
cd build-cqptoolkit
cmake -G Ninja ../cqptoolkit && ninja
```