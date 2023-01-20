#!/bin/sh

mvn install:install-file -Dfile=bcpkix-jdk18on-173b06.jar -DgroupId=org.bouncycastle -DartifactId=bcpkix-jdk18on -Dversion=173b06 -Dpackaging=jar
mvn install:install-file -Dfile=bctls-jdk18on-173b06.jar -DgroupId=org.bouncycastle -DartifactId=bctls-jdk18on -Dversion=173b06 -Dpackaging=jar
mvn install:install-file -Dfile=bcprov-jdk18on-173b06.jar -DgroupId=org.bouncycastle -DartifactId=bcprov-jdk18on -Dversion=173b06 -Dpackaging=jar
mvn install:install-file -Dfile=bcutil-jdk18on-173b06.jar -DgroupId=org.bouncycastle -DartifactId=bcutil-jdk18on -Dversion=173b06 -Dpackaging=jar
