package id.diskola.app.dataclass.ResponData

data class MenuItem(
    val title: String,
    val iconRes: Int
)

data class TransactionItem(
    val iconRes: Int,
    val title: String,
    val orderId: String,
    val date: String,
    val time: String,
    val status: String,
    val amount: String
)

