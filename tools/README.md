# ESA SST-CCI Tools


## Tools overview

The following tools are provided:

- Regional Averaging
- Regridding



### ESA CCI-SST Tools: Regional Averaging

usage: regavg [OPTIONS]

The regavg tool is used to generate regional average time-series from ARC (L2P, L3U) and SST_cci (L3U, L3P, L4) product
files given a time interval and a list of regions. An output NetCDF file will be written for each region.

OPTIONS may be one or more of the following:
    --ARC_L3U.dir <DIR>                Directory that hosts the products of type 'ARC_L3U'.
 -c,--configuration <FILE>             Reads a configuration (key-value pairs) from given FILE.
    --CCI_L2P.dir <DIR>                Directory that hosts the products of type 'CCI_L2P'.
    --CCI_L3C.dir <DIR>                Directory that hosts the products of type 'CCI_L3C'.
    --CCI_L3U.dir <DIR>                Directory that hosts the products of type 'CCI_L3U'.
    --CCI_L4.dir <DIR>                 Directory that hosts the products of type 'CCI_L4'.
    --climatologyDir <DIR>             The directory path to the reference climatology. The default value is
                                       './climatology'.
 -e,--errors                           Dumps a full error stack trace.
    --endDate <DATE>                   The end date for the analysis given in the format YYYY-MM-DD. The default value
                                       is '2020-12-31'.
    --filenameRegex <REGEX>            The input filename pattern. REGEX is Regular Expression that usually dependends
                                       on the parameter 'productType'. E.g. the default value for the product type
                                       'ARC_L3U' is 'AT[12S]_AVG_3PAARC\d{8}_[DTEM]_[nd][ND][23][bms][.]nc([.]gz)?'. For
                                       example, if you only want to include daily (D) L3 AATSR (ATS) files with night
                                       observations only, dual view, 3 channel retrieval, bayes cloud screening (nD3b)
                                       you could use the regex 'ATS_AVG_3PAARC\\d{8}_D_nD3b[.]nc[.]gz'.
 -h,--help                             Displays this help.
 -l,--logLevel <LEVEL>                 sets the logging level. Must be one of [OFF, ERROR, WARNING, INFO, ALL]. Use
                                       level 'ALL' to also output diagnostics. The default value is 'INFO'.
    --lut1File <FILE>                  A NetCDF file that provides lookup table 1. The default value is
                                       './configuration/auxdata/coverage_uncertainty_parameters.nc'.
    --lut2File <FILE>                  A plain text file that provides lookup table 2. The default value is
                                       './configuration/auxdata/RegionalAverage_LUT2.txt'.
    --outputDir <DIR>                  The output directory. The default value is '.'.
    --productType <NAME>               The product type. Must be one of [ARC_L3U, CCI_L2P, CCI_L3U, CCI_L3C, CCI_L4].
    --regionList <NAME=REGION[;...]>   A semicolon-separated list of NAME=REGION pairs. REGION may be given as
                                       coordinates in the format W,N,E,S or as name of a file that provides a region
                                       mask in plain text form. The region mask file contains 72 x 36 5-degree grid
                                       cells. Colums correspond to range -180 (first column) to +180 (last column)
                                       degrees longitude, while lines correspond to +90 (first line) to -90 (last line)
                                       degrees latitude. Cells can be '0' or '1', where a '1' indicates that the region
                                       represented by the cell will be considered in the averaging process. The default
                                       value is 'Global=-180,90,180,-90'.
    --sstDepth <DEPTH>                 The SST depth. Must be one of [skin, depth_20, depth_100]. The default value is
                                       'skin'.
    --startDate <DATE>                 The start date for the analysis given in the format YYYY-MM-DD. The default value
                                       is '1990-01-01'.
    --temporalRes <NUM>                The temporal resolution. Must be one of [daily, monthly, seasonal, annual]. The
                                       default value is 'monthly'.
 -v,--version                          Displays the version of this program and exits.
    --writeText                        Also writes results to a plain text file 'regavg-output-<date>.txt'.

All parameter options may also be read from a key-value-pair file. The tool will always try to read settings in the
default configuration file './regavg.properties'. Optionally, a configuration file may be provided using the -c <FILE>
option (see above).Command-line options overwrite the settings given by -c, which again overwrite settings in default
configuration file.


