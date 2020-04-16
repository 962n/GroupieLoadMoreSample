package com.example.a962n.groupieLoadMore

import android.os.Handler
import android.os.Looper
import android.util.Log

interface UserListRepository {
    fun fetch(
        page: Int,
        size: Int,
        onSuccess: (List<UserEntity>) -> Unit,
        onError: (Throwable) -> Unit
    )
}

class DummyUserListRepositoryImpl : UserListRepository {
    companion object {
        private var fetchCount = 0
    }

    override fun fetch(
        page: Int,
        size: Int,
        onSuccess: (List<UserEntity>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // サンプルの例として3回に1回エラーを発生させる
        fetchCount++
        when (fetchCount % 3 == 0) {
            true -> {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        Log.d("hoge","onError,failed!!")
                        onError(Exception("fetch failed!!"))
                    }, 3000
                )
            }
            false -> {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        val list = mutableListOf<UserEntity>()
                        repeat(size) {
                            val id: String = (it + page * size).toString()
                            list.add(UserEntity(id, randomName()))
                        }

                        onSuccess(list)
                    }, 3000
                )
            }
        }
    }

    private fun randomName(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..30)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

    }

}

