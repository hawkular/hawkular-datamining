# Functions used to generate time series data

LENGTH <- 200

# mean 0, variance sigma^2
whiteNoise <- function(l=LENGTH, mean=0, sigma=1) {
  set.seed(1)
  y <- rnorm(LENGTH, mean=mean, sd=sigma)
}

# trend is a function of the time
trendStationary <- function(y=whiteNoise(), intercept=0, slope=1) {
  time <- 1:length(y)
  y <- intercept + slope*time + y;
}

randomWalk <- function(l=LENGTH) {
  set.seed(1)
  x <- cumsum(sample(c(-1, 1), l, TRUE))
}