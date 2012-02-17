# Changelog of the ESA SST_cci Toolbox

## Changes in version 1.1_b01 released on 17.02.2012:

* Fixed a bug in the aggregation of weighted, random uncertainties (Eq. 1.3 in the tools spec, 1.1 applies as well).
  With 's' being an uncertainty (sigma) vector and 'w' a weight vector with sum(w) != 1, the formula used was
    s_agg = sqrt(sum(w * s) / sum(w ^ 2))
  and has now been corrected to
    s_agg = sqrt(sum(w * s) / sum(w) ^ 2)
  The resulting value are now by magnitudes lower.
  See code is in org/esa/cci/sst/util/accumulators/RandomUncertaintyAccumulator.java.
* Changed the way, how coverage uncertainties are aggregated from 5 degree cells to 90 degree cells. After discussion
  with Nick and Chris in Noc 2011, 90 degree cells coverage uncertainties are now computed as follows:
  "The sampling/coverage uncertainties (Variable G) for 5 degree monthlies (calculated using LUT1) need to be
  aggregated to 90 degree monthlies assuming they are uncorrelated and weighting according to their relative
  proportions of ocean according to Equation 1.3. These aggregated uncertainties for each 90 degree monthly grid box
  are then added to the coverage uncertainties (Variable G) for 90 degree monthlies (calculated using LUT2), by
  squaring the two uncertainties, summing them and taking the square root."

## Changes in version 1.0_b03 released on 26.10.2011:

* Fixed a bug in aggregation from 5 degree cells to 90 degree cells: The weighting of each 5 degree cell by its ocean
  coverage has been taken from the corresponding 90 degree ocean coverage grid (instead of the 5 degree one).

## Changes in version 1.0_b02 released on 25.10.2011:

* Added GPL 3 license to distribution
* Changed logging time and time in text filename to local time

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

## Changes in version 0.1.3 released on 17.10.2011:

* Fixed a bug in aggregating ARC from 0.1 deg to 5-deg cells. Only the very first, non-NaN
  5-deg cell was kept and used for later averaging at a given x,y. Now, all 5-deg cell
  contributions are accumulated at given x,y.

## Changes in version 0.1.2 released on 11.10.2011:

* Introduced optional parameter 'filenameRegex'. It is used, e.g. to distinguish between
  the various ARC L3U input file types. The default for ARC_L3U product type is
  "AT[12S]_AVG_3PAARC\\d{8}_[DTEM]_[nd][ND][23][bms][.]nc[.]gz". If you only want
  daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval,
  bayes cloud screening (nD3b) you could use the regex "ATS_AVG_3PAARC\\d{8}_D_nD3b[.]nc[.]gz".

## Changes in version 0.1.1 released on 10.10.2011:

* Fixed NPE that occured for all ARC_L3U filetypes other than AT2. The ARC_L3U product type
  now uses regexp to match filenames.


## Initial Version 0.1.0 released on 07.10.2011.
