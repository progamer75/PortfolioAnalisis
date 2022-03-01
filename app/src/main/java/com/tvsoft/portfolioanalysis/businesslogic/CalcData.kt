package com.tvsoft.portfolioanalysis.businesslogic

import android.util.Log
import com.tvsoft.portfolioanalysis.CurrenciesDB
import com.tvsoft.portfolioanalysis.OperationTypesDB
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDao
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.min

private const val TAG = "CalcData"

data class StockBatch ( //партия акций
    val figi: String,
    val date: OffsetDateTime,
    val currency: CurrenciesDB,
    var sum: Long, // в копейках
    var quantity: Int
    )

data class Deal (
    val figi: String,
    val date: OffsetDateTime,
    val rate: Double,
    val profit: Long, // в копейках
    val tax: Double
    )

class CalcData(private val tinkoffDao: TinkoffDao,
               private val portfolioNum: Int) {
    private val tinkoffApi = TinkoffAPI.getInstance()
    private val stockBatchList = mutableListOf<StockBatch>()
    val dealList = mutableListOf<Deal>()

    suspend fun calcAllData() {
        stockBatchList.clear()
        dealList.clear()

        val buyList = tinkoffDao.getOperationsByType(portfolioNum, listOf(OperationTypesDB.Buy, OperationTypesDB.BuyCard))
        for(operation in buyList) {
            stockBatchList.add(StockBatch(
                figi = operation.figi,
                date = operation.date,
                currency = operation.currency,
                sum = -operation.payment,
                quantity = operation.quantity
            ))
        }

        val sellList = tinkoffDao.getOperationsByType(portfolioNum, listOf(OperationTypesDB.Sell))
        sellList.forEach { sell ->
            var quantityForClose = sell.quantity
            var profit: Long = 0
            var tax = 0.0
            var rate = 0.0
            while (quantityForClose > 0) {
                val stockBatch: StockBatch = stockBatchList.first { (it.figi == sell.figi) && (it.quantity > 0) }
                quantityForClose = min(sell.quantity, stockBatch.quantity)
                val buySum = stockBatch.sum * quantityForClose / stockBatch.quantity
                stockBatch.quantity -= quantityForClose
                profit += sell.payment - buySum

                val buyKurs = tinkoffDao.getRate(stockBatch.currency, stockBatch.date.toLocalDate()) ?: 1.0 // TODO если курс не найден надо подгрузить
                rate = tinkoffDao.getRate(sell.currency, sell.date.toLocalDate()) ?: 1.0
                tax += max((sell.payment * rate - buySum * buyKurs) * 0.13, 0.0)

                stockBatch.sum -= buySum

                quantityForClose = sell.quantity - quantityForClose

                Log.i(TAG, "${sell.figi} = ${sell.payment} / ${stockBatch.sum} / $profit / $tax --- $quantityForClose")

                if(stockBatch.quantity == 0)
                    stockBatchList.remove(stockBatch)
            }
            dealList.add(Deal(
                sell.figi,
                sell.date,
                rate,
                profit,
                tax
            ))
        }
    }
}