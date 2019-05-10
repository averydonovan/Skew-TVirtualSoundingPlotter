# Skew-T Virtual Sounding Plotter

## About

A program to generate Skew-T meteorological data plots from GRIB files output by 
meteorological forecasting models, also known as a virtual sounding.
It is intended for use as an easy-to-use educational tool by students or
researchers.

The program uses the
[UCAR/Unidata NetCDF-Java](http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/)
library for GRIB file reading.
Currently it is able to read GRIB1 and GRIB2 files published on the
[NOAA NOMADS](http://nomads.ncdc.noaa.gov/data.php?name=access) site.

Licensed under the 2-clause BSD license.

## Features

* Read GRIB1 and GRIB2 files output by the NOAA RAP, NAM, and GFS forecasting
  models
* Generate high-quality Skew-T/Log-P plots from temperature and dew point data
  at various isobaric levels
* Convert relative humidity to dew point as needed
* Calculate values such as lifting condensation level (LCL), K-index,
  Total Totals, and SWEAT depending on data available
* Output high-resolution (approximately 328 DPI) plot to a PNG file

## Requirements

### Important

It is recommended to use the latest 0.3.0 alpha version.
Despite the "alpha" label, these versions now bundle the necessary Java runtime and they
contain bug fixes for reading some model output files, as well as other fixes and enhancements.

### Running

* For Windows: double-click on "SkewTVSP.cmd" inside installation folder
* For Unix-like systems: run `SkewTVSP.sh` inside installation folder

### Building

* [OpenJDK 11](https://adoptopenjdk.net/)
* [OpenJFX 11 SDK](https://gluonhq.com/products/javafx/)
* [Apache Maven 3](https://maven.apache.org/)
* [7-Zip](https://www.7-zip.org/) (only on Windows, optional)

Maven should pull in all required dependencies automatically.
Simply run `mvn clean compile package` to build and `mvn exec:java` to run.

To create a distribution, run `.\create-dist` on Windows or `./create-dist.sh` on Unix-like
systems.
To create an archive file instead of a folder, run `.\create-dist archive` or
`./create-dist.sh archive`.

## Citing/Acknowledging

Citing or acknowledging Skew-T Virtual Sounding Plotter in works that use plots
generated by it is *not* required; however, it is much appreciated.

BibTeX example:

    @software{skewtvsp,
        author = {{Donovan Smith}},
        title = {Skew-T Virtual Sounding Plotter},
        url = {https://github.com/donovan1983/Skew-TVirtualSoundingPlotter},
        version = {0.2.0},
        year = {2017},
    }

Text example:

    Donovan Smith, (2017): Skew-T Virtual Sounding Plotter (SkewTVSP) version 0.2.0 [software].
    (https://github.com/donovan1983/Skew-TVirtualSoundingPlotter)
