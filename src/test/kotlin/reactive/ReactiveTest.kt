package reactive

import com.mongodb.client.model.Filters
import com.mongodb.rx.client.FindObservable
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import junit.framework.Assert.assertEquals
import org.bson.Document
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import rx.Observable


class ReactiveTest {
    private val db = Mockito.mock(MongoDatabase::class.java)
    private val fakeConverter = CurrencyConverterImpl(
        mapOf(
            (Currency.EUR to Currency.EUR) to (1.0 of Currency.EUR),
            (Currency.EUR to Currency.RUB) to (90.0 of Currency.RUB),
            (Currency.EUR to Currency.USD) to (90.0 / 80 of Currency.USD),

            (Currency.RUB to Currency.EUR) to (1 / 90.0 of Currency.EUR),
            (Currency.RUB to Currency.RUB) to (1.0 of Currency.RUB),
            (Currency.RUB to Currency.USD) to (1.0 / 80 of Currency.USD),

            (Currency.USD to Currency.EUR) to (80.0 / 90.0 of Currency.EUR),
            (Currency.USD to Currency.RUB) to (80.0 of Currency.RUB),
            (Currency.USD to Currency.USD) to (1.0 of Currency.USD)
        )
    )
    private val fakeRates = object : CurrencyRates {
        override val converter = fakeConverter
    }

    private val reactiveDatabase = ReactiveDatabase(db, fakeRates)

    @Test
    fun testGetUser() {
        val userCollection = Mockito.mock(MongoCollection::class.java)
        val findObservable = Mockito.mock(FindObservable::class.java)
        val request = Mockito.mock(HttpServerRequest::class.java)

        `when`(db.getCollection("users")).thenReturn(userCollection as MongoCollection<Document>)
        `when`(
            userCollection.find(
                Filters.eq(
                    "userId",
                    Mockito.anyString()
                )
            )
        ).thenReturn(findObservable as FindObservable<Document>?)

        `when`(findObservable!!.toObservable())
            .thenReturn(
                Observable.just(
                    Document(
                        mapOf(
                            "userId" to "1",
                            "mainCurrency" to "USD"
                        )
                    )
                )
            )
        `when`(request.decodedPath).thenReturn("getUser")
        `when`(request.queryParameters).thenReturn(mapOf("userId" to listOf("1")))

        makeRequest(request).perform(reactiveDatabase).toBlocking().iterator
            .forEach { user ->
                assertEquals("User(userId=1, mainCurrency=USD)", user)
            }
    }

    @Test
    fun testGetProduct() {
        val productCollection = Mockito.mock(MongoCollection::class.java)
        val findObservableProduct = Mockito.mock(FindObservable::class.java)
        val request = Mockito.mock(HttpServerRequest::class.java)

        `when`(db.getCollection("products")).thenReturn(productCollection as MongoCollection<Document>)

        `when`(
            productCollection.find(
                Filters.eq(
                    "productId",
                    Mockito.anyString()
                )
            )
        ).thenReturn(findObservableProduct as FindObservable<Document>)

        `when`(findObservableProduct.toObservable())
            .thenReturn(
                Observable.just(
                    Document(
                        mapOf(
                            "productId" to "239",
                            "name" to "school",
                            "price" to 239000.0,
                            "currency" to "USD"
                        )
                    )
                )
            )
        `when`(request.getDecodedPath()).thenReturn("getProduct")
        `when`(request.getQueryParameters()).thenReturn(mapOf("productId" to listOf("239")))

        makeRequest(request).perform(reactiveDatabase).toBlocking().iterator
            .forEach { product ->
                assertEquals(
                    "Product(productId=239, name=school, price=Price(ammount=239000.0, currency=USD))",
                    product
                )
            }
    }

    @Test
    fun testConversion() {
        val productCollection = Mockito.mock(MongoCollection::class.java) as MongoCollection<Document>
        val userCollection = Mockito.mock(MongoCollection::class.java) as MongoCollection<Document>
        val findObservableProduct = Mockito.mock(FindObservable::class.java) as FindObservable<Document>
        val findObservableUser = Mockito.mock(FindObservable::class.java) as FindObservable<Document>
        val request = Mockito.mock(HttpServerRequest::class.java)

        `when`(db.getCollection("products")).thenReturn(productCollection)
        `when`(db.getCollection("users")).thenReturn(userCollection)
        `when`(userCollection.find(Filters.eq("userId", Mockito.anyString()))).thenReturn(findObservableUser)
        `when`(productCollection.find()).thenReturn(findObservableProduct)
        `when`(findObservableUser.toObservable())
            .thenReturn(
                Observable.just(
                    Document(
                        mapOf(
                            "userId" to "1",
                            "mainCurrency" to "RUB"
                        )
                    )
                )
            )
        `when`(findObservableProduct.toObservable())
            .thenReturn(
                Observable.just(
                    Document(
                        mapOf(
                            "productId" to "239",
                            "name" to "school",
                            "price" to 239000.0,
                            "currency" to "USD"
                        )
                    )
                )
            )
        `when`(request.getDecodedPath()).thenReturn("productsForUser")
        `when`(request.getQueryParameters()).thenReturn(mapOf("userId" to listOf("1"), "productId" to listOf("239")))

        makeRequest(request).perform(reactiveDatabase).toBlocking().iterator.forEach { prod ->
            assertEquals(
                "Product(productId=239, name=school, price=Price(ammount=${239000.0 * 80.0}, currency=RUB))",
                prod
            )
        }
    }
}