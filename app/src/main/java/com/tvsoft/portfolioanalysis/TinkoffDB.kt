package com.tvsoft.portfolioanalysis

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tvsoft.portfolioanalysis.Utils.Companion.ts2OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.tinkoff.piapi.contract.v1.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors

private val TAG = "TinkoffDB"

enum class CurrenciesDB(a: Int) {
    USD(0), RUB(1), EUR(2), GBP(3), HKD(4), CHF(5), JPY(6), CNY(7), TRY(8), SEK(9);

    fun getWithNull(s: String): CurrenciesDB? {
        return when(s.uppercase()) {
            "USD" -> USD
            "RUB" -> RUB
            "EUR" -> EUR
            "GBP" -> GBP
            "HKD" -> HKD
            "CHF" -> CHF
            "JPY" -> JPY
            "CNY" -> CNY
            "TRY" -> TRY
            "SEK" -> SEK
            else -> null
        }
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
            SEK -> "SEK"
        }
    }

    fun symbol(): String {
        return when(this) {
            USD -> '\u0024'.toString()
            RUB -> '\u20BD'.toString()
            EUR -> '\u20AC'.toString()
            GBP -> '\u00A3'.toString()
            HKD -> '\u0024'.toString()
            CHF -> '\u20A3'.toString()
            JPY -> '\u00A5'.toString()
            CNY -> '\u04B0'.toString()
            TRY -> '\u20BA'.toString()
            SEK -> "kr"
        }
    }

    fun rubSymbol(): String = '\u20BD'.toString()
}

fun getCurrenciesDB(s: String): CurrenciesDB? {
    return when(s.uppercase()) {
        "USD" -> CurrenciesDB.USD
        "RUB" -> CurrenciesDB.RUB
        "EUR" -> CurrenciesDB.EUR
        "GBP" -> CurrenciesDB.GBP
        "HKD" -> CurrenciesDB.HKD
        "CHF" -> CurrenciesDB.CHF
        "JPY" -> CurrenciesDB.JPY
        "CNY" -> CurrenciesDB.CNY
        "TRY" -> CurrenciesDB.TRY
        "SEK" -> CurrenciesDB.SEK
        else -> null
    }
}

fun getCurrenciesDByFigi(figi: String): CurrenciesDB? {
    return when(figi) {
        "BBG0013HGFT4" -> CurrenciesDB.USD
        "BBG0013HJJ31" -> CurrenciesDB.EUR
        "BBG0013J12N1" -> CurrenciesDB.TRY
        "BBG0013HSW87" -> CurrenciesDB.HKD
        "BBG0013HRTL0" -> CurrenciesDB.CNY
        "BBG0013HQ524" -> CurrenciesDB.JPY
        "BBG0013HQ5K4" -> CurrenciesDB.CHF
        "BBG0013HQ5F0" -> CurrenciesDB.GBP
        else -> null
    }
}

enum class InstrumentTypeDB(a: Int) {
    Stock(0), Currency(1), Bond(2), Etf(3), Null(4);
    val values = mapOf(
        "stock" to 0,
        "currency" to 1,
        "bond" to 2,
        "etf" to 3)
}

fun getInstrumentTypeDB(str: String): InstrumentTypeDB {
    return when(str.lowercase()) {
        "share" -> InstrumentTypeDB.Stock
        "currency" -> InstrumentTypeDB.Currency
        "bond" -> InstrumentTypeDB.Bond
        "etf" -> InstrumentTypeDB.Etf
        else -> InstrumentTypeDB.Null
    }
}

/*enum class OperationTypesDB(a: Int) {
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
    SecurityOut(20),
    BuyCurrency(21),
    SellCurrency(22)
    ;
    //BBG0013HGFT4 USD

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
}*/

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
    fun instrumentTypeToInt(cur: InstrumentTypeDB?): Int? {
        return cur?.ordinal
    }

    @TypeConverter
    fun intToInstrumentType(id: Int): InstrumentTypeDB? {
        return InstrumentTypeDB.values()[id]
    }

