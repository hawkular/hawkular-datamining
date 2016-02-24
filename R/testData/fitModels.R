# Fits multiple time series models 

library(forecast)

fitModels <- function(timeSeries) {

  for (i in seq(timeSeries)) {
      
    series <- attr(timeSeries[[i]], 'ts', damped=FALSE)
        
    model <- ets(series, opt.crit='mse', additive.only=TRUE)
    modelMetaData <- paste('model:', toString(model$method), '\n',
                           'MSE:', toString(model$mse), '\n',
                           'aic:', toString(model$aic), '\n',
                           'bic:', toString(model$bic), '\n',
                           'aicc:', toString(model$aicc), '\n',
                           'alpha:', toString(model$par['alpha']), '\n',
                           'beta:', toString(model$par['beta']), '\n',
                           'l:', toString(model$par['l']), '\n',
                           'b:', toString(model$par['b']), '\n')
    
    attr(timeSeries[[i]], 'model') <- modelMetaData
  }

  timeSeries
}
