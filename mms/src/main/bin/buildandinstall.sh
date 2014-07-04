#!/bin/bash

currentdir=`pwd`

# change to source tree
cd ${mms.github}

# fetch changes from github
git pull

# build new software version
mvn clean package assembly:assembly

# change file permissions manually because maven assembly does not do this
chmod -R ug+X target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version}

# remove current software
rm -rf ${mms.home}

# move assembly to target directory
mv target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version} ${mms.software.root}

cd ${currentdir}
