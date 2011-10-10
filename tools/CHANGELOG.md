# ESA SST_cci Toolbox


## Changes from 0.1.0 to 0.1.1:

* Fixed NPE that occured for all ARC_L3U filetypes other than AT2. The ARC_L3U filetype now uses regexp to match filenames.

## Changes from 0.1.1 to 0.1.2:

* Introduced optional parameter 'filenameRegex'. It is used, e.g. to distinguish between the various ARC L3U input file types.