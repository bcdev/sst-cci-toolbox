# ESA SST_cci Toolbox

## Initial Version 0.1.0 released on 07.10.2011.

## Changes in version 0.1.1 released on 10.10.2011:

* Fixed NPE that occured for all ARC_L3U filetypes other than AT2. The ARC_L3U product type
  now uses regexp to match filenames.

## Changes in version 0.1.2 released on 11.10.2011:

* Introduced optional parameter 'filenameRegex'. It is used, e.g. to distinguish between
  the various ARC L3U input file types. The default for ARC_L3U product type is
  "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz". If you only want
  daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval,
  bayes cloud screening (nD3b) you could use the regex "ATS_AVG_3PAARC\\d{8}_D_nD3b[.]nc[.]gz".

## Changes in version 0.1.3 released on 17.10.2011:

* Fixed a bug in aggregating ARC from 0.1 deg to 5-deg cells. Only the very first, non-NaN
  5-deg cell was kept and used for later averaging at a given x,y. Now, all 5-deg cell
  contributions are accumulated at given x,y.

## Changes in version 1.0_b01 released on 24.10.2011:

* SST averages and SST anomaly averages are now always included in the output.
  Thus, parameter 'outputType' has been removed.
* Included aggregated ARC uncertainty (variable A) and coverage uncertainty (variable G).
  Thus, two new parameters 'lut1File' and 'lut2File' have been added (see usage help).
* Slightly changed the file format (now v1.1) for the ARC_L3U input file type:
** SST variable name:         "sst_<sstDepth>"
** SST anomaly variable name: "sst_<sstDepth>_anomaly"
** ARC uncertainty:           "arc_uncertainty"
** Coverage uncertainty:      "coverage_uncertainty"
* Added parameter 'writeText' that allows to also output a file that contains all the
  generated results in plain text format.

## Changes in version 1.0_b02 released on 25.10.2011:

* Added GPL 3 license to distribution

