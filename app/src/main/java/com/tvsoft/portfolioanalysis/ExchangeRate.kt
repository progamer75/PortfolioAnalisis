package com.tvsoft.portfolioanalysis

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

object ExchangeRateAPI {
    val tinkoffDao: TinkoffDao = TinkoffDB.getDatabase(PortfolioAnalysisApplication.context!!).tinkoffDao

    fun loadRateFromRes(currency: CurrenciesDB, from: LocalDate, to: LocalDate): List<ExchangeRate> {
        val list = mutableListOf<ExchangeRate>()
        val res = PortfolioAnalysisApplication.context?.resources

        val resName = when(currency) {
            CurrenciesDB.USD -> R.raw.usd_r01235
            CurrenciesDB.EUR -> R.raw.eur_r01239
            else -> return list
        }

        //val inputStream = PortfolioAnalysisApplication.conext?.assets?.open(fileName)
        val inputStream = res?.openRawResource(resName)

        val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val doc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
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
            //Log.i(TAG, "${rate.date} / ${rate.rate}")
            list.add(rate)
        }

        return list
    }

    suspend fun loadRates(currency: CurrenciesDB, from: LocalDate, to: LocalDate): List<ExchangeRate> {
        val list = mutableListOf<ExchangeRate>()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val urlString = "http://www.cbr.ru/scripts/XML_dynamic.asp?" +
                "date_req1=" + from.format(formatter) +
                "&date_req2=" + to.format(formatter) +
                "&VAL_NM_RQ=" + currencyMap[currency]
        val url = URL(urlString)
        withContext(Dispatchers.IO) {
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            try {
                conn.readTimeout = 15000
                conn.connectTimeout = 15000
                conn.requestMethod = "GET"
                conn.doInput = true
                conn.connect()
                val stream = conn.inputStream

                val doc: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
                val element: Element = doc.documentElement
                element.normalize()

                val nList: NodeList = doc.getElementsByTagName("Record")
                for (i in 0 until nList.length) {
                    val node = nList.item(i)
                    if (node.nodeType != Node.ELEMENT_NODE)
                        continue

                    val elem = node as Element
                    val mMap = mutableMapOf<String, String>()
                    //for(j in 0 until elem.attributes.length)
                    mMap.putIfAbsent(
                        elem.attributes.item(0).nodeName,
                        elem.attributes.item(0).nodeValue
                    )
                    val date = LocalDate.parse(mMap["Date"], formatter2)
                    val nominal: Int =
                        elem.getElementsByTagName("Nominal").item(0).textContent.toInt()
                    val value: Double =
                        elem.getElementsByTagName("Value").item(0).textContent.replace(',', '.')
                            .toDouble()
                    val rate = ExchangeRate(currency, date, value / nominal)
                    list.add(rate)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn.disconnect()
            }
        }

        return list
    }

    suspend fun getLastRate(currency: CurrenciesDB): Double {
        var result: Double = 1.0
        if(currency == CurrenciesDB.RUB)
            return result

        val urlString = "http://www.cbr.ru/scripts/XML_daily.asp"
        val url = URL(urlString)
        withContext(Dispatchers.IO) {
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            try {
                conn.readTimeout = 5000
                conn.connectTimeout = 5000
                conn.requestMethod = "GET"
                conn.doInput = true
                conn.connect()
                val stream = conn.inputStream

                val doc: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
                val element: Element = doc.documentElement
                element.normalize()

                val nList: NodeList = doc.getElementsByTagName("Valute")
                for (i in 0 until nList.length) {
                    val node = nList.item(i)
                    if (node.nodeType != Node.ELEMENT_NODE)
                        continue

                    val elem = node as Element
                    val name: String = elem.getElementsByTagName("CharCode").item(0).textContent
                    if (name != currency.itName())
                        continue
                    val nominal: Int =
                        elem.getElementsByTagName("Nominal").item(0).textContent.toInt()
                    val value: Double =
                        elem.getElementsByTagName("Value").item(0).textContent.replace(',', '.')
                            .toDouble()
                    result = value / nominal

                    break
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn.disconnect()
            }
        }

        return result
    }

    suspend fun getRate(currency: CurrenciesDB, date: LocalDate): Double {
        val rate = if(currency == CurrenciesDB.RUB)
            1.0
        else
            tinkoffDao.getRate(currency, date) ?: loadRate(currency, date)

/*
        if(rate < 0.1)
            Log.i("TAG", "$date / $currency / $rate")
*/

        return rate
    }

    private suspend fun loadRate(currency: CurrenciesDB, date: LocalDate): Double {
        var rate = 1.0
        if(currency == CurrenciesDB.RUB)
            return 1.0

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val urlString = "http://www.cbr.ru/scripts/XML_daily.asp?" +
                "date_req=" + date.format(formatter)
        //"https://cbr-xml-daily.ru/scripts/XML_daily.asp"
        val url = URL(urlString)
        withContext(Dispatchers.IO) {
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            try {
                conn.readTimeout = 5000
                conn.connectTimeout = 5000
                conn.requestMethod = "GET"
                conn.doInput = true
                conn.connect()
                val stream = conn.inputStream

                val doc: Document =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
                val element: Element = doc.documentElement
                element.normalize()

                val nList: NodeList = doc.getElementsByTagName("Valute")
                for (i in 0 until nList.length) {
                    val node = nList.item(i)
                    if (node.nodeType != Node.ELEMENT_NODE)
                        continue

                    val elem = node as Element
                    val name: String = elem.getElementsByTagName("CharCode").item(0).textContent
                    val nominal: Int =
                        elem.getElementsByTagName("Nominal").item(0).textContent.toInt()
                    val value: Double =
                        elem.getElementsByTagName("Value").item(0).textContent.replace(',', '.')
                            .toDouble()
                    // rate возвратим для выбранной валюты, курсы остальных валют загрузим в базу
                    val curRate = value / nominal
                    if (name == currency.itName())
                        rate = curRate
                    val cur: CurrenciesDB? = currency.getWithNull(name)
                    if (cur != null) {
                        tinkoffDao.insertRate(ExchangeRateDB(cur, date, curRate))
                        Log.i(TAG, "$cur / $date / $curRate")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn.disconnect()
            }
        }

        return rate
    }
}

