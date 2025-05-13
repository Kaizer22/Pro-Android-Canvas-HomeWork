package otus.homework.customview.pie_chart_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
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
    val onSelectorClickListener: ((PieChartValue?) -> Unit)? = null,
    val selectedSector: Int = -1,
    val superState: Parcelable?,
    val data: PieChartData,
    val amountSum: Int,
) : Parcelable

private val MONTHS = arrayOf(
    "Январе",
    "Феврале",
    "Марте", "Апреле", "Мае", "Июне", "Июле", "Августе", "Сентябре", "Октябре",
    "Ноябре", "Декабре"
)

private const val MAX_VALUES_COUNT = 12
private val GRADIENTS = arrayOf(
    Pair("#36D1DC".toColorInt(), "#D8B5FF".toColorInt()),
    Pair("#FFCE00".toColorInt(), "#A6E088".toColorInt()),
    Pair("#FFBFCB".toColorInt(), "#FFF647".toColorInt()),
    Pair("#F7B733".toColorInt(), "#FF6A00".toColorInt()),
    Pair("#FC5E39".toColorInt(), "#F00000".toColorInt()),
    Pair("#FF758C".toColorInt(), "#C973FF".toColorInt()),
    Pair("#7F00FF".toColorInt(), "#134E5E".toColorInt()),
    Pair("#7F55F9".toColorInt(), "#3ED4D9".toColorInt()),
    Pair("#87D300".toColorInt(), "#4DCF9F".toColorInt()),
    Pair("#F7FD04".toColorInt(), "#FFA751".toColorInt()),
    Pair("#FF6A84".toColorInt(), "#FE881E".toColorInt()),
    Pair("#EC0404".toColorInt(), "#1C1B1B".toColorInt()),
)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val sectorSelectionColor = "#8c43f527"

    private lateinit var viewState: PieChartViewState

    private val randomColorPaint = Paint()
    private val sectorSelectionPaint = Paint().apply {
        color = sectorSelectionColor.toColorInt()
    }

    private fun setGradient(index: Int) = randomColorPaint.setShader(
        LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            GRADIENTS[index].first, GRADIENTS[index].second, Shader.TileMode.MIRROR
        )
    )

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
        if (isInEditMode) {
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

        viewState = PieChartViewState(
            superState = null,
            data = d.copy(values = d.values.sortedBy { it.amount }),
            amountSum = d.values.sumBy { it.amount }
        )

        calculatedAngles = false
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = min(wSize, hSize)
        when (wMode) {
            MeasureSpec.AT_MOST -> setMeasuredDimension(size, size)
            MeasureSpec.EXACTLY -> setMeasuredDimension(size, size)
            MeasureSpec.UNSPECIFIED -> setMeasuredDimension(size, size)
        }

        headerPaint.apply {
            textSize = size / 8f
        }
        subtitlePaint.apply {
            textSize = size / 17f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawArcs(canvas)
        drawCenter(canvas)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return PieChartViewState(
            superState = super.onSaveInstanceState(),
            data = viewState.data,
            amountSum = viewState.amountSum,
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

        if (calculatedAngles) {
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
        }
        return super.onTouchEvent(event)
    }

    private var sectorsAngles: MutableList<Pair<Float, Float>> = mutableListOf()
    private var calculatedAngles = false

    private fun drawArcs(canvas: Canvas) {
        var currentAngle = 0f
        viewState.data.values.forEachIndexed { index, data ->
            val angle = (data.amount.toFloat() / viewState.amountSum) * 360f
            if (!calculatedAngles) {
                sectorsAngles.add(Pair(currentAngle, currentAngle + angle))
            }
            setGradient(index)
            canvas.drawArc(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                currentAngle, angle,
                true,
                randomColorPaint,
            )
            if (viewState.selectedSector == index) {
                canvas.drawArc(
                    0f, 0f,
                    width.toFloat(), height.toFloat(),
                    currentAngle, angle,
                    true,
                    sectorSelectionPaint,
                )
            }
            currentAngle += angle
        }
        calculatedAngles = true
    }

    private fun drawCenter(canvas: Canvas) {
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            width / 2.7f,
            backgroundPaint
        )
        canvas.drawText(
            "${viewState.amountSum} ${viewState.data.currencySymbol}",
            width / 2f,
            height / 2f,
            headerPaint,
        )
        canvas.drawText(
            context.getString(R.string.pie_chart_spend_text, MONTHS[viewState.data.month]),
            width / 2f,
            height / 2f + headerPaint.textSize / 2 + height / 20f,
            subtitlePaint,
        )
    }
}