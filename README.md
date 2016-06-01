#Skew-T MultiTool#

##About##

A program to generate Skew-T meteorological data plots from GRIB files output by meteorological forecasting models
and other data sources.

##Status##

Work on the basic functionality of the program is in progress.
A preview release will be posted when it is able to at least read a GRIB file output by the NOAA RAP forecasting model
and generate a Skew-T.

##Planned Features##

* Generate Skew-T Log(P) diagrams from temperature and dewpoint data at various isobaric levels
* Convert relative humidity to dewpoint as needed
* Calculate values such as lifting condensation level (LCL), K-index, Total Totals, and SWEAT
  depending on data available
* Read GRIB files output by the NOAA RAP, NAM, and GFS forecasting models
* Read comma-separated values files
* Read data from and write data to an SQL database

##Requirements##

* Java 8 SE or above
* Desktop operating system with windowing system (e.g. Microsoft Windows, Apple Mac OS X, Linux with X11)
