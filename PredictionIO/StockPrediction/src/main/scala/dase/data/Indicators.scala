package dase.data

import java.time.LocalDate

import org.saddle.Series

/**
  * Base class for an indicator.
  *
  * All indicators should be defined as classes that extend
  * this base class. See RSIIndicator as an example. These indicators can then
  * be instantiated and passed into a StockStrategy class. Refer to tutorial 
  * for further explanation (found in the README.md file).
  */
@SerialVersionUID(100L)
sealed trait Indicator extends Serializable {
  /** Calculates training series for a particular stock.
    *
    * @param logPrice series of logarithm of all prices for a particular stock. Logarithm values are recommended for more accurate results.
    * @return the training series of the stock
    */
  def getTraining(logPrice: Series[LocalDate, Double]): Series[LocalDate, Double]

  /** Applies indicator on a window size of the value returned by
    * getMinWindowSize() and returns the last value in the resulting series to
    * be used for prediction in RegressionStrategy.
    *
    * @param input series of logarithm of all prices for a particular stock
    * @return the last value in the resulting series from the feature calculation
    */
  def getLast(input: Series[LocalDate, Double]): Double

  /** Returns window size to be used in getOne()
    *
    * @return the window size
    */
  def getMinWindowSize: Int
}

object Indicators {
  def from(indicatorNames: Seq[String]): Seq[Indicator] = indicatorNames.map {
    case "rsi" => new RSIIndicator()
    case "shifts" => new ShiftsIndicator(???)
  }
}

/** Indicator that implements a relative strength index formula
  *
  * Relative Strength Index
  *
  * RSI is a technical indicator used in the analysis of financial markets.
  * It is intended to chart the current and historical strength or weaknesses of a stock or market based on the closing prices of a recent trading period.
  *
  * RSI is classified as a momentum classifier, measuring the velocity and magnitude of directional price movements.
  * Momentum is the rate of the rise and fall in price.
  * The RSI computes momentum as the ratio of higher closes to lower closes: stocks which have had more or stronger positive changes have a higher RSI than stocks which have had more or stronger negative changes.
  *
  * The RSI is most typically used on a 14-day timeframe, measured on a scale from 0 to 100, with high and low levels marked at 70 and 30, respectively.
  *
  * RSI is a technical momentum indicator that compares the magnitude of recent gains to recent losses in an attempt to determine overbought and oversold conditions of an asset.
  *
  * @param rsiPeriod number of days to use for each of the 14 periods that are used in the RSI calculation
  */
class RSIIndicator(rsiPeriod: Int = 14) extends Indicator {
  import common.implicits.localDateOrdering

  def getMinWindowSize: Int = rsiPeriod + 1

  // Computes RSI of price data over the defined training window time frame
  def getTraining(logPrice: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    val rsiSeries = rsi(logPrice)

    // Fill in first 14 days offset (due to `rolling`) with 50 to maintain results
    rsiSeries.reindex(logPrice.rowIx).fillNA(_  => 50.0)
  }

    // Computes the RSI for the most recent time frame, returns single double
  def getLast(logPrice: Series[LocalDate, Double]): Double =
    getTraining(logPrice).last

  private def rsi(logPriceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
    /**
      * RS = SMMA(U, n) / SMMA(D, n)
      *
      * where RS is Relative Strength,
      * SMMA is Modified Moving Average which is exponentially smoothed Moving Average with a = 1 / period,
      * U is Upward change where up periods are characterized by the close being higher than the previous close and
      * D is Download change down periods are characterized by the close being lower than the previous period's close.
      */
    def rs(priceSeries: Series[LocalDate, Double]): Series[LocalDate, Double] = {
      val dailyReturnSeries = (priceSeries - priceSeries.shift(1)).fillNA(_ => 0.0)

      val upSeries = dailyReturnSeries.mapValues(dailyReturn => if (dailyReturn > 0) dailyReturn else 0)
      val downSeries = dailyReturnSeries.mapValues(dailyReturn => if (dailyReturn < 0) dailyReturn else 0)

      val upMovingAverageSeries = upSeries.rolling(rsiPeriod, _.mean)
      val downMovingAverageSeries = downSeries.rolling(rsiPeriod, _.mean)

      upMovingAverageSeries / downMovingAverageSeries
    }

    val rsSeries = rs(logPriceSeries)

    rsSeries.mapValues(rs => 100 - (100 / (1 + rs)))
  }
}

/** Indicator that calculate differences of closing prices
  *
  * @constructor create an instance of a ShiftsIndicator
  * @param period number of days between any 2 closing prices to consider for
  *          calculating a return
  */
class ShiftsIndicator(period: Int) extends Indicator {
  import common.implicits.localDateOrdering

  private def getRet(logPrice: Series[LocalDate, Double], frame: Int = period) =
   (logPrice - logPrice.shift(frame)).fillNA(_ => 0.0)

  def getMinWindowSize: Int = period + 1

  def getTraining(logPrice: Series[LocalDate, Double]): Series[LocalDate, Double] =
    getRet(logPrice)

  def getLast(logPrice: Series[LocalDate, Double]): Double =
    getRet(logPrice).last
}
