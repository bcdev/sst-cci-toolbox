# ESA SST_cci Toolbox 

## Toolbox overview

The Toolbox services will be implemented basically a set of programs written in the Java
programming language. A number of the required functions are already implemented in
ESA BEAM libraries, which are used for the development of the tools. Users will be able 
to invoke the tools either via a posix
shell compatible command-line interface and/or plugged into the 
[BEAM](http://www.brockmann-consult.de/cms/web/beam/)/VISAT graphical
user interface. For all tools a technical description and user guide will be provided.

The Toolbox is composed of numerous services and features on top of the *Multi-sensor Match-up 
System (MMS)* and the full EO / reference data collections. It addresses the different 
needs of the EO Science and Climate teams during the various project tasks.

For Task 2 it provides features for:

* Generation of datasets used for SST algorithm development and improvement, validation and round-robin inter-comparison,
* Algorithm inter-comparison and selection.

For Task 3 it provides features for:

* Verification of the correct implementation of the prototype system against the algorithms,
* Generation of the CRDP and the verification of the data products contained herein.

For Task 4 it provides features for:

* ECV product inter-comparison and validation,
* Analysis of multi-annual trends in ECV products.

We propose to implement the majority of the Toolbox features using the ESA BEAM Development 
Platform. Many of the required functions and features are already available as executable 
tools in BEAM. 
Other features can be implemented straight forward using the various BEAM application 
programming interfaces (APIs). The desktop application provides advanced imaging,
processing and analysis out-of-the-box, permitting effective assessment of data products 
by the EO Science Team and Climate Research Group.

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
    psql -U postgres -d mygisdb -f /usr/share/postgresql/9.0/contrib/postgis-1.5/postgis.sql
    psql -U postgres -d mygisdb -f /usr/share/postgresql/9.0/contrib/postgis-1.5/spatial_ref_sys.sql
    psql -U postgres -d mygisdb -c"select postgis_lib_version();"
    exit

#### Mac OS X

Install [Homebrew](http://mxcl.github.com/homebrew/). Then from the Terminal type:

    brew install postgresql 
    brew install postgis  
    mkdir -p /any/path/postgres 
    cd /any/path/postgres  
    initdb mygisdb 
    createdb mygisdb  
    createlang -d mygisdb plpgsql  
    psql -d mygisdb -f /usr/local/share/postgis/postgis.sql  
    psql -d mygisdb -f /usr/local/share/postgis/spatial_ref_sys.sql
    psql -d mygisdb 
    # ALTER USER <user> RENAME TO mms ;    
    # \q

In order to automatically start the database on log-in, type

    cp /usr/local/Cellar/postgresql/9.0.2/org.postgresql.postgres.plist ~/Library/LaunchAgents
    launchctl load -w ~/Library/LaunchAgents/org.postgresql.postgres.plist

Then open `~/Library/LaunchAgents/org.postgresql.postgres.plist` and in the entry following `-D` replace the existing path with the actual path to the database.

## Usage

Coming soon.

## Contributors

* Martin Böttcher (martin_boettcher)
* Norman Formferra (forman)
* Ralf Quast (flar)
* Thomas Storm (thomasstorm)
* Marco Zuehlke (mzuehlke)
