package com.example.a962n.groupieLoadMore.core

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.a962n.groupieLoadMore.R
import com.example.a962n.groupieLoadMore.databinding.ItemLoadMoreBinding
import com.xwray.groupie.*
import com.xwray.groupie.databinding.BindableItem
import java.lang.IllegalStateException

/**
 * 追加読み込みに関する状態クラス
 */
sealed class LoadMoreState {
    /**
     * 追加読み込み可能な状態
     * 本状態がAdapterに設定されている場合、onLoadMoreコールバックが実行される
     */
    object Ready : LoadMoreState() {
        override fun getId() = this.javaClass.simpleName.hashCode().toLong()
    }

    /**
     * 追加読み込み中状態
     */
    object Loading : LoadMoreState() {
        override fun getId() = this.javaClass.simpleName.hashCode().toLong()
    }

    /**
     * もうこれ以上、追加読み込みをしない状態。
     */
    object NoMore : LoadMoreState() {
        override fun getId() = this.javaClass.simpleName.hashCode().toLong()
    }

    /**
     * 再試行ボタンを表示する状態
     * データ取得失敗時などに本状態を設定する
     * @param errorMessage エラーメッセージ
     */
    data class Retry(val errorMessage: String?) : LoadMoreState() {
        override fun getId() = (this.javaClass.simpleName + errorMessage).hashCode().toLong()
    }

    abstract fun getId(): Long
}

private class LoadMoreItem constructor(
    private val state: LoadMoreState,
    private var retry: (() -> Unit)? = null
) : BindableItem<ItemLoadMoreBinding>(state.getId()) {

    private val innerRetryClickListener = View.OnClickListener {
        retry?.invoke()
    }

    override fun bind(viewBinding: ItemLoadMoreBinding, position: Int) {
        // 初期化
        viewBinding.errorMsg.visibility = View.GONE
        viewBinding.progressBar.visibility = View.GONE
        viewBinding.retryButton.visibility = View.GONE

        when (state) {
            is LoadMoreState.Loading -> {
                viewBinding.progressBar.visibility = View.VISIBLE
            }
            is LoadMoreState.Retry -> {
                viewBinding.errorMsg.visibility = View.VISIBLE
                viewBinding.errorMsg.text = state.errorMessage
                viewBinding.retryButton.visibility = View.VISIBLE
                viewBinding.retryButton.setOnClickListener(innerRetryClickListener)
            }
            else -> {
                throw IllegalStateException("please set LoadMoreState.Loading or LoadMoreState.Retry")
            }
        }
    }
    override fun getLayout() = R.layout.item_load_more
    override fun isClickable() = false
    override fun isLongClickable() = false
}

class GroupieLoadMoreAdapter<VH : GroupieViewHolder> : GroupAdapter<VH>() {

    private var state: LoadMoreState = LoadMoreState.Ready
    private var listener: Listener? = null

    interface Listener {
        fun onLoadMore()
        fun onRetry()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }


    private val innerScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (state !is LoadMoreState.Ready) {
                return
            }

            val layoutManager = recyclerView.layoutManager ?: return

            val totalItemCount = layoutManager.itemCount
            if (totalItemCount == 0) {
                return
            }
            val lastVisibleItemPosition = when (layoutManager) {
                is LinearLayoutManager -> {
                    layoutManager.findLastVisibleItemPosition()
                }
                is GridLayoutManager -> {
                    layoutManager.findLastVisibleItemPosition()
                }
                else -> {
                    throw IllegalStateException("sorry this class only support LinearLayoutManager or GridLayoutManager")
                }
            }

            if (totalItemCount * 0.9 < lastVisibleItemPosition + 1) {
                listener?.apply {
                    setState(LoadMoreState.Loading)
                    this.onLoadMore()
                }
            }
        }
    }

    fun getActualItemCount(): Int {
        return super.getItemCount()
    }

    private fun hasExtraRow(): Boolean {
        return when (state) {
            is LoadMoreState.Loading -> true
            is LoadMoreState.Retry -> true
            else -> false
        }
    }

    fun setState(state: LoadMoreState) {
        val previousState = this.state
        val hadExtraRow = hasExtraRow()
        this.state = state
        val hasExtraRow = hasExtraRow()
        val hasChangedExtra = hadExtraRow != hasExtraRow
        val hasChangedExtraContent = hasExtraRow && previousState != this.state
        if (hasChangedExtra || hasChangedExtraContent) {
            notifyDataSetChanged()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(innerScrollListener)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(innerScrollListener)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItem(position: Int): Item<*> {
        val lastPosition = itemCount - 1
        return when (hasExtraRow() && lastPosition == position) {
            true -> LoadMoreItem(state) {
                setState(LoadMoreState.Loading)
                listener?.onRetry()
            }
            false -> super.getItem(position)
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }
}