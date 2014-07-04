#!/bin/bash

# fetch changes from github
git -C ${mms.github} pull

# build new software version
mvn -f ${mms.github}/pom.xml clean package assembly:assembly

# change file permissions manually because maven assembly does not do this
chmod -R ug+X ${mms.github}/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version}

# remove current software
rm -rf ${mms.home}

# move assembly to target directory
mv ${mms.github}/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version} ${mms.software.root}
