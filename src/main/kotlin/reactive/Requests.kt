package reactive

import io.reactivex.netty.protocol.http.server.HttpServerRequest
import rx.Observable

interface Request {
    fun perform(data: ReactiveDatabase): Observable<String>
}

private fun <T> HttpServerRequest<T>.getParam(param: String) =
    this.queryParameters[param]?.toList()?.get(0)
        ?: throw IllegalStateException("Expected $param to be present")


class AddProductRequest(private val product: Product) : Request {
    override fun perform(data: ReactiveDatabase) = data.addProduct(product).map { it.toString() }
}

class RegisterUserRequest(private val user: User) : Request {
    override fun perform(data: ReactiveDatabase) = data.registerUser(user).map { it.toString() }
}

object DeleteProductsRequest : Request {
    override fun perform(data: ReactiveDatabase) = data.deleteAllProducts().map { it.toString() }
}

object UnregisterUsersRequest : Request {
    override fun perform(data: ReactiveDatabase) = data.deleteAllUsers().map { it.toString() }
}

class GetProductRequest(private val id: Long) : Request {
    override fun perform(data: ReactiveDatabase) = data
        .getProduct(id)
        .map { it.toString() }
        .singleOrDefault("No product with userId $id")
}


class GetProductsForUserRequest(private val id: Long) : Request {
    override fun perform(data: ReactiveDatabase) = data.getProductsForUser(id).map { it.toString() }
}

class GetUserRequest(private val id: Long) : Request {
    override fun perform(data: ReactiveDatabase) = data
        .getUser(id)
        .map { it.toString() }
        .singleOrDefault("No user with userId $id")
}

class InvalidRequest(private val reason: Throwable) : Request {
    override fun perform(data: ReactiveDatabase) = Observable.just("Invalid command: $reason")
}

fun <T> makeRequest(httpRequest: HttpServerRequest<T>): Request = httpRequest.decodedPath.let { cmd ->
    when (cmd) {
        "registerUser" -> RegisterUserRequest(
            User(
                userId = httpRequest.getParam("userId").toLong(),
                mainCurrency = httpRequest.getParam("currency").toCurrencyOrThrow()
            )
        )
        "getUser" -> GetUserRequest(id = httpRequest.getParam("userId").toLong())
        "addProduct" -> AddProductRequest(
            Product(
                productId = httpRequest.getParam("productId").toLong(),
                price = httpRequest.getParam("price").toDouble()
                        of httpRequest.getParam("currency").toCurrencyOrThrow(),
                name = httpRequest.getParam("name")
            )
        )
        "getProduct" -> GetProductRequest(id = httpRequest.getParam("productId").toLong())
        "deleteProducts" -> DeleteProductsRequest
        "unregisterUser" -> UnregisterUsersRequest
        "productsForUser" -> GetProductsForUserRequest(id = httpRequest.getParam("userId").toLong())
        else -> InvalidRequest(IllegalArgumentException("No such command $cmd"))
    }
}