import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Food(
    val itemId: String = "",
    val title: String = "",
    val category: String = "",
    val picUrl: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val isAvailable: Boolean? = null
)
{
    companion object {
        const val FOOD_TABLE = "food"
        const val FOOD_ID = "itemId"
        const val FOOD_TITLE = "title"
        const val FOOD_CATEGORY = "category"
        const val FOOD_PIC_URL = "picUrl"
        const val FOOD_DESCRIPTION = "description"
        const val FOOD_PRICE = "price"
        const val FOOD_IS_AVAILABLE = "isAvailable"
    }
}