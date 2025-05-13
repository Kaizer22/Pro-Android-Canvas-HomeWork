package otus.homework.customview

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import otus.homework.customview.category_details_view.CategoryDetailsData
import otus.homework.customview.category_details_view.CategoryDetailsView
import otus.homework.customview.category_details_view.SpendValue
import otus.homework.customview.pie_chart_view.PieChartData
import otus.homework.customview.pie_chart_view.PieChartValue
import otus.homework.customview.pie_chart_view.PieChartView
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Resources.readRawFile(@RawRes id: Int): String =
    openRawResource(id).bufferedReader().use { it.readText() }

data class SpendEntity(
    val id: Int,
    val name: String,
    val amount: Int,
    val category: String,
    val time: Long,
)

private fun getRawValues(
    spends: Array<SpendEntity>,
): List<PieChartValue> {
    val categorizedSpends = mutableMapOf<String, Int>()
    spends.forEach { spend ->
        categorizedSpends[spend.category]?.let {
            categorizedSpends[spend.category] = it + spend.amount
        } ?: run { categorizedSpends[spend.category] = spend.amount }
    }
    return categorizedSpends.map { entry ->
        PieChartValue(entry.key, entry.value)
    }
}

class MainActivity : AppCompatActivity() {

    private var selectedCategory = MutableStateFlow<PieChartValue?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pieChartView = findViewById<PieChartView>(R.id.pieChart)
        val categoryDetailsView = findViewById<CategoryDetailsView>(R.id.categoryDetails)
        val selectedCategoryLabel = findViewById<TextView>(R.id.selectedCategory)
        categoryDetailsView.visibility = View.GONE
        val spends = Gson().fromJson(
            resources.readRawFile(R.raw.payload),
            Array<SpendEntity>::class.java
        )

        pieChartView.setData(
            PieChartData(
                month = 5,
                currencySymbol = "₽",
                values = getRawValues(spends)
            )
        )
        pieChartView.setOnSectorClickListener { value ->
            selectedCategory.update { value }
            value?.let {
                selectedCategoryLabel.visibility = View.VISIBLE
                selectedCategoryLabel.text = value.category
            } ?: run { selectedCategoryLabel.visibility = View.GONE }
        }
        lifecycleScope.launchWhenCreated {
            selectedCategory.collectLatest { value ->
                value?.let {
                    categoryDetailsView.visibility = View.VISIBLE
                    var data = spends.filter { it.category == value.category }
                        .map {
                            SpendValue(
                                amount = it.amount,
                                date = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(it.time),
                                    ZoneId.systemDefault()
                                )
                            )
                        }
                    if (data.size > 1) {
                        data = data.groupingBy { it.date.dayOfYear }
                            .reduce { _, acc, element ->
                                acc.copy(amount = acc.amount + element.amount)
                            }.values.toList()
                    }
                    categoryDetailsView.setData(
                        CategoryDetailsData(
                            spends = data,
                            category = value.category
                        )
                    )
                } ?: run { categoryDetailsView.visibility = View.GONE }
            }
        }
    }
}