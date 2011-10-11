# ESA SST_cci Toolbox

## Version 0.1.0 released on 07.10.2011:

## Version 0.1.1 released on 10.10.2011:

* Fixed NPE that occured for all ARC_L3U filetypes other than AT2. The ARC_L3U product type
  now uses regexp to match filenames.

## Version 0.1.2 released on 11.10.2011:

* Introduced optional parameter 'filenameRegex'. It is used, e.g. to distinguish between
  the various ARC L3U input file types. The default for ARC_L3U product type is
  "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz". If you only want
  daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval,
  bayes cloud screening (nD3b) you could use the regex "ATS_AVG_3PAARC\\d{8}_D_nD3b[.]nc[.]gz".
