# ESA SST-CCI Toolbox

## Overview

To be completed.

## Setting up the developer environment on CEMS

1. Append the following snippet to your '.bash_profile'
>
   export JAVA_HOME='/usr/java/jdk1.7.0_51'
   export PATH=$HOME/bin:$JAVA_HOME/bin:$PATH
   export http_proxy=wwwcache.rl.ac.uk:8080
   export https_proxy=wwwcache.rl.ac.uk:8080
>

2. Include the following snippet in your '.m2/settings.xml' file and adapt the username
>
   <settings>
     <localRepository>/group_workspaces/cems2/esacci_sst/mms/m2/repository/</localRepository>
     <servers>
       <server>
         <id>bc-mvn-repo-public</id>
         <username>maven</username>
         <privateKey>/home/<ADAPT USERNAME>/.m2/id_rsa</privateKey>
         <passphrase><ADAPT PASSPHRASE></passphrase>
         <filePermissions>664</filePermissions>
         <directoryPermissions>775</directoryPermissions>
       </server>
       <server>
         <id>bc-mvn-repo-closed</id>
         <username>maven-cs</username>
         <privateKey>/home/<ADAPT USERNAME>/.m2/id_rsa</privateKey>
         <passphrase><ADAPT PASSPHRASE></passphrase>
         <filePermissions>664</filePermissions>
         <directoryPermissions>775</directoryPermissions>
       </server>
     </servers>
     <proxies>
       <proxy>
         <active>true</active>
         <protocol>http</protocol>
         <host>wwwcache.rl.ac.uk</host>
         <port>8080</port>
       </proxy>
       <proxy>
         <active>true</active>
         <protocol>https</protocol>
         <host>wwwcache.rl.ac.uk</host>
         <port>8080</port>
       </proxy>
     </proxies>
     <profiles>
       <profile>
         <id>compiler</id>
           <properties>
             <java.home>/group_workspaces/cems2/esacci_sst/mms/software/java/jdk1.7.0_51</java.home>
           </properties>
       </profile>
     </profiles>
     <activeProfiles>
       <activeProfile>compiler</activeProfile>
     </activeProfiles>
   </settings>
>   

## How to copy data to CEMS?

Edit your .ssh/configuration file
>
    Host            cems-login
    HostName        comm-login1.cems.rl.ac.uk
    User            <your user name>
    ForwardX11      no
    ForwardAgent    yes

Then use e.g. rsync to copy directories of data

    rsync -av -e 'ssh cems-login ssh' <sourceDir> mms1:<targetDir>


## How to install the SST-CCI software?

1. Login to mms1 virtual machine
>
   ssh cems-login
   ssh mms1
   
2. Change your working directory
>
   cd /group_workspaces/cems2/esacci_sst/mms/github/sst-cci-toolbox
 
3. Build the software. Type 
>
   git pull
   mvn clean package install
   
4. Make assemblies
>
   mvn -f mms/pom.xml assembly:assembly
   mvn -f tools/pom.xml assembly:assembly
   
5. Move the assembly to the installation location. Type
>
   mv mms/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version} /group_workspaces/cems2/esacci_sst/mms/software
   
6. Change file modes. Type
>
   chmod -R ug+X /group_workspaces/cems2/esacci_sst/mms/software/sst-cci-mms-${project.version}


## How to update the SST-CCI software?

1. Login to mms1 virtual machine
>
   ssh cems-login
   ssh mms1
   
2. Execute the build and install script
>
   /group_workspaces/cems2/esacci_sst/mms/software/sst-cci-mms-${project.version}/bin/mymmsinstall


## How to produce a set of MMD files?

1. Login to lotus frontend
>
   ssh cems-login
   ssh lotus
   
2. Change your working directory
>
   cd /group_workspaces/cems2/esacci_sst/mms/inst
   
3. Create a new directory for your usecase and change to the new directory
>
   mkdir mms11a
   cd mms11a
   
4. Source the 'mymms' settings
>
    . ../../software/sst-cci-mms-2.0-SNAPSHOT/bin/mymms

5. Prepare or clean the directory structure 
>
   pmclean mms11a

6. Start the Python script for your usecase
>
   pmstartup mms11a.py

7. Inspect lotus job list 
>
   bjobs -w
               
8. Watch pmonitor status
>
   watch cat mms11a.status 

9. For inspecting job message files inspect the '*.out' files in the 'log' directory

10. For inspecting job log files inspect the '*.err' files in the 'log' directory

11. For inspecting output of shell scripts inspect the '*.out' files in the 'trace' directory

12. For stopping MMD production
>
    pmshutdown mms11a.py
    
13. For killing all jobs on CEMS lotus
>   
    bkill 0
    
14. For displaying all jobs running on the lotus front end
>
    ps aux | grep <username>
    
15. For killing all shell scripts running on the lotus front end
>
    killall /bin/bash

16. For clearing all tables in a database
>
    psql mms11b
    delete from mm_observation; delete from mm_datafile; delete from mm_sensor; delete from mm_variable; delete from mm_matchup; delete from mm_coincidence;
    


## Contact information

* Ralf Quast (ralf.quast@brockmann-consult.de)
