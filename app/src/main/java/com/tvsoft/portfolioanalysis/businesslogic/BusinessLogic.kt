package com.tvsoft.portfolioanalysis.businesslogic

import android.util.Log
import com.tvsoft.portfolioanalysis.*
import org.apache.commons.math3.analysis.DifferentiableUnivariateFunction
import org.apache.commons.math3.analysis.UnivariateFunction
import ru.tinkoff.piapi.contract.v1.OperationType
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.min
import kotlin.math.pow


private const val TAG = "BusinessLogic"

data class StockBatch ( //партия акций
    val figi: String,
    val date: OffsetDateTime,
    val currency: CurrenciesDB,
    val securityIn: Boolean, // перевод из другого депозитария
    var sum: Double,
    var quantity: Int
    )

data class Deal(
    val figi: String,
    val date: OffsetDateTime,
    val rate: Double,
    val profit: Double,
    val tax: Double
    )

data class CashFlow(
    val date: LocalDate,
    //val currency: CurrenciesDB,
    val cash: Double
)

data class PortfolioBalance(
    val date: LocalDate,
    val balance: MutableMap<CurrenciesDB, Double>
)

/*class IrrFun(private val firstDate: LocalDate,
             private val list: MutableList<CashFlow>): UnivariateDifferentiableFunction {

    override fun value(t: DerivativeStructure): DerivativeStructure {
        var res = 0.0
        for (cashFlow in list) {
            val d: Double = (cashFlow.date.toEpochDay() - firstDate.toEpochDay()).toDouble()
            val x = d * cashFlow.cash * t.value.pow(-d / 365.0) / (365 * t.value)
            res += x
            //Log.i(TAG, "${cashFlow.date} - $rate / $d / $x /$res")
        }

        return DerivativeStructure(1, 1, -res)
    }

    override fun value(rate: Double): Double {
        var res = 0.0
        for(cashFlow in list) {
            val d = cashFlow.date.toEpochDay() - firstDate.toEpochDay()
            val x = cashFlow.cash / rate.pow(d.toDouble() / 365.0)
            res += x
            //Log.i(TAG, "${cashFlow.date} - $rate / $d / $x /$res")
        }
        return res
    }
}*/

class IrrFun(private val firstDate: LocalDate,
             private val list: MutableList<CashFlow>): DifferentiableUnivariateFunction {

    override fun value(rate: Double): Double {
        var res = 0.0
        for(cashFlow in list) {
            val d = cashFlow.date.toEpochDay() - firstDate.toEpochDay()
            val x = cashFlow.cash / rate.pow(d.toDouble() / 365.0)
            res += x
            //Log.i(TAG, "${cashFlow.date} - $rate / $d / $x /$res")
        }
        return res
    }

    override fun derivative(): UnivariateFunction {
        val f = UnivariateFunction { rate: Double ->
            var res = 0.0
            for (cashFlow in list) {
                val d: Double = (cashFlow.date.toEpochDay() - firstDate.toEpochDay()).toDouble()
                val x = d * cashFlow.cash * rate.pow(-d / 365.0) / (365 * rate)
                res += x
            }
            res
        }
        return f
    }
}

