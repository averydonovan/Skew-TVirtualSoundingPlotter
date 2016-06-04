#Skew-T MultiTool#

##About##

A program to generate Skew-T meteorological data plots from GRIB files output by 
meteorological forecasting models and other data sources.
The program uses the
[UCAR Unidata netCDF-Java](http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/)
library for GRIB file reading and JavaFX 8 for the GUI.

##Status##

Work on the basic functionality of the program is in progress.
Currently it is able to read in GRIB1 and GRIB2 files published on the
[NOAA NOMADS](http://nomads.ncdc.noaa.gov/data.php?name=access) site.
A preview release will be posted when it is able to generate a Skew-T plot.

##Planned Features##

* Generate Skew-T Log(P) diagrams from temperature and dew point data at various
  isobaric levels
* Convert relative humidity to dew point as needed
* Calculate values such as lifting condensation level (LCL), K-index,
  Total Totals, and SWEAT depending on data available
* Read GRIB1 and GRIB2 files output by the NOAA RAP, NAM, and GFS forecasting
  models
* Read and write comma-separated values (CSV) files

##Requirements##

* Java 8 SE or above
* Desktop operating system with windowing system (e.g. Microsoft Windows,
  Apple Mac OS X, Linux with X11)
