package com.tvsoft.portfolioanalysis

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val TAG = "TinkoffDB"

enum class CurrenciesDB(a: Int) {
    USD(0), RUB(1), EUR(2), GBP(3), HKD(4), CHF(5), JPY(6), CNY(7), TRY(8);

    fun get(s: String): CurrenciesDB {
        when(s) {
            "USD" -> return USD
            "RUB" -> return RUB
            "EUR" -> return EUR
            "GBP" -> return GBP
            "HKD" -> return HKD
            "CHF" -> return CHF
            "JPY" -> return JPY
            "CNY" -> return CNY
            "TRY" -> return TRY
        }

        throw IllegalArgumentException("нет валюты: $s")
    }
    
 /*   val values = mapOf(
        "USD" to 0,
        "RUB" to 1,
        "EUR" to 2,
        "GBP" to 3
    )*/
}

enum class InstrumentsTypeDB(a: Int) {
    Stock(0), Currency(1), Bond(2), Etf(3);
    val values = mapOf(
        "Stock" to 0,
        "Currency" to 1,
        "Bond" to 2,
        "Etf" to 3)
}

enum class OperationTypesDB(a: Int) {
    Sell(0), // +
    Buy(1), // -
    BuyCard(2), // дублируется PayIn
    BrokerCommission(3),
    ExchangeCommission(4),
    ServiceCommission(5),
    MarginCommission(6),
    OtherCommission(7),
    PayOut(8),  // - вывод cash
    PayIn(9),  // + ввод
    Dividend(10),
    TaxDividend(11),
    Coupon(12),
    TaxCoupon(13),
    Tax(14),
    TaxLucre(15),
    TaxBack(16),
    PartRepayment(17),
    Repayment(18),
    SecurityIn(19),
    SecurityOut(20);

    fun get(s: String): OperationTypesDB {
        when(s) {
            "Sell" -> return Sell
            "Buy" -> return Buy
            "BuyCard" -> return BuyCard
            "BrokerComission" -> return BrokerCommission
            "ExchangeCommission" -> return ExchangeCommission
            "ServiceCommission" -> return  ServiceCommission
            "MarginCommission" -> MarginCommission
            "OtherCommission" -> OtherCommission
            "PayOut" -> return PayOut
            "PayIn" -> return PayIn
            "Dividend" -> return Dividend
            "TaxDividend" -> return TaxDividend
            "Coupon" -> return Coupon
            "TaxCoupon" -> return TaxCoupon
            "Tax" -> return Tax
            "TaxLucre" -> return TaxLucre
            "TaxBack" -> return TaxBack
            "PartRepayment" -> return PartRepayment
            "Repayment" -> return Repayment
            "SecurityIn" -> return SecurityIn
            "SecurityOut" -> return SecurityOut
        }

        throw IllegalArgumentException("нет операции: $s")
    }
}

/**
 ** PortfolioDB
 */
@Entity(tableName = "portfolio")
data class PortfolioDB(
    @PrimaryKey val id: Int,
    val brokerAccountId: String,
    val name: String
) {}

/**
 ** MarketInstrumentDB
 */
@Entity(
    indices = [
    Index(value = ["currency"]),
    Index(value = ["instrumentType"])])
data class MarketInstrumentDB(
    @PrimaryKey val figi: String,
    val currency: CurrenciesDB,
    val name: String,
    val ticker: String,
    val instrumentType: InstrumentsTypeDB
    ) {
        constructor(instr: ru.tinkoff.invest.openapi.model.rest.MarketInstrument):
            this(figi = instr.figi,
                currency = CurrenciesDB.valueOf(instr.currency.value),
                name = instr.name,
                ticker = instr.ticker,
                instrumentType = InstrumentsTypeDB.valueOf(instr.type.value)
                ) {}
    }

/**
 * OperationDB
**/
fun bigDec2Double(a: java.math.BigDecimal?): Double {
    return a?.toDouble() ?: 0.0
}
@Entity(tableName = "operation",
/*(foreignKeys = [ForeignKey(
    entity = MarketInstrument :: class,
    parentColumns = ["figi"],
    childColumns = ["figi"])])*/
    indices = [
        Index(value = ["portfolio"]),
        Index(value = ["figi"]),
        Index(value = ["date"]),
        Index(value = ["operationType"])])
