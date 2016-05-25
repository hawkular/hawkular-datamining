# Functions used to generate time series data

LENGTH <- 200

# mean 0, variance sigma^2
whiteNoise <- function(l=LENGTH, mean=0, sigma=1) {
  set.seed(123)
  y <- rnorm(l, mean=mean, sd=sigma)
}

# trend is a function of the time
trendStationary <- function(y=whiteNoise(), intercept=0, slope=1) {
  time <- 1:length(y)
  y <- intercept + slope*time + y;
}

randomWalk <- function(l=LENGTH) {
  set.seed(1)
  y <- cumsum(sample(c(-1, 1), l, TRUE))
}

sine <- function(periods=LENGTH, seasons=1, amplitude=1, error=c('gaussian', 'uniform'), sigma=1) {

  errorType = match.arg(error)
  if (errorType == 'uniform') { 
    error <- runif(periods*seasons, min=(-sigma*0.5), max=(sigma*0.5))
  } else {
    error <- rnorm(periods*seasons, mean=0, sigma=1)
  }

  t <- seq(0, 2*pi,, periods*seasons)
  y <- amplitude*sin(seasons*t) + error
}

addRandomShocks <- function(x, number=10, timesStd=2) {
  std <- sd(x)
  indices <- round(runif(number, 1, length(x)), digits=0)
  
  for (i in indices) {
   
    x[i] = x[i]*timesStd * sample(c(TRUE,FALSE), 1, TRUE)
  }
  
  x
}