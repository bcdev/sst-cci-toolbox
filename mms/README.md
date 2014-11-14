# ESA SST-CCI Multi-sensor Matchup System (MMS) software

## Configuration instructions

### Configuring your environment on CEMS

1. Append the following snippet to your '.bash_profile'

        export JAVA_HOME='/usr/java/jdk1.7.0_51'
        export PATH=$HOME/bin:$JAVA_HOME/bin:$PATH
        export http_proxy=wwwcache.rl.ac.uk:8080
        export https_proxy=wwwcache.rl.ac.uk:8080

2. Include the following snippet in your '.m2/settings.xml' file and adapt username and pass phrases

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

## Installation instructions

### Prerequisites

The MMS software requires that the following software is installed on the target system:

* Java 7 Development Kit
* Python 2.7
* A recent version of Apache Maven
* A recent version of Git
* A recent version of PostgreSQL with PostGIS extension

#### Example: installing PostgreSQL and PostGIS on Mac OS

Install [Homebrew](http://mxcl.github.com/homebrew/). Then from the Terminal type:

    brew install postgresql 
    brew install postgis  
    mkdir -p /any/path/postgres 
    cd /any/path/postgres  
    initdb mmdb 
    pg_ctl -D mmdb -l mmdb.log start
    createdb mms   
    psql -d mms -c "CREATE EXTENSION postgis;"  
    psql -d mms -c "CREATE EXTENSION postgis_topology;"

In order to automatically start your database on log-in, type

    ln -sfv /usr/local/opt/postgresql/*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

Then open `~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist` and in the entry following `-D` replace the existing path with the actual path to `/any/path/postgres/mmdb`.

### Building the SST-CCI software from source

1. Clone the source code from github

        git clone git@github.com:bcdev/sst-cci-toolbox.git

2. Change your directory

        cd sst-cci-toolbox

3. Build the software. Type

        git pull
        mvn clean package install

4. Make an assembly

        mvn -f mms/pom.xml assembly:assembly

5. Move the assembly to the destination directory. Type

        mv mms/target/sst-cci-mms-${project.version}-bin/sst-cci-mms-${project.version} <DESTINATION>

6. Change file modes. Type

        chmod -R ug+X <DESTINATION>


## Operating instructions

### How to copy data to CEMS?

Edit your .ssh/configuration file

        Host            cems-login
        HostName        comm-login1.cems.rl.ac.uk
        User            <your user name>
        ForwardX11      no
        ForwardAgent    yes

Then use e.g. rsync to copy directories of data

        rsync -av -e 'ssh cems-login ssh' <sourceDir> mms1:<targetDir>


### How to produce a set of MMD files?

1. Login to lotus frontend

        ssh cems-login
        ssh lotus

2. Change your working directory

        cd /group_workspaces/cems2/esacci_sst/mms/inst

3. Create a new directory for your usecase and change to the new directory

        mkdir mms11a
        cd mms11a

4. Source the 'mymms' settings

        . ../../software/sst-cci-mms-${project.version}/bin/mymms

5. Prepare or clean the directory structure

        pmclean mms11a

6. Start the Python script for your usecase

        pmstartup mms11a.py

7. Inspect lotus job list

        bjobs -w

8. Watch pmonitor status

        watch cat mms11a.status

9. For inspecting job message files inspect the '*.out' files in the 'log' directory

10. For inspecting job log files inspect the '*.err' files in the 'log' directory

11. For inspecting output of shell scripts inspect the '*.out' files in the 'trace' directory

12. For stopping MMD production first

        pmshutdown mms11a.py

    Then kill all jobs on CEMS lotus

        bkill 0

    Then display all jobs running on the lotus front end

        ps aux | grep <your user name>

    Then kill all shell scripts running on the lotus front end

        killall /bin/bash

16. For clearing all tables in a database

        psql mms11b
        delete from mm_observation; delete from mm_datafile; delete from mm_sensor; delete from mm_variable; delete from mm_matchup; delete from mm_coincidence;

17. All work flows are configured so that the ingestion step has to be completed successfully before the subsequent steps can be executed

18. For work flows using in-situ data, the sampling step must not be run concurrently, due to a bug in the sampling tool.

19. Errors in the MMD steps that produce MMD files (sub-, nwp-, matchup-nwp, gbcs-, mmd-, selection-) cannot corrupt the database. It is safe to restart them.

20. Errors in the reingestion steps might corrupt the database. But usually it is safe to restart them.

21. Errors in the sampling step might corrupt the database, if and only if insitu data are used.

22. Errors in the clearsky step might corrupt the database. But usually it is safe to restart them.

23. Running several use cases in parallel is feasible, but doing so complicates things in step 12!

24. Errors like that below can be ignored. Don't know what happens there, but wait until the pmonitor job is completed and restart again to resolve the error.

        [rquast@lotus mms2]$ cat log/reingestion-1991-09-atsr.1-sub.err
        2014-09-04T09:32:17.795Z INFO: connecting to database jdbc:postgresql://130.246.142.93:5423/mms2
        29  matchupdb  INFO   [main] openjpa.Runtime - Starting OpenJPA 2.3.0
        99  matchupdb  INFO   [main] openjpa.jdbc.JDBC - Using dictionary class "org.apache.openjpa.jdbc.sql.PostgresDictionary".
        1177  matchupdb  INFO   [main] openjpa.jdbc.JDBC - Connected to PostgreSQL version 9.9 using JDBC driver PostgreSQL Native Driver version PostgreSQL 9.0 JDBC4 (build 801).
        2286  matchupdb  WARN   [main] openjpa.Runtime - Found no persistent property in "org.esa.cci.sst.data.InsituObservation"
        2014-09-04T09:32:20.610Z SEVERE: Can only perform operation while a transaction is active.
        org.esa.cci.sst.tools.ToolException: Can only perform operation while a transaction is active.
            at org.esa.cci.sst.tools.ingestion.MmdIngestionTool.main(MmdIngestionTool.java:54)
        Caused by: <openjpa-2.3.0-r422266:1540826 nonfatal user error> org.apache.openjpa.persistence.InvalidStateException: Can only perform operation while a transaction is active.
            at org.apache.openjpa.kernel.BrokerImpl.assertTransactionOperation(BrokerImpl.java:4735)
            at org.apache.openjpa.kernel.BrokerImpl.rollback(BrokerImpl.java:1545)
            at org.apache.openjpa.kernel.DelegatingBroker.rollback(DelegatingBroker.java:941)
            at org.apache.openjpa.persistence.EntityManagerImpl.rollback(EntityManagerImpl.java:599)
            at org.esa.cci.sst.orm.PersistenceManager.rollback(PersistenceManager.java:80)
            at org.esa.cci.sst.tools.ingestion.MmdIngestionTool.ingest(MmdIngestionTool.java:116)
            at org.esa.cci.sst.tools.ingestion.MmdIngestionTool.main(MmdIngestionTool.java:50)
        <openjpa-2.3.0-r422266:1540826 nonfatal user error> org.apache.openjpa.persistence.InvalidStateException: Can only perform operation while a transaction is active.
            at org.apache.openjpa.kernel.BrokerImpl.assertTransactionOperation(BrokerImpl.java:4735)
            at org.apache.openjpa.kernel.BrokerImpl.rollback(BrokerImpl.java:1545)
            at org.apache.openjpa.kernel.DelegatingBroker.rollback(DelegatingBroker.java:941)
            at org.apache.openjpa.persistence.EntityManagerImpl.rollback(EntityManagerImpl.java:599)
            at org.esa.cci.sst.orm.PersistenceManager.rollback(PersistenceManager.java:80)
            at org.esa.cci.sst.tools.ingestion.MmdIngestionTool.ingest(MmdIngestionTool.java:116)
            at org.esa.cci.sst.tools.ingestion.MmdIngestionTool.main(MmdIngestionTool.java:50)


## Copyright and licensing

This software is under copyright 2010-2014 by Brockmann Consult GmbH and distributed under the GNU General Public
License. Terms and conditions are described in the accompanying LICENSE.txt and NOTICE.txt files.


## Contact information

* Ralf Quast (ralf.quast@brockmann-consult.de)
* Tom Block (tom.block@brockmann-consult.de)
