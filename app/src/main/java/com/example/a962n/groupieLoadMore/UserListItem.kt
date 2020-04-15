package com.example.a962n.groupieLoadMore

import com.example.a962n.groupieLoadMore.databinding.ItemUserListBinding
import com.xwray.groupie.databinding.BindableItem

class UserListItem constructor(
    private val userEntity: UserEntity
) : BindableItem<ItemUserListBinding>(userEntity.id.toLong()) {
    override fun getLayout(): Int {
        return R.layout.item_user_list
    }
    override fun bind(viewBinding: ItemUserListBinding, position: Int) {
        viewBinding.entity = userEntity
    }
}