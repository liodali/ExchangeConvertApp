package dali.hamza.echangecurrencyapp.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T, E : BaseAdapter.BaseViewHolder<T>>(
    protected val list: MutableList<T> = emptyList<T>().toMutableList()
) :
    RecyclerView.Adapter<E>() {


    abstract class BaseViewHolder<T>(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(data: T)
    }


    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: E, position: Int) {
        val data: T = list[position]
        holder.bind(data)
    }

    fun removeItemPosition(position: Int) {
        list.removeAt(position)
        notifyDataSetChanged()
    }

}