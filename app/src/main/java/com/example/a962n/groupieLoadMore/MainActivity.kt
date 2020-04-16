package com.example.a962n.groupieLoadMore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a962n.groupieLoadMore.core.GroupLoadMoreAdapter
import com.example.a962n.groupieLoadMore.core.LoadMoreState
import com.example.a962n.groupieLoadMore.databinding.ActivityMainBinding
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var adapter = GroupLoadMoreAdapter<GroupieViewHolder>()
    private var section = Section()

    private val viewModel by lazy {
        ViewModelProvider(this, Factory()).get(MainViewModel::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private class Factory : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(DummyUserListRepositoryImpl()) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        initialize()
        initializeView(binding)
        viewModel.refresh()
    }

    private fun initialize() {
        adapter.add(section)
        adapter.setOnLoadingListener(object : GroupLoadMoreAdapter.OnLoadingListener {
            override fun onLoadMore() {
                // 追加読み込み処理
                viewModel.fetchUserList()
            }
            override fun onRetry() {
                // リトライボタンタップ時の処理
                viewModel.fetchUserList()
            }
        })

        // リストデータ読み込み失敗時の通知
        viewModel.loadFailed.observe(this, Observer { throwable:Throwable ->
            // 読み込みが失敗した場合にRetryを設定することでリトライボタンが表示される
            adapter.setState(LoadMoreState.Retry(throwable.message))
        })
        // リストデータ読み込み成功時の通知
        viewModel.list.observe(this, Observer {
            val items = it.map { entity -> UserListItem(entity) }
            section.update(items)
            // リストデータの読み込みが完了した際にステータスを設定する
            when (items.size > 100) {
                // もう追加読み込みするデータが無い場合は NoMoreを設定。
                // 以降通知が来なくなる。
                true -> adapter.setState(LoadMoreState.NoMore)
                // まだ追加読み込みデータがある場合はReadyを設定
                false -> adapter.setState(LoadMoreState.Ready)
            }
        })
    }

    private fun initializeView(binding: ActivityMainBinding) {
        this.binding = binding

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        baseContext?.apply {
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = adapter
        }
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

}

