data class User(val userId: Long, val mainCurrency: Currency)


data class Product(val productId: Long, val name: String, val price: Price)

data class UserListingItem(val id: Long, val name: String, val price: Price)
