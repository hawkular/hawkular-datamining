# Script for evaluation part of the master's thesis
#
# to run this script execute command from R console source(pathToThisFile.R)
# example: source('/home/pavol/projects/hawkular/hawkular-datamining/R/tests/evaluation.R')
# setwd('/home/pavol/projects/hawkular/hawkular-datamining/R/tests')

library(forecast)
library(fpp)

# init directory
script.dir <- dirname(sys.frame(1)$ofile)
setwd(script.dir)
source('loadCsv.R')
source('testTimeSeriesGenerators.R')


predictionError <- function(prediction, x, forecastingHorizon=1) {
  predicted <- as.numeric(prediction$mean)
  trainLength <- length(prediction$x)
  
  if (length(x) < length(predicted) + trainLength) {
    stop(cat('x.length=',length(x), ', predicted.length=',length(predicted), ', trainLength=', trainLength))
  }
  
  original <- x[(trainLength+1):(trainLength+length(predicted) + 1)]
  
  error <- original[forecastingHorizon]-predicted[forecastingHorizon]
  
  #cat(error, '\n')
  #cat('Oiginal y =', original[forecastingHorizon], '\n')
  sse <- error^2
  absSum <- abs(error)
  
  data.frame('sse'= sse, 'absSum'= absSum)   
}

testPredictions <- function(timeSeries, modelType='ANN', trainSize, numberOfPredictions, forecastingHorizon=1) {
  if (trainSize + numberOfPredictions > length(timeSeries$ts) + forecastingHorizon) {
    stop()
  }

  freq <- frequency(timeSeries$ts)
  trainTs <- ts(timeSeries$ts[1:trainSize], frequency=freq)
  print(length(trainTs))
  testModel <- ets(trainTs, opt.crit='mse', additive.only=TRUE, damped=FALSE, model=modelType)
  
  sse <- 0
  absSum <- 0  
  count <- 0;
  
  for (i in trainSize:(trainSize + numberOfPredictions - 1)) {
    count <- count + 1;
    
    trainTs <- ts(timeSeries$ts[1:i], frequency=freq)
    model <- ets(trainTs, model=testModel)
    f <- forecast(model, h=forecastingHorizon)  
  
    error <- predictionError(f, timeSeries$ts, forecastingHorizon)
    sse <- sse + error$sse
    absSum <- absSum + error$absSum
  }

  mse <- sse/numberOfPredictions
  mae <- absSum/numberOfPredictions
  
  cat("\n\n\n")
  print(timeSeries$name[1])
  #summary(testModel)
  print(paste('Model:', modelType, ', trainSize=', trainSize, ', end=', trainSize + numberOfPredictions))
  print(paste('Total number of predictions=', count))
  print(paste('Horizon=', forecastingHorizon, 'steps'))
  print(paste('MSE=', mse, ', MAE=', mae))
  
  data.frame('mse' = mse, 'mae' = mae)
}

performTest <- function(dataList, model='ANN') {

  stringForLatex = ''
  for (x in dataList) {
    result <- testPredictions(x, model, 200, 12, forecastingHorizon=12)
    
    stringForLatex <- paste(stringForLatex, format(round(result$mse, 2), nsmall = 2), '&')     
  }
  
  print(stringForLatex)
}

wnLowVar <- data.frame(ts=ts(loadCsv('evaluationData/wnLowVariance.csv')), name='wnLowVariance.csv')
trendStatUpwardLowVar <- data.frame(ts=ts(loadCsv('evaluationData/trendStatUpwardLowVar.csv')), name='trendStatUpwardLowVar.csv')
trendStatDownwardHigh <- data.frame(ts=ts(loadCsv('evaluationData/trendStatDownwardHighVar.csv')), name='trendStatDownwardHighVar.csv')
sineLowVarLong <- data.frame(ts=ts(loadCsv('evaluationData/sineLowVarLong.csv'), frequency=20), name='sineLowVarLong.csv')
sineTrendLowVar <- data.frame(ts=ts(loadCsv('evaluationData/sineTrendLowVar.csv'), frequency=20), name='sineTrendLowVar.csv')

#wnLowVar <- addRandomShocks(wnLowVar, 10);
#trendStatUpwardLowVar <- addRandomShocks(trendStatUpwardLowVar, 10)
#trendStatDownwardHigh <- addRandomShocks(trendStatDownwardHigh, 10)
#sineLowVarLong <- addRandomShocks(sineLowVarLong, 10)
#sineTrendLowVar<- addRandomShocks(sineTrendLowVar, 10)

seasonalData = list(sineLowVarLong,
  sineTrendLowVar);
data = list(wnLowVar,
  trendStatUpwardLowVar, trendStatDownwardHigh, 
  sineLowVarLong, sineTrendLowVar)

#simple 
#performTest(data, model='ANN')

# double
#performTest(data, model='AAN')

# triple
#performTest(seasonalData, model='AAA')

# plot
par(mfrow=c(5, 1))
plot(wnLowVar$ts, main="Constant White Noise", type='l', ylab='')
plot(trendStatUpwardLowVar$ts, main="Upward Trend", type='l', ylab='')
plot(trendStatDownwardHigh$ts, main="Downward Trend", type='l', ylab='')
plot(as.numeric(sineLowVarLong$ts)[0:350], main="Constant Sine", type='l', ylab='', xlab='Time')
plot(as.numeric(sineTrendLowVar$ts)[0:350], main="Trend Sine", type='l', ylab='',xlab='Time')

