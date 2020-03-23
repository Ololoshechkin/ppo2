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

object DeleteUsersRequest : Request {
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

fun <T> makeRequest(httpRequest: HttpServerRequest<T>): Request = httpRequest.decodedPath.substring(1).let { cmd ->
    when (cmd) {
        "add_user" -> RegisterUserRequest(
            User(
                userId = httpRequest.getParam("userId").toLong(),
                mainCurrency = httpRequest.getParam("currency").toCurrencyOrThrow()
            )
        )
        "get_user" -> GetUserRequest(id = httpRequest.getParam("userId").toLong())
        "add_product" -> AddProductRequest(
            Product(
                productId = httpRequest.getParam("userId").toLong(),
                price = httpRequest.getParam("price").toDouble()
                        of httpRequest.getParam("currency").toCurrencyOrThrow(),
                name = httpRequest.getParam("name")
            )
        )
        "get_product" -> GetProductRequest(id = httpRequest.getParam("userId").toLong())
        "delete_products" -> DeleteProductsRequest
        "delete_users" -> DeleteUsersRequest
        "list_for_user" -> GetProductsForUserRequest(id = httpRequest.getParam("userId").toLong())
        else -> InvalidRequest(IllegalArgumentException("No such command $cmd"))
    }
}