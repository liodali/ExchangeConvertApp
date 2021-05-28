package dali.hamza.echangecurrencyapp.ui.utitlies

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.common.disabled
import dali.hamza.echangecurrencyapp.common.enabled
import dali.hamza.echangecurrencyapp.databinding.ViewTitleBottomSheetBinding


class TitleBottomSheet(context: Context, attrs: AttributeSet?, defStyle: Int?) :
    FrameLayout(context, attrs, defStyle!!) {


    constructor(context: Context, attrs: AttributeSet) : this(
        context,
        attrs,
        0
    )

    constructor(context: Context) : this(context, null, null)

    private var binding: ViewTitleBottomSheetBinding
    private var title: TextView
    private var closeBottomSheet: MaterialButton
    private var positiveButtonBottomSheet: MaterialButton


    var positiveAction: OnClickListener? = null
        set(value) {
            positiveButtonBottomSheet.setOnClickListener(
                value
            )
        }
    var closeAction: OnClickListener? = null
        set(value) {
            closeBottomSheet.setOnClickListener(
                value
            )
        }

    init {
        val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewTitleBottomSheetBinding.inflate(inflater, this, true)
        title = binding.idTitleTxtBottomSheet
        closeBottomSheet = binding.idCloseBtBottomSheet
        positiveButtonBottomSheet = binding.idCreateBtBottomSheet

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TitleBottomSheet,
            0, 0
        ).apply {

            try {
                title.text = getString(R.styleable.TitleBottomSheet_titleText) ?: ""
                if (getBoolean(R.styleable.TitleBottomSheet_enablePositiveAction, true))
                    positiveButtonBottomSheet.enabled()
                else {
                    positiveButtonBottomSheet.disabled()

                }
                positiveButtonBottomSheet.text =
                    getString(R.styleable.TitleBottomSheet_positiveButtonText)
                        ?: resources.getString(R.string.selectLabel)
            } finally {
                recycle()
            }
        }
    }

    fun enableOrDisablePositiveAction(enable: Boolean) =
        if (enable) positiveButtonBottomSheet.enabled()
        else positiveButtonBottomSheet.disabled()


}