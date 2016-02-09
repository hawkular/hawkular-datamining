# Script generates test time series data in csv format
# to run this script execute command from R console source(pathToThisFile.R)
# example: source('/home/pavol/projects/hawkular/hawkular-datamining/R/testData/testTimeSeries.R')

script.dir <- dirname(sys.frame(1)$ofile)
setwd(script.dir)
source('testTimeSeriesGenerators.R')
source('exporter.R')
source('fitModels.R')

LENGTH <- 250

# white noise low/high variance
wnLowVariance <- whiteNoise(LENGTH, mean=0, sigma=20)
wnHighVariance <- whiteNoise(LENGTH, mean=0, sigma=10000)

# trend stationary upward/downward, low/high variance
trendStatUpwardLowVar <- trendStationary(wnLowVariance, intercept=0, slope=3.337)
trendStatUpwardHighVar <- trendStationary(wnHighVariance, intercept=0, slope=60.667)
trendStatDownwardLowVar <- trendStationary(wnLowVariance, intercept=0, slope=-3.337)
trendStatDownwardHighVar <- trendStationary(wnHighVariance, intercept=0, slope=-60.667)

# random walk
rw <- randomWalk(LENGTH)

# time series export to CSV file
timeSeriesList <- list(addName(wnLowVariance), addName(wnHighVariance), addName(trendStatUpwardLowVar), addName(trendStatUpwardHighVar), addName(trendStatDownwardLowVar), addName(trendStatDownwardHighVar))

timeSeriesList <- fitModels(timeSeriesList)


# export to files
export(timeSeriesList)




