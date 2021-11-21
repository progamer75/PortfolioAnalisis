package com.tvsoft.portfolioanalisis

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.lang.IllegalArgumentException
import java.util.*

@Entity
enum class Currencies(a: Int) {
    USD(0), RUB(1), EUR(2);

    fun get(s: String): Currencies {
        when(s) {
            "USD" -> return USD
            "RUB" -> return RUB
            "EUR" -> return EUR
        }

        throw IllegalArgumentException("нет валюты: $s")
    }
    
    val values = mapOf(
        "USD" to 0,
        "RUB" to 1,
        "EUR" to 2)
}

enum class Operation_types(a: Int) {
    Sell(0), // +
    Buy(1), // -
    BrokerCommission(2),
    ServiceCommission(3),
    PayOut(4),  // - вывод cash
    PayIn(5),  // + ввод
    Dividend(6),
    TaxDividend(7),
    BuyCard(8), // дублируется PayIn
    Coupon(9),
    TaxCoupon(10),
    Tax(11),
    PartRepayment(12),
    Repayment(13),
    SecurityIn(14);

    fun get(s: String): Operation_types {
        when(s) {
            "Sell" -> return Sell
            "Buy" -> return Buy
            "BrokerComission" -> return BrokerCommission
            "ServiceCommission" -> return  ServiceCommission
            "PayOut" -> return PayOut
            "PayIn" -> return PayIn
            "Dividend" -> return Dividend
            "TaxDividend" -> return TaxDividend
            "BuyCard" -> return BuyCard
            "Coupon" -> return Coupon
            "TaxCoupon" -> return TaxCoupon
            "Tax" -> return Tax
            "PartRepayment" -> return PartRepayment
            "Repayment" -> return Repayment
            "SecurityIn" -> return SecurityIn
        }

        throw IllegalArgumentException("нет операции: $s")
    }
}

@Entity
data class MarketInstrument(
    @PrimaryKey val figi: String,
    val currency: Int,
    val name: String,
    val ticker: String,
    val instrument_type: Int
)

@Dao
interface MarketInstrumentDao {
    @Query("Select * from MarketInstrument")
    fun getAll(): List<MarketInstrument>

    @Insert
    fun insert(mi: MarketInstrument)
}

@Entity(foreignKeys = [ForeignKey(
    entity = MarketInstrument :: class,
    parentColumns = ["figi"],
    childColumns = ["figi"])])
data class Operation(
    @PrimaryKey val id: String,
    val figi: String,
    val date: Date,
    val currency: Int,
    val operation_type: Int,
    val payment: Double,
    val price: Double,
    val quantity: Int,
    val commission: Double,
    val commission_currency: String
)

@Dao
interface OperationDao {
    @Query("Select * from Operation")
    fun getAll(): Flow<List<Operation>>

    @Insert
    suspend fun insert(tr: Operation)
}

@Database(entities = [MarketInstrument::class, Operation::class], version = 1)
abstract class TinkoffDB : RoomDatabase() {
    abstract fun marketInstrumentDao(): MarketInstrumentDao
    abstract fun TransactionDao(): OperationDao
}

TinkoffDB_Impl