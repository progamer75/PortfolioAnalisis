package com.tvsoft.portfolioanalysis

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    fun itName(): String {
        return when(this) {
            USD -> "USD"
            RUB -> "RUB"
            EUR -> "EUR"
            GBP -> "GBP"
            HKD -> "HKD"
            CHF -> "CHF"
            JPY -> "JPY"
            CNY -> "CNY"
            TRY -> "TRY"
        }
    }
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

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): OffsetDateTime? {
        return value?.let { OffsetDateTime.of(LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC), ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: OffsetDateTime?): Long? {
        return date?.toEpochSecond()
    }

    @TypeConverter
    fun fromLD(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToLD(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun currencyToInt(cur: CurrenciesDB?): Int? {
        return cur?.ordinal
    }

    @TypeConverter
    fun intToCurrency(id: Int): CurrenciesDB? {
        return CurrenciesDB.values()[id]
    }

    @TypeConverter
    fun instrumentTypeToInt(cur: InstrumentsTypeDB?): Int? {
        return cur?.ordinal
    }

    @TypeConverter
    fun intToInstrumentType(id: Int): InstrumentsTypeDB? {
        return InstrumentsTypeDB.values()[id]
    }

    @TypeConverter
    fun operationTypeToInt(cur: OperationTypesDB?): Int? {
        return cur?.ordinal
    }

    @TypeConverter
    fun intToOperationType(id: Int): OperationTypesDB? {
        return OperationTypesDB.values()[id]
    }
}

/**
 ** ExchangeRates
 */
@Entity(tableName = "exchange_rate",
        primaryKeys = [
        "currency",
        "date"])
data class ExchangeRateDB(
    val currency: CurrenciesDB,
    val date: LocalDate,
    val rate: Double
) { constructor(e: ExchangeRate): this(
    currency = e.currency,
    date = e.date,
    rate = e.rate
)}

data class SumOf(
    val sumOf: Long
)

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
    val lot: Int,
    val minQuantity: Int,
    val instrumentType: InstrumentsTypeDB
    ) {
        constructor(instr: ru.tinkoff.invest.openapi.model.rest.MarketInstrument):
            this(figi = instr.figi,
                currency = CurrenciesDB.valueOf(instr.currency.value),
                name = instr.name,
                ticker = instr.ticker,
                lot = instr.lot ?: 0,
                minQuantity = instr.minQuantity ?: 0,
                instrumentType = InstrumentsTypeDB.valueOf(instr.type.value)
                ) {
            }
    }

/**
 * OperationDB
**/
fun bigDec2LongCent(a: java.math.BigDecimal?): Long {
    return a?.multiply(BigDecimal.valueOf(100))?.toLong() ?: 0
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
    val date: OffsetDateTime, // OffsetDateTime.toEpochSecond()
    val currency: CurrenciesDB,
    val operationType: OperationTypesDB,
    val payment: Long,
    val price: Long,
    val quantity: Int,
    val commission: Long,
    val commissionCurrency: CurrenciesDB?) {
    constructor(p: Int, oper: ru.tinkoff.invest.openapi.model.rest.Operation):
        this(
            id = oper.id,
            portfolio = p,
            figi = oper.figi ?: "",
            date = oper.date,
            currency = CurrenciesDB.valueOf(oper.currency.value),
            operationType = OperationTypesDB.valueOf(oper.operationType.value),
            payment = bigDec2LongCent(oper.payment),
            price = bigDec2LongCent(oper.price),
            quantity = oper.quantityExecuted ?: 0,
            commission = bigDec2LongCent(oper.commission?.value),
            commissionCurrency = if(oper.commission != null)
                CurrenciesDB.valueOf(oper.commission.currency.value) else
                    CurrenciesDB.USD) {
            //if(oper.quantity != oper.quantityExecuted) Log.e(TAG, "${oper} Quantity != QuantityExecuted")
        }
}

@Dao
interface TinkoffDao {
// ExchangeRatesDB
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateDB)

    @Query("DELETE from exchange_rate")
    suspend fun deleteAllRates()

    @Query("Select rate from exchange_rate where currency=:currency and date=:date")
    suspend fun _getRate(currency: CurrenciesDB, date: LocalDate): Double

    suspend fun getRate(currency: CurrenciesDB, date: LocalDate): Double {
        return if(currency == CurrenciesDB.RUB)
            1.0
        else
            _getRate(currency, date)
    }

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
    suspend fun getMarketInstrument(figi: String): MarketInstrumentDB?

    @Query("DELETE from MarketInstrumentDB")
    suspend fun deleteAllMarketInstrument()

    @Transaction
    suspend fun loadAllMarketInstrument(mi: List<MarketInstrumentDB>) {
        deleteAllMarketInstrument()
        insertMarketInstrumentList(mi)
    }

// OperationDB
    @Query("Select * from operation")
    suspend fun getAllOperation(): List<OperationDB>

    @Query("Select * from operation where (portfolio=:portfolio and operationType=:oper and figi=:figi) order by date desc")
    suspend fun getOperations(portfolio: Int, figi: String, oper:OperationTypesDB = OperationTypesDB.Buy): List<OperationDB>

    @Query("Select sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from)")
    suspend fun getDividends(portfolio: Int, figi: String, from: OffsetDateTime,
                             oper:List<OperationTypesDB> = listOf(OperationTypesDB.Dividend,
                                OperationTypesDB.Coupon)): SumOf

    @Query("Select sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from)")
    suspend fun getDividendsTax(portfolio: Int, figi: String, from: OffsetDateTime,
                                oper:List<OperationTypesDB> = listOf(OperationTypesDB.TaxDividend,
                                OperationTypesDB.TaxCoupon)): SumOf

    @Query("Select * from operation where id=:operId")
    suspend fun findOperationById(operId: String): List<OperationDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(tr: OperationDB)

    @Update
    suspend fun updateOperation(tr: OperationDB)

    @Query("DELETE from operation")
    suspend fun deleteAllOperation()
}

@Database(entities = [ExchangeRateDB::class, PortfolioDB::class, MarketInstrumentDB::class, OperationDB::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
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