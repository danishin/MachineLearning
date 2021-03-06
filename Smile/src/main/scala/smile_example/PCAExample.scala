package smile_example

import java.awt.{Color, Dimension}
import java.time.LocalDate

import smile.plot.{Line, LinePlot}
import smile.projection.PCA

import scala.swing.{MainFrame, SimpleSwingApplication}

/*
* Principal Component Analysis - PCA
*
* Principal Component Analysis is a technique used in statistics to convert a set of correlated columns into a smaller set of uncorrelated columns, reducing the amount of features of a problem.
* This smaller set of columns are called the principle components.
* This technique is mostly used in exploratory data analysis as it reveals internal structure in the data that can not be found with eye-balling the data
*
* A big weakness of PCA however, are outliers in the data.
* These heavily influences its result, thus looking at the data on beforehand, eliminating large outliers can greatly improve its performance.
*
* PCA is used when it is clear that, for example, from the 2 dimensions (X and Y), you could reduce to 1 dimension and still classify properly.
* With PCA, a new value is calculated for each datapoint, based on its original dimensions.
* Note that the values for X generated by PCA do not correspond to the original X and this shows that PCA does not 'just drop' a dimension
* */

/*
* Using unsupervised learning to merge features - PCA (Principal Component Analysis)
*
* The basic idea of PCA is to reduce the amount of dimensions of a problem.
* This can be useful for getting rid of the curse of dimensionality or to merge data such that you can see trends within the data without noise of correlated data
*
* In this example, we are going to use PCA to merge stock prices from 24 stocks into 1 over a time period of 2002 - 2012.
* This single value (over time) then represents a stock market index based on data of these 24 stocks.
* Merging these 24 stock prices into 1 significantly reduces the amount of data to process, and decreases the dimension of our data, which is a big advantage if we later apply other machine learning algorithms such as regression for prediction.
* In order to see the performance of our feature reduction of 24 to 1, we will compare our result to the Dow Jones Index over the same period
* */
object PCAExample extends SimpleSwingApplication with util.Helper {
  case class StockData(date: LocalDate, stock: String, close: Double)

  def top = new MainFrame {
    title = "PCA Example"

    val (dates, dataPoints) = getTrainingData

    val pca = new PCA(dataPoints)
    pca.setProjection(1)

    val plotData1 = {
      val points = pca.project(dataPoints)
      // Normalize PCA'ed values
      val rangeValue = points.maxBy(_.head).head - points.minBy(_.head).head
      points
        .zipWithIndex
        .map { case (dataPoint, index) => Array(index.toDouble, -dataPoint.head / rangeValue) }
    }

    val plotData2 = getTestingData._2

    val plot = LinePlot.plot("Merged Features Index", plotData1, Line.Style.DASH, Color.red)
    plot.line("Dow Jones Index", plotData2, Line.Style.DOT_DASH, Color.blue)

    peer.setContentPane(plot)
    size = new Dimension(400, 400)

    /*
    * We see now that even though the data of the DJI ranges between 0.8 and 1.8 whereas our new feature ranges between -0.5 and 0.5, the trend lines correspond quite well.
    * */
  }

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
  /*
  * With this training data, and the fact that we already know that we want to merge the 24 features into 1 single feature,
  * we can do teh PCA and retrieve the values for the datapoints
  * */
  def getTrainingData: (Array[LocalDate], Array[Array[Double]]) =
    files.readCSV("resources/PCA/PCA_Example_1.csv")
      .drop(1)
      .map { case List(date, stock, close) => StockData(LocalDate.parse(date), stock, close.toDouble) }
      .toArray
      .groupBy(_.date)
      .mapValues(_.sortBy(_.stock).map(_.close))
      .toArray
      .sortBy(_._1)
      .unzip

  /*
  * As you can see, the ranges of the DJI and our computed feature are far off.
  * This is why we will now normalize the data.
  * The idea is that we scale the data based on its range, such that both datasets are on the same scale
  * */
  def getTestingData: (Array[LocalDate], Array[Double]) = {
    val (dates, closes) = files.readCSV("resources/PCA/PCA_Example_2.csv")
      .drop(1)
      .map { case List(date, open, high, low, close, volume, adj_close) => (LocalDate.parse(date), close.toDouble) }
      .sortBy(_._1)
      .toArray
      .unzip

    val rangeValue = closes.max - closes.min
    // normalize close values by the range of values
    (dates, closes.map(_ / rangeValue))
  }
}
