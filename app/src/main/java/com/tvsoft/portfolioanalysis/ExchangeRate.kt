package com.tvsoft.portfolioanalysis

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "ExchangeRate"

data class ExchangeRate (
    val currency: CurrenciesDB,
    val date: LocalDate,
    val rate: Double
){}

private val currencyMap = mapOf(
    CurrenciesDB.USD to "R01235",
    CurrenciesDB.CHF to "R01775",
    CurrenciesDB.CNY to "R01375",
    CurrenciesDB.EUR to "R01239",
    CurrenciesDB.GBP to "R01035",
    CurrenciesDB.HKD to "R01200",
    CurrenciesDB.JPY to "R01820",
    CurrenciesDB.TRY to "R01700J"
)

class ExchangeRateAPI {
    fun getRate(currency: CurrenciesDB, from: LocalDate, to: LocalDate): List<ExchangeRate> {
        val list = mutableListOf<ExchangeRate>()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val urlString = "http://www.cbr.ru/scripts/XML_dynamic.asp?" +
                "date_req1=" + from.format(formatter) +
                "&date_req2=" + to.format(formatter) +
                "&VAL_NM_RQ=" + currencyMap[currency]
        val url = URL(urlString)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        try {
            conn.readTimeout = 5000
            conn.connectTimeout = 5000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            val stream = conn.inputStream

            val doc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
            val element: Element = doc.documentElement
            element.normalize()

            val nList: NodeList = doc.getElementsByTagName("Record")
            for(i in 0 until nList.length) {
                val node = nList.item(i)
                if(node.nodeType != Node.ELEMENT_NODE)
                    continue

                val elem = node as Element
                val mMap = mutableMapOf<String, String>()
                //for(j in 0 until elem.attributes.length)
                    mMap.putIfAbsent(elem.attributes.item(0).nodeName, elem.attributes.item(0).nodeValue)
                val date = LocalDate.parse(mMap["Date"], formatter2)
                val nominal: Int = elem.getElementsByTagName("Nominal").item(0).textContent.toInt()
                val value: Double = elem.getElementsByTagName("Value").item(0).textContent.
                    replace(',', '.').toDouble()
                val rate = ExchangeRate(currency, date, value / nominal)
                list.add(rate)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }

        return list
    }

/*    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val url = URL(urlString)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.readTimeout = 5000
        conn.connectTimeout = 5000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
        return conn.inputStream
    }*/

/*    fun getXmlFromUrl(url: String?): String? {
        var xml: String? = null
        try {
            // defaultHttpClient
            val httpClient = DefaultHttpClient()
            val httpPost = HttpPost(url)
            val httpResponse: HttpResponse = httpClient.execute(httpPost)
            val httpEntity: HttpEntity = httpResponse.getEntity()
            xml = EntityUtils.toString(httpEntity)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: ClientProtocolException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // return XML
        return xml
    }*/
}