/*    @TypeConverter
    fun operationTypeToInt(op: OperationTypesDB?): Int? {
        return op?.ordinal
    }

    @TypeConverter
    fun intToOperationType(id: Int): OperationTypesDB? {
        return OperationTypesDB.values()[id]
    }*/

    @TypeConverter
    fun operationTypeToInt2(op: OperationType): Int {
        return op.ordinal
    }

    @TypeConverter
    fun intToOperationType2(id: Int): OperationType? {
        return OperationType.forNumber(id)
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
    val sumOf: Double
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
    val isin: String,
    val lot: Int,
    val instrumentType: InstrumentTypeDB
    ) {
        constructor(instr: Share):
            this(figi = instr.figi,
                currency = getCurrenciesDB(instr.currency)!!,
                name = instr.name,
                ticker = instr.ticker,
                isin = instr.isin,
                lot = instr.lot ?: 0,
                instrumentType = InstrumentTypeDB.Stock
                )
        constructor(instr: Bond):
                this(figi = instr.figi,
                    currency = getCurrenciesDB(instr.currency)!!,
                    name = instr.name,
                    ticker = instr.ticker,
                    isin = instr.isin,
                    lot = instr.lot ?: 0,
                    instrumentType = InstrumentTypeDB.Bond
                )
        constructor(instr: Etf):
                this(figi = instr.figi,
                    currency = getCurrenciesDB(instr.currency)!!,
                    name = instr.name,
                    ticker = instr.ticker,
                    isin = instr.isin,
                    lot = instr.lot ?: 0,
                    instrumentType = InstrumentTypeDB.Etf
                )
        constructor(instr: Currency):
                this(figi = instr.figi,
                    currency = getCurrenciesDB(instr.currency)!!,
                    name = instr.name,
                    ticker = instr.ticker,
                    isin = instr.isin,
                    lot = instr.lot ?: 0,
                    instrumentType = InstrumentTypeDB.Currency
                )
    }

/**
 * OperationDB
**/
fun bigDec2LongCent(a: java.math.BigDecimal?): Long {
    return a?.multiply(BigDecimal.valueOf(100))?.toLong() ?: 0
}

fun money2Double(a: MoneyValue): Double {
    return (a.units).toDouble() + a.nano.toDouble() / 1000000000.0
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
        Index(value = ["operationType"]),
        Index(value = ["instrumentType"])])
data class OperationDB(
    @PrimaryKey val id: String,
    val portfolio: Int, //portfolioDB.id
    val figi: String,
    val date: OffsetDateTime, // OffsetDateTime.toEpochSecond()
    val currency: CurrenciesDB,
    val operationType: OperationType,
    val instrumentType: InstrumentTypeDB,
    val payment: Double,
    val price: Double,
    val quantity: Int
/*    val commission: Long,
    val commissionCurrency: CurrenciesDB?,
    val profit: Long = 0L*/
    ) {
    constructor(p: Int, oper: Operation):
        this(
            id = oper.id,
            portfolio = p,
            figi = oper.figi ?: "",
            date = ts2OffsetDateTime(oper.date),
            currency =
                getCurrenciesDB(oper.currency)!!,
            operationType = oper.operationType,
            instrumentType = getInstrumentTypeDB(oper.instrumentType),
            payment = money2Double(oper.payment),
            price = money2Double(oper.price),
            quantity = oper.quantity.toInt() // ?: 0,
        )
}

