import com.mongodb.client.model.Filters
import com.mongodb.rx.client.MongoDatabase
import com.mongodb.rx.client.Success
import org.bson.Document
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers

class ReactiveDatabase(
    private val database: MongoDatabase,
    private val conversionRates: UpToDateCurrencyRates
) {
    private fun <T> observeGet(
        collectionName: String,
        filter: Pair<String, Any>? = null,
        transform: (Document) -> T
    ): Observable<T> =
        database
            .getCollection(collectionName)
            .let {
                if (filter != null) it.find(Filters.eq(filter.first, filter.second))
                else it.find()
            }
            .toObservable()
            .map(transform)
            .subscribeOn(scheduler)

    fun getUser(id: Long) = observeGet("users", filter = "userId" to id) {
        User(id, it.getString("currency").toCurrencyOrThrow())
    }

    fun getProduct(productId: Long) = observeGet("products", filter = "userId" to productId) {
        Product(
            productId,
            name = it.getString("name"),
            price = it.getDouble("price") of it.getString("currency").toCurrencyOrThrow()
        )
    }

    fun getProductsForUser(userId: Long) = getUser(userId).flatMap { user ->
        observeGet("products", filter = null) {
            val price = it.getDouble("price") of it.getString("currency").toCurrencyOrThrow()

            UserListingItem(
                id = it.getLong("userId"),
                name = it.getString("name"),
                price = conversionRates.converter.convert(price, user.mainCurrency)
            )
        }
    }

    fun registerUser(user: User): Observable<Boolean> {
        return getUser(user.userId).singleOrDefault(null).flatMap { userOpt ->
            if (userOpt != null) {
                Observable.just(false)
            } else {
                val document = Document(
                    mutableMapOf(
                        Pair("userId", user.userId),
                        Pair("currency", user.mainCurrency.toString())
                    )
                )
                database.getCollection("users")
                    .insertOne(document)
                    .asObservable()
                    .isEmpty
                    .map { !it }
            }
        }
    }

    fun addProduct(product: Product): Observable<Boolean> {
        return getProduct(product.productId).singleOrDefault(null).flatMap { productOpt ->
            if (productOpt != null) {
                Observable.just(false)
            } else {
                val document = Document(
                    mutableMapOf(
                        Pair("userId", product.productId),
                        Pair("name", product.name),
                        Pair("price", product.price)
                    )
                )
                database.getCollection("products")
                    .insertOne(document)
                    .asObservable()
                    .isEmpty
                    .map { !it }
            }
        }

    }

    fun deleteAllUsers(): Observable<Success> {
        return database.getCollection("users").drop().subscribeOn(scheduler)
    }

    fun deleteAllProducts(): Observable<Success> {
        return database.getCollection("products").drop().subscribeOn(scheduler)
    }


    companion object {
        val scheduler: Scheduler = Schedulers.io()
    }
}