package dali.hamza.echangecurrencyapp.ui.utitlies

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.common.gone
import dali.hamza.echangecurrencyapp.common.visible
import dali.hamza.echangecurrencyapp.databinding.AppRecyclerViewBinding
import dali.hamza.echangecurrencyapp.databinding.ChargementBinding
import dali.hamza.echangecurrencyapp.ui.adapter.BaseAdapter

class AppRecyclerView<T : BaseAdapter<*, *>, K : Any>(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int?
) :
    FrameLayout(context, attrs, defStyle!!) {

    var data: MutableList<K> = emptyList<K>().toMutableList()
        set(value) {
            data.clear()
            data.addAll(value)
            adapter?.notifyItemInserted(0)
            if (viewLoading.root.isVisible) {
                viewLoading.root.gone()
                recyclerView.visible()
            }
        }
    var adapter: T? = null
        set(value) {
            recyclerView.adapter = value
            field = value
        }
    private var recyclerView: RecyclerView
    private var viewLoading: ChargementBinding
    private var binding: AppRecyclerViewBinding

    constructor(context: Context, attrs: AttributeSet) : this(
        context,
        attrs,
        0
    )

    init {
        val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = AppRecyclerViewBinding.inflate(inflater, this, true)
        recyclerView = binding.idAppRecyclerView
        viewLoading = binding.idLoadingLayout
        viewLoading.idChargementText.text =
            resources.getString(R.string.loadingText)
        recyclerView.gone()

    }

    fun addItems(item: K, position: Int = -1) {
        if (position == -1)
            data.add(item)
        else
            data.add(position, item)
        adapter?.notifyItemInserted(0)
    }

    fun removeItems(item: K) {
        val index = data.indexOf(item)
        data.remove(item)
        if (index != -1)
            adapter?.removeItemPosition(index)
    }

    fun goToPosition(position: Int) {
        recyclerView.smoothScrollToPosition(position)
    }

    fun setLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        recyclerView.layoutManager = layoutManager
    }


}