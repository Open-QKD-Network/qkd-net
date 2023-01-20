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