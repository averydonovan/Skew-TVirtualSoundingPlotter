#Skew-T Virtual Sounding Plotter#

##About##

A program to generate Skew-T meteorological data plots from GRIB files output by 
meteorological forecasting models, also known as a virtual sounding.
The program uses the
[UCAR Unidata netCDF-Java](http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/)
library for GRIB file reading and JavaFX 8 for the GUI.

##Status##

The program has implemented all features listed at a basic level.
Currently it is able to read in GRIB1 and GRIB2 files published on the
[NOAA NOMADS](http://nomads.ncdc.noaa.gov/data.php?name=access) site.
Further work on fixing bugs and adding features is ongoing.

##Features##

* Generate high-quality Skew-T/Log-P plots from temperature and dew point data
  at various isobaric levels
* Convert relative humidity to dew point as needed
* Calculate values such as lifting condensation level (LCL), K-index,
  Total Totals, and SWEAT depending on data available
* Read GRIB1 and GRIB2 files output by the NOAA RAP, NAM, and GFS forecasting
  models
* Output high-resolution (approximately 300 DPI) plot to a PNG file

##Requirements##

* Java 8 SE or above
* Desktop operating system with windowing system (e.g. Microsoft Windows,
  Apple Mac OS X, Linux with X11)
