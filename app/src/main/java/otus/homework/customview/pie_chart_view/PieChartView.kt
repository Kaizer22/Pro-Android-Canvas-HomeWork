package otus.homework.customview.pie_chart_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import otus.homework.customview.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

@Parcelize
data class PieChartValue(
    val category: String,
    val amount: Int,
) : Parcelable

@Parcelize
data class PieChartData(
    val month: Int,
    val currencySymbol: String,
    val values: List<PieChartValue>,
) : Parcelable

@Parcelize
data class PieChartViewState(
    @IgnoredOnParcel
    val onSelectorClickListener: ((PieChartValue?) -> Unit)? = null,
    val selectedSector: Int = -1,
    val superState: Parcelable?,
    val data: PieChartData,
    val amountSum: Int,
    val monthText: String,
    val amountText: String,
) : Parcelable

private val MONTHS = arrayOf(
    "Январе",
    "Феврале",
    "Марте", "Апреле", "Мае", "Июне", "Июле", "Августе", "Сентябре", "Октябре",
    "Ноябре", "Декабре"
)

private const val MAX_VALUES_COUNT = 12
private val COLORS = arrayOf(
    "#36D1DC".toColorInt(),
    "#FFCE00".toColorInt(),
    "#FFBFCB".toColorInt(),
    "#F7B733".toColorInt(),
    "#FC5E39".toColorInt(),
    "#FF758C".toColorInt(),
    "#7F00FF".toColorInt(),
    "#7F55F9".toColorInt(),
    "#87D300".toColorInt(),
    "#F7FD04".toColorInt(),
    "#FF6A84".toColorInt(),
    "#EC0404".toColorInt(),
)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val sectorSelectionColor = "#8c43f527"

    private lateinit var viewState: PieChartViewState

    private val sectorColorPaint = Paint()
    private val sectorSelectionPaint = Paint().apply {
        color = sectorSelectionColor.toColorInt()
    }

    private var sectorsAngles: MutableList<Pair<Float, Float>> = mutableListOf()

    private fun setColor(index: Int) {
        sectorColorPaint.color = COLORS[index]
    }

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
    }

    private val headerPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }

    private val subtitlePaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.DKGRAY
    }

    init {
        setData(
            PieChartData(
                month = 8,
                currencySymbol = "₽",
                values = listOf(
                    PieChartValue(
                        "Test", 1000
                    ),
                    PieChartValue(
                        "Test2", 1000
                    ),
                    PieChartValue(
                        "Test4", 1000,
                    ),
                    PieChartValue(
                        "Test5", 1000,
                    ),
                    PieChartValue(
                        "Test5", 1000,
                    ),
                    PieChartValue(
                        "Test5", 1000,
                    ),
                    PieChartValue(
                        "Test5", 1000,
                    ),
                    PieChartValue(
                        "Test5", 1000,
                    ),
                    PieChartValue(
                        "Test5", 10000,
                    ),
                    PieChartValue(
                        "Test5", 10000,
                    ),
                )
            )
        )
    }

    fun setOnSectorClickListener(listener: (PieChartValue?) -> Unit) {
        viewState = viewState.copy(
            onSelectorClickListener = listener
        )
    }

    fun setData(d: PieChartData) {
        if (d.values.size > MAX_VALUES_COUNT)
            throw IllegalArgumentException(
                "PieChartView: Max values count cannot be greater than $MAX_VALUES_COUNT"
            )

        val sortedData = d.copy(values = d.values.sortedBy { it.amount })
        val amountSum = d.values.sumBy { it.amount }
        viewState = PieChartViewState(
            superState = null,
            data = sortedData,
            amountSum = amountSum,
            monthText = context.getString(
                R.string.pie_chart_spend_text, MONTHS[sortedData.month]
            ),
            amountText = "$amountSum ${sortedData.currencySymbol}",
        )
        calculateSectorsAngles()
        invalidate()
    }

    private fun calculateSectorsAngles() {
        var currentAngle = 0f
        sectorsAngles.clear()
        viewState.data.values.forEach {
            val angle = (it.amount.toFloat() / viewState.amountSum) * 360f
            sectorsAngles.add(Pair(currentAngle, currentAngle + angle))
            currentAngle += angle
        }
    }

    private var availableSize = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = min(wSize, hSize)
        availableSize = min(
            size - paddingStart - paddingEnd,
            size - paddingTop - paddingBottom,
        )
        when (wMode) {
            MeasureSpec.AT_MOST -> setMeasuredDimension(size, size)
            MeasureSpec.EXACTLY -> setMeasuredDimension(size, size)
            MeasureSpec.UNSPECIFIED -> setMeasuredDimension(size, size)
        }

        headerPaint.apply {
            textSize = availableSize / 8f
        }
        subtitlePaint.apply {
            textSize = availableSize / 17f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(paddingStart.toFloat(), paddingTop.toFloat())
        drawArcs(canvas)
        drawCenter(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        return PieChartViewState(
            superState = super.onSaveInstanceState(),
            data = viewState.data,
            amountSum = viewState.amountSum,
            monthText = viewState.monthText,
            amountText = viewState.amountText,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is PieChartViewState) {
            super.onRestoreInstanceState(state.superState)
            viewState = state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        val angle = atan2(
            y - height / 2,
            x - width / 2
        )
        val normalizedAngle = if (angle < 0) (angle + 2 * PI.toFloat()) else angle
        val degree = normalizedAngle * 180 / PI

        sectorsAngles.forEachIndexed { index, sector ->
            if (sector.first < degree && sector.second > degree) {
                val new = if (viewState.selectedSector == index) -1 else
                    index
                viewState = viewState.copy(
                    selectedSector = new
                )
                viewState.onSelectorClickListener?.invoke(
                    viewState.data.values.getOrNull(new)
                )
            }
        }
        invalidate()
        return super.onTouchEvent(event)
    }

    private fun drawArcs(canvas: Canvas) {
        viewState.data.values.forEachIndexed { index, data ->
            setColor(index)
            canvas.drawArc(
                0f, 0f,
                availableSize.toFloat(), availableSize.toFloat(),
                sectorsAngles[index].first,
                sectorsAngles[index].second - sectorsAngles[index].first,
                true,
                sectorColorPaint,
            )
            if (viewState.selectedSector == index) {
                canvas.drawArc(
                    0f, 0f,
                    availableSize.toFloat(), availableSize.toFloat(),
                    sectorsAngles[index].first,
                    sectorsAngles[index].second - sectorsAngles[index].first,
                    true,
                    sectorSelectionPaint,
                )
            }
        }
    }

    private fun drawCenter(canvas: Canvas) {
        canvas.drawCircle(
            availableSize / 2f,
            availableSize / 2f,
            availableSize / 2.7f,
            backgroundPaint
        )
        canvas.drawText(
            viewState.amountText,
            availableSize / 2f,
            availableSize / 2f,
            headerPaint,
        )
        canvas.drawText(
            viewState.monthText,
            availableSize / 2f,
            availableSize / 2f + headerPaint.textSize / 2 + availableSize / 20f,
            subtitlePaint,
        )
    }
}