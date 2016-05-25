# Script tests execution time of several time series models 
#    implemented in R forecast package
# to run this script execute command from R console source(pathToThisFile.R)
# example: source('/home/pavol/projects/hawkular/hawkular-datamining/R/tests/performanceTests.R')
# setwd('/home/pavol/projects/hawkular/hawkular-datamining/R/tests')

library(forecast)
library(fpp)

# init directory
script.dir <- dirname(sys.frame(1)$ofile)
setwd(script.dir)
source('loadCsv.R')

# test data
x <- loadCsv('data/sineLowVarLong.csv')
x <- ts(x, frequency=20)

start.time <- Sys.time()
ets(x, model='ANN', opt.crit='mse', additive.only=TRUE, damped=FALSE)
end.time <- Sys.time()
cat('Simple exponential smoothing', end.time - start.time, '\n')

start.time <- Sys.time()
ets(x, model='AAN', opt.crit='mse', additive.only=TRUE, damped=FALSE)
end.time <- Sys.time()
cat('Double exponential smoothing', end.time - start.time, '\n')

start.time <- Sys.time()
ets(x, model='AAA', opt.crit='mse', additive.only=TRUE, damped=FALSE)
end.time <- Sys.time()
cat('Triple exponential smoothing', end.time - start.time, '\n')


