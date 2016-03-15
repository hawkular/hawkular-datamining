# Fits multiple time series models 

library(forecast)

fitModels <- function(timeSeries) {

  for (i in seq(timeSeries)) {
      
    series <- attr(timeSeries[[i]], 'ts')
    
    #get frequency (period)
    model <- 'ZZZ'
    frequency <- tsp(series)[3]
    if (frequency > 1) {
      model <- 'AAA'
    }
        
    model <- ets(series, opt.crit='mse', additive.only=TRUE, damped=FALSE, model=model)
    modelMetaData <- paste('model:', toString(model$method), '\n',
                           'MSE:', toString(model$mse), '\n',
                           'aic:', toString(model$aic), '\n',
                           'bic:', toString(model$bic), '\n',
                           'aicc:', toString(model$aicc), '\n',
                           'alpha:', toString(model$par['alpha']), '\n',
                           'beta:', toString(model$par['beta']), '\n',
                           'gamma:', toString(model$par['gamma']), '\n',
                           'l:', toString(model$par['l']), '\n',
                           'b:', toString(model$par['b']), '\n',
                           'periods:', toString(frequency(series)), '\n')
    
    attr(timeSeries[[i]], 'model') <- modelMetaData
  }

  timeSeries
}
