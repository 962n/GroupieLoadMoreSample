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

/**
 * 追加読み込みに対応したGroupAdapter
 * 状態によって、最後尾にローディング or 再試行ボタンを表示する。
 */
class GroupLoadMoreAdapter<VH : GroupieViewHolder> : GroupAdapter<VH>() {

    private var state: LoadMoreState = LoadMoreState.Ready
    private var listener: Listener? = null

    /**
     * 追加読み込み用のリスナー
     */
    interface Listener {
        /**
         * 追加処理を実装してください
         */
        fun onLoadMore()

        /**
         * リトライ処理を実装してください。
         */
        fun onRetry()
    }

    /**
     * リスナー設定処理
     * @param listener 追加読み込み用のリスナー
     */
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * 状態の設定処理
     * @param state 追加読み込み状態
     */
    fun setState(state: LoadMoreState) {
        val previousState = this.state
        val hadExtraRow = hasExtraRow()
        this.state = state
        val hasExtraRow = hasExtraRow()
        // 最後尾のアイテム表示に変更が起きたかどうか
        val hasChangedExtra = hadExtraRow != hasExtraRow
        // 最後尾のアイテムの中身(ローディング or 再試行)に変更が起きたかどうか
        val hasChangedExtraContent = hasExtraRow && previousState != this.state
        if (hasChangedExtra || hasChangedExtraContent) {
            /**
             * Note: 本来であればnotifyItemInserted,notifyItemRemoved,notifyItemChangedなどの
             *       部分更新で最後尾のアイテム更新をしたいが、部分更新で行うと勝手にスクロール位置がずれてしまうケースがある。
             *       そのため、notifyDataSetChangedで更新を行う。
             */
            notifyDataSetChanged()
        }
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
                /**
                 * Note: 一番最下部までスクロールしたかどうかではなく
                 *       スクロール位置が全体アイテム数の90%を超えたタイミングでコールバックを実行。(プリフェッチ)
                 */
                listener?.apply {
                    setState(LoadMoreState.Loading)
                    this.onLoadMore()
                }
            }
        }
    }

    /**
     * 実際のアイテム数を返却する。
     * (ローディング or 再試行ボタン用の表示アイテムがあるため)
     * @return 実際のアイテム数
     */
    fun getActualItemCount(): Int {
        return super.getItemCount()
    }

    /**
     * 最後尾にローディング or 再試行用のアイテムを表示させるかどうか
     * @return true:表示させる / false: 表示させない
     */
    private fun hasExtraRow(): Boolean {
        return when (state) {
            is LoadMoreState.Loading -> true
            is LoadMoreState.Retry -> true
            else -> false
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