package com.example.data.local.seed

import com.example.data.model.ShayariEntity

internal fun shayariItem(
    id: Int,
    couplet: String,
    poet: String,
    categoryId: String,
    moodTag: String,
    isFeatured: Boolean = false
): ShayariEntity = ShayariEntity(
    id = id,
    couplet = couplet,
    poet = poet,
    categoryId = categoryId,
    moodTag = moodTag,
    isFeatured = isFeatured
)
