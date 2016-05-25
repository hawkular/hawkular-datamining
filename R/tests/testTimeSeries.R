# Script generates test time series data in csv format
# to run this script execute command from R console source(pathToThisFile.R)
# example: source('/home/pavol/projects/hawkular/hawkular-datamining/R/tests/testTimeSeries.R')
# setwd('/home/pavol/projects/hawkular/hawkular-datamining/R/tests')

# austourists
library(fpp) 

script.dir <- dirname(sys.frame(1)$ofile)
setwd(script.dir)
source('testTimeSeriesGenerators.R')
source('exporter.R')
source('fitModels.R')

LENGTH <- 350

# white noise low/high variance
wnLowVariance <- whiteNoise(LENGTH, mean=0, sigma=20)
wnHighVariance <- whiteNoise(LENGTH, mean=0, sigma=10000)

# trend stationary upward/downward, low/high variance
trendStatUpwardLowVar <- trendStationary(wnLowVariance, intercept=0, slope=3.337)
trendStatUpwardHighVar <- trendStationary(wnHighVariance, intercept=0, slope=60.667)
trendStatDownwardLowVar <- trendStationary(wnLowVariance, intercept=0, slope=-3.337)
trendStatDownwardHighVar <- trendStationary(wnHighVariance, intercept=0, slope=-60.667)

# seasonal
sineLowVarShort <- sine(periods=20, seasons=3, amplitude=18, sigma=4, error='uniform')
sineLowVarMedium <- sine(periods=20, seasons=5, amplitude=18, sigma=4, error='uniform')
sineLowVarLong <- sine(periods=20, seasons=20, amplitude=18, sigma=4, error='uniform')
# seasonal trend  
sineTrendLowVar <- trendStationary(sineLowVarLong, intercept=2.773, slope=0.73376)


# random walk
randomWalk <- randomWalk(1000)

# random shocks
#wnLowVariance <- addRandomShocks(wnLowVariance, 10, 2)
#trendStatUpwardLowVar <- addRandomShocks(trendStatUpwardLowVar, 10, 2)
#trendStatDownwardHighVar <- addRandomShocks(trendStatDownwardHighVar, 10, 2)
#sineTrendLowVar <- addRandomShocks(sineTrendLowVar, 10, 2)
#sineLowVarLong <- addRandomShocks(sineLowVarLong, 10, 2)

# time series export to CSV file
timeSeriesList <- list(addName(wnLowVariance), addName(wnHighVariance),
  addName(trendStatUpwardLowVar), addName(trendStatUpwardHighVar),
  addName(trendStatDownwardLowVar), addName(trendStatDownwardHighVar),
  addName(sineLowVarShort, frequency=20),
  addName(sineLowVarLong, frequency=20),
  addName(sineLowVarMedium, frequency=20),
  addName(sineTrendLowVar, frequency=20),
  addName(austourists, frequency=4),
  addName(randomWalk))

timeSeriesList <- fitModels(timeSeriesList)


# export to files
export(timeSeriesList)
