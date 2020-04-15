package com.example.a962n.groupieLoadMore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a962n.groupieLoadMore.core.GroupieLoadMoreAdapter
import com.example.a962n.groupieLoadMore.core.LoadMoreState
import com.example.a962n.groupieLoadMore.databinding.ActivityMainBinding
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var adapter = GroupieLoadMoreAdapter<GroupieViewHolder>()
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
        adapter.setListener(object : GroupieLoadMoreAdapter.Listener {
            override fun onLoadMore() {
                viewModel.fetchUserList()
            }
            override fun onRetry() {
                viewModel.fetchUserList()
            }
        })
        viewModel.loadFailed.observe(this, Observer {
            binding.swipeRefresh.isRefreshing = false
            adapter.setState(LoadMoreState.Retry(it.message))
        })
        viewModel.list.observe(this, Observer {
            val items = it.map { entity -> UserListItem(entity) }
            section.update(items)
            binding.swipeRefresh.isRefreshing = false
            when(items.size > 100) {
                true -> adapter.setState(LoadMoreState.NoMore)
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
    }

}

