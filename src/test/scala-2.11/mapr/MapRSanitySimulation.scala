package mapr

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.ojai.Document
import com.mapr.db._
import com.mapr.db.Table._

class MapRSanitySimulation extends Simulation {

  // declare constants
  // TODO: read from config file
  val _nodes = "mfs200.qa.lab,mfs210.qa.lab"
  val _cluster1 = "cluster1"
  val _cluster2 = "cluster2"
  val _tableBase = "table"
  val _MAPR_PATH = "/mapr/"
  var _id: String = ""
  val _url_prefix = "https://root:mapr@"
  val _url_postfix = ":8443/rest/table"
  val headers_10 = Map("Content-Type" -> "application/json")


  val httpConf = http.baseURLs(_buildBaseUrlList(_nodes))
    .acceptHeader("application/json,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  // stress several servers at the same time, to bypass a load-balancer
  def _buildBaseUrlList(nodes : String): List[String] = {
    var _urlList = List[String]()
    nodes.split(",").foreach(node => _urlList :+
      (_url_prefix + node + _url_postfix))
    println("_urlList" + _urlList)
    _urlList
  }

  // client id
  def _getUUID = java.util.UUID.randomUUID.toString

  def _buildTableName(clusterName: String, tableName: String) = {
    _MAPR_PATH + clusterName + "/" + tableName
  }

  // create table url
  def _createUrl(clusterName: String, tableName: String): String = {
    _url_prefix + _nodes.split(",").head + _url_postfix +
      "/create?path=" + _buildTableName(clusterName, tableName) + "&tabletype=json"
  }

  // replica autosetup url
  def _replicaAutosetupUrl(srcclusterName: String, srcTableName: String,
                           dstclusterName: String, dstTableName: String): String = {
    _url_prefix + _nodes.split(",").head + _url_postfix +  "/replica/autosetup?path=" +
      _buildTableName(srcclusterName, srcTableName) +
      "&replica=" + _buildTableName(dstclusterName, dstTableName)
  }

  def _loadData(clusterName: String, tableName: String) = {
    val document: Document = MapRDB.newDocument()
    document.set("_id", _getUUID)
    document.set("AAPL", 132.97)
    document.set("GOOG", 789.87)
    document.set("MSFT", 56.85)
    val table: Table =
      MapRDB.getTable(_buildTableName(clusterName, tableName))
        .setOption(TableOption.BUFFERWRITE, false)
    table.insertOrReplace(document)
  }

  val scn = scenario("FactSet Scenario")
    .exec {
      _id = _getUUID
      println("_id " + _id + " url: " + _createUrl(_cluster1, _tableBase + _id))
      http("request1 - create src table")
        .post(_createUrl(_cluster1, _tableBase + _id))
        .basicAuth("root","mapr")
        .headers(headers_10)
    }
    .pause(5)
    .exec {
    println("_id " + _id + " replica: " + _replicaAutosetupUrl(_cluster1, _tableBase + _id, _cluster2, _tableBase + _id))
    http("request2 - autosetup replica")
      .post(_replicaAutosetupUrl(_cluster1, _tableBase + _id, _cluster2, _tableBase + _id))
      .basicAuth("root","mapr")
      .headers(headers_10)
    }
    .pause(5)
    .exec { session => // load hack with expressions
      http("request3 - load data in src (replicates to dst)")
      _loadData(_cluster1, _tableBase + _id)
      session
    }

  setUp(scn.inject(atOnceUsers(20)).protocols(httpConf))
}
