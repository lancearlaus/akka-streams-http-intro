# Intro to Akka Streams & HTTP Sample Code

Welcome to the sample code for my [Intro to Akka Streams & HTTP](http://slides.com/lancearlaus/akka-streams-http-intro) talk.

This full version of the accompanying code demonstrates additional concepts and structure. 
Two simple services are used to demonstrate the basics of developing a streaming application and exposing it as an HTTP service.
Comments and feedback welcome.

## Getting Started

```bash
git clone https://github.com/lancearlaus/akka-streams-http-intro.git
sbt run

[info] Running Main
Starting server on localhost:8080...STARTED
Get started with the following URLs:
 Stock Price Service:
   Yahoo with default SMA        : http://localhost:8080/stock/price/daily/yhoo
   Yahoo 2 years w/ SMA(200)     : http://localhost:8080/stock/price/daily/yhoo?period=2y&calculated=sma(200)
   Facebook 1 year raw history   : http://localhost:8080/stock/price/daily/fb?period=1y&raw=true
 Bitcoin Trades Service:
   Hourly OHLCV (Bitstamp USD)   : http://localhost:8080/bitcoin/price/hourly/bitstamp/USD
   Daily  OHLCV (itBit USD)      : http://localhost:8080/bitcoin/price/daily/itbit/USD
   Recent trades (itBit USD)     : http://localhost:8080/bitcoin/trades/itbit/USD
   Trades raw response           : http://localhost:8080/bitcoin/trades/bitstamp/USD?raw=true
```