@Dao
interface TinkoffDao {
// ExchangeRatesDB
    @Query("Select * from exchange_rate")
    suspend fun getAllRates(): List<ExchangeRateDB>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateDB)

    @Query("DELETE from exchange_rate")
    suspend fun deleteAllRates()

    @Query("Select rate from exchange_rate where currency=:currency and date=:date")
    suspend fun getRate(currency: CurrenciesDB, date: LocalDate): Double?

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

    @Query("Select count(*) from MarketInstrumentDB")
    suspend fun getNumMarketInstrument(): Int

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
    @Query("Select * from operation where portfolio=:portfolio order by date")
    suspend fun getAllOperation(portfolio: Int): List<OperationDB>

    @Query("Select * from operation where (portfolio=:portfolio and" +
            " (operationType in (:oper))) and" +
            "(instrumentType != :instr) order by date")
    suspend fun getOperationsByType(portfolio: Int, oper:List<OperationType>,
                                    instr:InstrumentTypeDB = InstrumentTypeDB.Null): List<OperationDB>

    @Query("Select * from operation where (portfolio=:portfolio and operationType in (:oper) and figi=:figi) order by date desc")
    suspend fun getOperations(portfolio: Int, figi: String,
                              oper:List<OperationType> = listOf(OperationType.OPERATION_TYPE_BUY)): List<OperationDB>

    @Query("Select sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from and date <= :to)")
    suspend fun getDividends(portfolio: Int, figi: String, from: OffsetDateTime,
                             to: OffsetDateTime = OffsetDateTime.now(),
                             oper:List<OperationType> = listOf(
                                 OperationType.OPERATION_TYPE_DIVIDEND,
                                 OperationType.OPERATION_TYPE_COUPON,
                                 OperationType.OPERATION_TYPE_BOND_REPAYMENT,
                                 OperationType.OPERATION_TYPE_BOND_REPAYMENT_FULL)): SumOf

    @Query("Select -sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from and date <= :to)")
    suspend fun getDividendsTax(portfolio: Int, figi: String, from: OffsetDateTime,
                                to: OffsetDateTime = OffsetDateTime.now(),
                                oper:List<OperationType> = listOf(
                                    OperationType.OPERATION_TYPE_BOND_TAX,
                                    OperationType.OPERATION_TYPE_BOND_TAX_PROGRESSIVE,
                                    OperationType.OPERATION_TYPE_DIVIDEND_TAX,
                                    OperationType.OPERATION_TYPE_DIVIDEND_TAX_PROGRESSIVE
                                )): SumOf

    @Query("Select * from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from and date <= :to)")
    suspend fun getDividendsAndTax(portfolio: Int, figi: String, from: OffsetDateTime,
                             to: OffsetDateTime = OffsetDateTime.now(),
                             oper:List<OperationType> = listOf(
                                 OperationType.OPERATION_TYPE_DIVIDEND,
                                 OperationType.OPERATION_TYPE_COUPON,
                                 OperationType.OPERATION_TYPE_BOND_REPAYMENT,
                                 OperationType.OPERATION_TYPE_BOND_REPAYMENT_FULL,
                                 OperationType.OPERATION_TYPE_BOND_TAX,
                                 OperationType.OPERATION_TYPE_BOND_TAX_PROGRESSIVE,
                                 OperationType.OPERATION_TYPE_DIVIDEND_TAX,
                                 OperationType.OPERATION_TYPE_DIVIDEND_TAX_PROGRESSIVE
                             )): List<OperationDB>

    @Query("Select sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and figi=:figi and date >= :from and date <= :to)")
    suspend fun getComission(portfolio: Int, figi: String, from: OffsetDateTime,
                             to: OffsetDateTime = OffsetDateTime.now(),
                             oper:List<OperationType> = listOf(
                                 OperationType.OPERATION_TYPE_BROKER_FEE,
                                 OperationType.OPERATION_TYPE_MARGIN_FEE,
                                 OperationType.OPERATION_TYPE_SERVICE_FEE,
                                 OperationType.OPERATION_TYPE_SUCCESS_FEE
                                 )): SumOf

    @Query("Select sum(payment) as sumOf from operation where (portfolio=:portfolio and (operationType in (:oper))" +
            " and date >= :from and date <= :to)")
    suspend fun getTotalTax(portfolio: Int, from: OffsetDateTime, // Без налогов с дивидендов
                             to: OffsetDateTime = OffsetDateTime.now(),
                             oper:List<OperationType> = listOf(
                                 OperationType.OPERATION_TYPE_BENEFIT_TAX,
                                 OperationType.OPERATION_TYPE_BENEFIT_TAX_PROGRESSIVE,
                                 OperationType.OPERATION_TYPE_TAX_CORRECTION,
                                 OperationType.OPERATION_TYPE_TAX_CORRECTION_PROGRESSIVE,
                                 OperationType.OPERATION_TYPE_TAX,
                                 OperationType.OPERATION_TYPE_TAX_PROGRESSIVE
                             )): SumOf

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