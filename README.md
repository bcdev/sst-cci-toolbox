

ESA CCI-SST Toolbox 
===================

Toolbox overview
----------------

The Toolbox services will be implemented basically a set of programs written in the Java
programming language. A number of the required functions are already implemented in
ESA BEAM libraries, which are used for the development of the tools. Users will be able 
to invoke the tools either via a posix
shell compatible command-line interface and/or plugged into the 
[BEAM](http://www.brockmann-consult.de/cms/web/beam/)/VISAT graphical
user interface. For all tools a technical description and user guide will be provided.

The Toolbox is composed of numerous services and features on top of the MMDB and
the full EO / reference data collections. It addresses the different needs of the EO
Science and Climate teams during the various project tasks.

For Task 2 it provides features for:

* Generation of datasets used for SST algorithm development and improvement, alidation and round-robin inter-comparison,
* Algorithm inter-comparison and selection.

For Task 3 it provides features for:

* Verification of the correct implementation of the prototype system against the algorithms,
* Generation of the CRDP and the verification of the data products contained herein.

For Task 4 it provides features for:

* ECV product inter-comparison and validation,
* Analysis of multi-annual trends in ECV products.

We propose to implement the majority of the Toolbox features using the ESA. Many of the 
required functions and features are already available as executable tools in BEAM. 
Other features can be implemented straight forward using the various BEAM application 
programming interfaces (APIs). The desktop application provides advanced imaging,
processing and analysis out-of-the-box, permitting effective assessment of data products 
by the EO Science Team and Climate Research Group.

### Toolbox MMDB features

The MMDB is composed of the MD and MMD files and a relational database, which is
used for efficient match-up queries. The relational data model used is relatively simple; it
is basically a searchable index to the thousands of MD and MMD files. The database will
also ensure that the MMDB remains assessable, verifiable, scalable, extendable and
maintainable.
For the features operating on the MMDB, a dedicated will be developed, with
the aim to decouple lower-level database access from higher level Toolbox code. Besides
Toolbox code (Java), the MMDB API may also be callable from C or IDL programs. Table
2 characterises the most important Toolbox features operating on the MMDB. More
features may be added as a result of the consultation for the URD and PSD, which may
further impose requirements on-the-fly during Tasks 2 to 4. We will make sure that these
needs will be addressed in a timely manner by iterating each required feature in terms of
user interfaces and capabilities.

#### Toolbox EO data features

A number of features and functions are generic with respect to the origin and format of
the EO data. These will be either developed as add-on modules to BEAM or as standalone,
command-line tools. BEAM exhibits a module-based architecture and its API
allows developing add-on modules for several predefined extension points. Due to its
generic data model and public API, BEAM supports a wide range of file formats used in
optical remote sensing, including the generic GeoTIFF NetCDF/CF formats. It
supports also a wide range of special data formats such as MERIS, ATSR, ASAR, NOAA
and METOP AVHRR, MODIS, AVNIR, PRISM and CHRIS.


Installation
------------

Coming soon.

Usage
-----

Coming soon.

Contributors
------------

* Marco Zuehlke (mzuehlke)
* Ralf Quast (flar)
* Norman Formferra (forman)