data class OperationDB(
    @PrimaryKey var id: String,
    val portfolio: Int, //portfolioDB.id
    val figi: String,
    val date: Long, // OffsetDateTime.toEpochSecond()
    val currency: CurrenciesDB,
    val operationType: OperationTypesDB,
    val payment: Double,
    val price: Double,
    val quantity: Int,
    val commission: Double,
    val commissionCurrency: CurrenciesDB?) {
    constructor(p: Int, oper: ru.tinkoff.invest.openapi.model.rest.Operation):
        this(
            id = oper.id,
            portfolio = p,
            figi = oper.figi ?: "",
            date = oper.date.toEpochSecond(),
            currency = CurrenciesDB.valueOf(oper.currency.value),
            operationType = OperationTypesDB.valueOf(oper.operationType.value),
            payment = bigDec2Double(oper.payment),
            price = bigDec2Double(oper.price),
            quantity = oper.quantity ?: 0,
            commission = bigDec2Double(oper.commission?.value),
            commissionCurrency = if(oper.commission != null)
                CurrenciesDB.valueOf(oper.commission.currency.value) else
                    null) {
            if(oper.quantity != oper.quantityExecuted) Log.e(TAG, "${oper} Quantity != QuantityExecuted")
        }
}

@Dao
interface TinkoffDao {
// PortfolioDB
    @Query("Select * from portfolio")
    suspend fun getAllPortfolio(): List<PortfolioDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(p: PortfolioDB)

    @Query("DELETE from portfolio")
    suspend fun deleteAllPortfolio()

//MarketInstrument
    @Query("Select * from MarketInstrumentDB")
    suspend fun getAllMarketInstrument(): List<MarketInstrumentDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketInstrument(mi: MarketInstrumentDB): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketInstrumentList(mi: List<MarketInstrumentDB>): List<Long>

    @Query("Select * from MarketInstrumentDB where figi = :figi")
    fun getMarketInstrument(figi: String): MarketInstrumentDB?

    @Query("DELETE from MarketInstrumentDB")
    suspend fun deleteAllMarketInstrument()

    @Transaction
    suspend fun loadAllMarketInstrument(mi: List<MarketInstrumentDB>) {
        deleteAllMarketInstrument()
        insertMarketInstrumentList(mi)
    }

// OpeartionDB
    @Query("Select * from operation")
    suspend fun getAllOperation(): List<OperationDB>

    @Query("Select * from operation where id=:operId")
    suspend fun findOperationById(operId: String): List<OperationDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(tr: OperationDB)

    @Update
    suspend fun updateOperation(tr: OperationDB)

    @Query("DELETE from operation")
    suspend fun deleteAllOperation()
}

@Database(entities = [PortfolioDB::class, MarketInstrumentDB::class, OperationDB::class], version = 3, exportSchema = false)
abstract class TinkoffDB : RoomDatabase() {
    abstract val tinkoffDao: TinkoffDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: TinkoffDB? = null
        var portfolioList: MutableList<PortfolioDB> = mutableListOf()

        fun getDatabase(context: Context): TinkoffDB {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TinkoffDB::class.java,
                    "tinkoff_db"
                ).fallbackToDestructiveMigration()
                .addCallback(TinkoffDBCallback(context))
                .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        fun populatePortfolios(context: Context) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                portfolioList = getDatabase(context).tinkoffDao.getAllPortfolio().toMutableList()
            }
        }

        fun ioThread(f : () -> Unit) {
            Executors.newSingleThreadExecutor().execute(f)
        }

        private class TinkoffDBCallback(val context: Context) : RoomDatabase.Callback() {
            /**
             * Override the onCreate method to populate the database.
             */
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // If you want to keep the data through app restarts,
                // comment out the following line.
                INSTANCE?.let {
                    populatePortfolios(context)
                }
            }
        }
    }
}