# ESA SST CCI Multi-sensor Matchup System (MMS)

## Overview

To be completed.

## Installation

### Installation of PostgreSQL and PostGIS

#### Linux

(taken from http://www.tokumine.com/2010/10/12/postgres-9-postgis-1-5-2-geos-3-2-2-and-gdal-1-7-on-ubuntu-10-04-lucid)

Ubuntu 10.04 / Lucid is the latest long term release that most of us will be using for our server deployments for now. Unfortunately, it was released just before the latest big releases in the FOSS GIS world: Postgres 9 and PostGIS 1.5.

Thankfully, it’s pretty simple to install these latest versions. Here is quick rundown of the steps needed to install a great OSS server-side GIS stack with all these new toys using easy to remove .deb packages. Tested on EC2 with the latest stock 10.04 server AMI (ami-60067832).

    * Postgresql 9.0.1 + hstore NoSQL columns
    * PostGIS 1.5.2
    * GEOS 3.2.2
    * Proj 4.7
    * GDAL 1.7.2
    * Spatialite 2.4 RC4

All components with test PostGIS database example (changed to mygisdb by Boe)

    sudo apt-get install python-software-properties

    sudo apt-add-repository ppa:pitti/postgresql
    sudo add-apt-repository ppa:ubuntugis/ubuntugis-unstable
    sudo apt-get update
    sudo apt-get install -y postgresql-9.0 postgresql-server-dev-9.0 postgresql-contrib-9.0 proj libgeos-3.2.2 libgeos-c1 libgeos-dev libgdal1-1.7.0 libgdal1-dev build-essential libxml2 libxml2-dev
    checkinstall

    wget http://postgis.refractions.net/download/postgis-1.5.2.tar.gz
    tar zxvf postgis-1.5.2.tar.gz && cd postgis-1.5.2/
    sudo ./configure && make && sudo checkinstall --pkgname postgis-1.5.2 --pkgversion 1.5.2-src --default

    sudo su postgres
    createdb -U postgres mygisdb
    createlang -dmygisdb plpgsql
    psql -d mygisdb -f /usr/pgsql-9.0/share/contrib/postgis-1.5/postgis.sql
    psql -d mygisdb -f /usr/pgsql-9.0/share/contrib/postgis-1.5/spatial_ref_sys.sql
    psql -U postgres -d mygisdb -c"select postgis_lib_version();"
    exit

#### Mac OS X

Install [Homebrew](http://mxcl.github.com/homebrew/). Then from the Terminal type:

    brew install postgresql 
    brew install postgis  
    mkdir -p /any/path/postgres 
    cd /any/path/postgres  
    initdb mydb 
    pg_ctl -D mydb -l logfile start
    createdb mydb   
    psql -d mydb -f /usr/local/share/postgis/postgis.sql  
    psql -d mydb -f /usr/local/share/postgis/spatial_ref_sys.sql

In order to automatically start your database on log-in, type

    cp /usr/local/Cellar/postgresql/9.0.4/org.postgresql.postgres.plist ~/Library/LaunchAgents
    launchctl load -w ~/Library/LaunchAgents/org.postgresql.postgres.plist

Then open `/Library/LaunchAgents/org.postgresql.postgres.plist` and in the entry following `-D` replace the existing path with the actual path to your database.

## Installation and database server startup on Eddie VM

mkdir ~/pgdata
/usr/pgsql-9.0/bin/initdb -D ~/pgdata/mms
mkdir ~/pgdata/mms/logs
/usr/pgsql-9.0/bin/pg_ctl -D /home/v1mbottc/pgdata/mms -l ~/pgdata/mms/logs/mms.log start
psql -d mms -f /usr/pgsql-9.0/share/contrib/postgis-1.5/postgis.sql

(failed with /usr/pgsql-9.0/lib/postgis-1.5.so: undefined symbol: GEOSHausdorffDistance; new version of GEOS has cured the problem.)

## How to copy data to CEMS?

Edit your .ssh/configuration file

    Host            cems-login
    HostName        comm-login1.cems.rl.ac.uk
    User            <your user name>
    ForwardX11      no
    ForwardAgent    yes

Then use e.g. rsync to copy directories of data

    rsync -av -e 'ssh cems-login ssh' <sourceDir> mms1:<targetDir>


## Usage

To be completed.

## Contact information

* Martin Böttcher (martin.boettcher@brockmann-consult.de)
* Ralf Quast (ralf.quast@brockmann-consult.de)
