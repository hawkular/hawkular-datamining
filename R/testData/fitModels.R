# Fits multiple time series models 

library(forecast)

fitModels <- function(timeSeries) {

  for (i in seq(timeSeries)) {
      
    series <- attr(timeSeries[[i]], 'ts')
        
    model <- ets(series, opt.crit='mse')
    modelMetaData <- data.frame(method=model$method, mse=model$mse, par=model$par)
    
    attr(timeSeries[[i]], 'model') <- modelMetaData
  }

  timeSeries
}
