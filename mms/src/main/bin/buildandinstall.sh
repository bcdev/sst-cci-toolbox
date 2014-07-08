#!/bin/bash

# fetch changes from github
currentdir=`pwd`
cd ${mms.github} && git pull
cd ${currentdir}

# build new software version
mvn -f ${mms.github}/pom.xml clean package assembly:assembly

# change file permissions manually because maven assembly does not do this
chmod -R ug+X ${mms.github}/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version}

# remove current software
rm -rf ${mms.home}

# move assembly to target directory
mv -f ${mms.github}/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version} ${mms.home}/..
