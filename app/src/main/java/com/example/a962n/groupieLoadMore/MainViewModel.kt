package com.example.a962n.groupieLoadMore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel constructor(
    private val userListRepository: UserListRepository
) : ViewModel() {
    companion object {
        private const val FETCH_SIZE = 20
    }

    private var nextPage = 0
    var list: MutableLiveData<List<UserEntity>> = MutableLiveData(emptyList())
    val loadFailed: MutableLiveData<Throwable> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    fun refresh() {
        isLoading.value = true
        nextPage = 0
        fetchUserList()
    }

    fun fetchUserList() {
        userListRepository.fetch(
            nextPage,
            FETCH_SIZE,
            {
                val mutableList = list.value?.toMutableList() ?: mutableListOf()
                if (nextPage == 0) {
                    isLoading.value = false
                    mutableList.removeAll { true }
                }
                mutableList.addAll(it)
                nextPage++
                list.value = mutableList
            }, {
                if (nextPage == 0) {
                    isLoading.value = false
                }
                loadFailed.value = it
            }
        )

    }

}