class BusinessLogic(private val tinkoffDao: TinkoffDao,
                    private val portfolioNum: Int) {
    private val stockBatchList = mutableListOf<StockBatch>() // партии
    val dealList = mutableListOf<Deal>() // продажи
    private val rubCashList = mutableListOf<CashFlow>() // купля/продажа валюты
    private val usdCashList = mutableListOf<CashFlow>() // купля/продажа валюты
    private val portfolioBalanceList = mutableListOf<PortfolioBalance>() // баланс портфеля на дату

    var rubIrr = 0.0
    var usdIrr = 0.0

    suspend fun calcIrr(portfolioBalanceRub: Double, portfolioBalanceUsd: Double) {
        // рассчитаем баланс на каждый день, за одним заполним список покупок/продаж валюты
        rubCashList.clear()
        usdCashList.clear()
        portfolioBalanceList.clear()
        val curBalance = mutableMapOf<CurrenciesDB, Double>()
        enumValues<CurrenciesDB>().forEach {
            curBalance.set(it, 0.0)
        }
        val operList = tinkoffDao.getAllOperation(portfolioNum)
        var curDate = operList.first().date.toLocalDate()
        val firstDate = curDate
        for(oper in operList) {
            val operDate = oper.date.toLocalDate()
            if(operDate > curDate) {
                //Log.i(TAG,"$curDate - $curBalance")
                curDate = operDate
            }
            val prevBalance: Double = curBalance[oper.currency]!!
            val usdRate = ExchangeRateAPI.getRate(CurrenciesDB.USD, operDate)
            if(oper.operationType in arrayOf(OperationType.OPERATION_TYPE_BUY_CARD,
                    OperationType.OPERATION_TYPE_OUTPUT,
                    OperationType.OPERATION_TYPE_INPUT)) {
                var sum = oper.payment
                if(oper.operationType == OperationType.OPERATION_TYPE_BUY_CARD)
                    sum = -sum

                when(oper.currency) {
                    CurrenciesDB.USD -> {
                        usdCashList.add(CashFlow(operDate, sum))
                        rubCashList.add(CashFlow(operDate, sum * usdRate))
                    }
                    CurrenciesDB.RUB -> {
                        rubCashList.add(CashFlow(operDate, sum))
                        usdCashList.add(CashFlow(operDate, sum / usdRate))
                    }
                    else -> {
                        rubCashList.add(CashFlow(operDate,
                            Utils.moneyConvert(sum, oper.currency, CurrenciesDB.RUB, operDate)))
                        usdCashList.add(CashFlow(operDate,
                            Utils.moneyConvert(sum, oper.currency, CurrenciesDB.USD, operDate)))
                    }
                }
            }

/* не учитываем, т.к. сумма по операции = 0
               OperationTypesDB.SecurityIn -> {
                    Log.i(TAG, "In ${oper.currency.name}: ${oper.payment}")
                }
                OperationTypesDB.SecurityOut -> {
                    Log.i(TAG, "Out ${oper.currency.name}: ${oper.payment}")
                }
*/
            if(oper.instrumentType == InstrumentTypeDB.Currency &&
                (oper.operationType == OperationType.OPERATION_TYPE_SELL ||
                 oper.operationType == OperationType.OPERATION_TYPE_BUY))
            { // buy: oper.payment = -, sell +    payment в рублях
                val currency = getCurrenciesDByFigi(oper.figi)
                    ?: throw IllegalArgumentException("Не найдена валюта c figi ${oper.figi}")
                if(oper.operationType == OperationType.OPERATION_TYPE_SELL)
                    curBalance[currency] = curBalance[currency]!! - oper.quantity
                else
                    curBalance[currency] = curBalance[currency]!! + oper.quantity
                curBalance[CurrenciesDB.RUB] = curBalance[CurrenciesDB.RUB]!! + oper.payment
            } else
                curBalance[oper.currency] = prevBalance + oper.payment
        }

        rubCashList.add(CashFlow(LocalDate.now(), -portfolioBalanceRub))
        usdCashList.add(CashFlow(LocalDate.now(), -portfolioBalanceUsd))

        //val solver = org.apache.commons.math3.analysis.solvers.BrentSolver(1e-5)
        //val solver = org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver(1e-2)
/*        rubIrr = solver.solve(1000, IrrFun(firstDate, rubCashList),
            0.0, 100.0, 0.05) * 100.0 - 100.0*/
        val solver = org.apache.commons.math3.analysis.solvers.NewtonSolver(1e-3)
        rubIrr = solver.solve(100, IrrFun(firstDate, rubCashList),
            0.0, 100.0, 0.05) * 100.0 - 100.0
        Log.i(TAG, "$rubIrr")
        usdIrr = solver.solve(100, IrrFun(firstDate, usdCashList),
            0.0, 100.0, 0.05) * 100.0 - 100.0
        Log.i(TAG, "$usdIrr")

        /*rubIrr = solver.solve(100, IrrFun(firstDate, rubCashList),
            -100.0, 100.0, 0.1) * 100.0*/
/*        usdIrr = solver.solve(100, IrrFun(firstDate, usdCashList),
            -100.0, 100.0, 0.1) * 100.0*/
    }

    suspend fun calcDeals() {
        stockBatchList.clear()
        dealList.clear()

        val buyList = tinkoffDao.getOperationsByType(portfolioNum, listOf(OperationType.OPERATION_TYPE_BUY,
            OperationType.OPERATION_TYPE_BUY_CARD, OperationType.OPERATION_TYPE_INPUT_SECURITIES))
        for(operation in buyList) {
/*
            if(operation.operationType == OperationTypesDB.SecurityIn)
                continue
*/

            stockBatchList.add(StockBatch(
                figi = operation.figi,
                date = operation.date,
                currency = operation.currency,
                securityIn = (operation.operationType == OperationType.OPERATION_TYPE_INPUT_SECURITIES),
                sum = -operation.payment,
                quantity = operation.quantity
            ))

/*
            if(operation.operationType == OperationTypesDB.SecurityIn)
                Log.i(TAG, "${operation.payment} / ${operation.quantity} / ${operation.price}")
*/

        }

        val sellList = tinkoffDao.getOperationsByType(portfolioNum, listOf(OperationType.OPERATION_TYPE_SELL))
        sellList.forEach { sell ->
            var remainedForClose = sell.quantity
            var quantityForClose = sell.quantity
            var totalBuySum: Double = 0.0
            var tax = 0.0
            var rate = 0.0
            var quantitySecurityIn = 0
            while (remainedForClose > 0) {
                if(sell.figi.isEmpty())
                    Log.i(TAG, "${sell}")
                val stockBatch: StockBatch = stockBatchList.first { (it.figi == sell.figi) && (it.quantity > 0) }
                quantityForClose = min(remainedForClose, stockBatch.quantity)
                val buySum = stockBatch.sum * quantityForClose / stockBatch.quantity
                stockBatch.quantity -= quantityForClose
                if(!stockBatch.securityIn) {
                    totalBuySum += buySum

                    val buyKurs =
                        ExchangeRateAPI.getRate(stockBatch.currency, stockBatch.date.toLocalDate())
                    rate = ExchangeRateAPI.getRate(sell.currency, sell.date.toLocalDate())
                    tax += (sell.payment * quantityForClose / sell.quantity * rate - buySum * buyKurs) * 0.13
                    //max((sell.payment * quantityForClose / sell.quantity * rate - buySum * buyKurs) * 0.13, 0.0)
// TODO Здесь надо уточнить если часть партий закрывается в минус, а часть в плюс - должны ли минусовые компенсировать плюсовые
                    stockBatch.sum -= buySum
                } else {
                    quantitySecurityIn++
                    rate = 1.0
                }

                remainedForClose -= quantityForClose

/*
                if(sell.figi == "BBG00KHGQ0H4")
                    Log.i(TAG, "${sell.figi} = ${sell.payment} / ${stockBatch.quantity} - ${stockBatch.sum} / $totalBuySum / $tax --- $remainedForClose")
*/

                if(stockBatch.quantity == 0)
                    stockBatchList.remove(stockBatch)
            }

            val sellPrice = sell.payment / sell.quantity
            val profit = if(quantitySecurityIn == 0) { // если закрывается партия переведенная из другого депозитария, по ней прибыль не учитываем
                sell.payment - totalBuySum
            } else {
                //Log.i(TAG, "${sell.quantity} / $sellPrice")
                sellPrice * (sell.quantity - quantitySecurityIn)
            }

            if(tax < 0)
                tax = 0.0

/*
            if(sell.figi == "BBG00KHGQ0H4")
                Log.i(TAG, "${sell.figi} = ${sell.payment} / $profit / $tax")
*/
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