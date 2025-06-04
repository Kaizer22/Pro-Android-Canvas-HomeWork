package otus.homework.customview.category_details_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Parcelize
data class SpendValue(
    val amount: Int,
    val date: LocalDateTime,
) : Parcelable

@Parcelize
data class CategoryDetailsData(
    val spends: List<SpendValue>,
    val category: String,
) : Parcelable

@Parcelize
data class CategoryDetailsViewState(
    val superState: Parcelable?,
    val data: CategoryDetailsData,
    val dateStrings: List<String>,
    val horizontalLinesCount: Int,
    val horizontalLinesStep: Int,
    val horizontalLinesStrings: List<String>,
) : Parcelable

class CategoryDetailsView(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val divisionPrice = 500
    private val dotRadius = 30f

    private lateinit var viewState: CategoryDetailsViewState

    private var valueWidth = 20f
    private var maxValue = Int.MAX_VALUE

    private val path = Path()
    private val pathPaint = Paint().apply {
        strokeWidth = 10f
        color = Color.BLUE
        pathEffect = CornerPathEffect(30f)
        style = Paint.Style.STROKE
    }
    private val circlePaint = Paint().apply {
        color = Color.BLUE
    }
    private val legendPaint = Paint().apply {
        color = Color.DKGRAY
        pathEffect = DashPathEffect(
            floatArrayOf(5f, 10f), 0F
        )
        textSize = 30f
    }

    init {
        setData(
            CategoryDetailsData(
                spends = listOf(
                    SpendValue(1000, LocalDateTime.now()),
                    SpendValue(2000, LocalDateTime.now().plusDays(2))
                ),
                category = "Здоровье"
            )
        )
    }

    fun setData(d: CategoryDetailsData) {
        val max = d.spends.maxOf { it.amount }
        maxValue = max + max / 10

        val hLinesCount = maxValue / divisionPrice + 1
        val hLinesStrings = mutableListOf<String>()

        repeat(hLinesCount) { index ->
            hLinesStrings.add(
                (hLinesCount * divisionPrice - divisionPrice * index
                        ).toString()
            )
        }

        viewState = CategoryDetailsViewState(
            superState = null,
            data = d,
            dateStrings = d.spends.map { it.date.format(DateTimeFormatter.ISO_DATE) },
            horizontalLinesCount = hLinesCount,
            horizontalLinesStep = height / hLinesCount,
            horizontalLinesStrings = hLinesStrings,
        )

        requestLayout()
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable {
        return CategoryDetailsViewState(
            superState = super.onSaveInstanceState(),
            data = viewState.data,
            dateStrings = viewState.dateStrings,
            horizontalLinesCount = viewState.horizontalLinesCount,
            horizontalLinesStep = viewState.horizontalLinesStep,
            horizontalLinesStrings = viewState.horizontalLinesStrings,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is CategoryDetailsViewState) {
            super.onRestoreInstanceState(state.superState)
            viewState = state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        when (wMode) {
            MeasureSpec.AT_MOST -> setMeasuredDimension(wSize, hSize)
            MeasureSpec.EXACTLY -> setMeasuredDimension(wSize, hSize)
            MeasureSpec.UNSPECIFIED -> setMeasuredDimension(wSize, hSize)
        }
        valueWidth = wSize.toFloat() / viewState.data.spends.size
    }

    override fun onDraw(canvas: Canvas) {
        drawPath(canvas)
        drawHorizontalLegend(canvas)
        drawVerticalLegend(canvas)
    }

    private fun drawVerticalLegend(canvas: Canvas) {
        val verticalLines = (width / valueWidth).toInt()
        repeat(verticalLines) { index ->
            val x = valueWidth * (index + 1)
            canvas.drawLine(x, 0f, x, height.toFloat(), legendPaint)
            canvas.drawText(
                viewState.dateStrings[index],
                x - valueWidth / 2, height.toFloat(),
                legendPaint
            )
        }
    }

    private fun drawHorizontalLegend(canvas: Canvas) {
        repeat(viewState.horizontalLinesCount) { index ->
            val xStartHorizontal = 0f
            val yStartHorizontal = (index * viewState.horizontalLinesStep).toFloat()
            canvas.drawText(
                viewState.horizontalLinesStrings[index],
                xStartHorizontal, yStartHorizontal, legendPaint
            )
            canvas.drawLine(
                xStartHorizontal, yStartHorizontal,
                width.toFloat(), (index * viewState.horizontalLinesStep).toFloat(),
                legendPaint,
            )
        }
        canvas.drawText("0", 0f, height.toFloat(), legendPaint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), legendPaint)
    }

    private fun drawPath(canvas: Canvas) {
        path.reset()
        path.moveTo(0f, height.toFloat())
        viewState.data.spends.forEachIndexed { index, value ->
            val x = valueWidth * index + valueWidth / 2
            val y = height - value.amount.toFloat() / maxValue * height
            path.lineTo(x, y)
            canvas.drawCircle(x, y, dotRadius, circlePaint)
        }
        canvas.drawPath(path, pathPaint)
    }
}