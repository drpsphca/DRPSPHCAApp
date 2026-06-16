package com.drpsphca.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.drpsphca.app.PostItem
import com.drpsphca.app.ui.theme.DRPSPHCATheme
import com.drpsphca.app.ui.viewmodel.PostItemUiModel

@Preview(showBackground = true)
@Composable
fun PostItemPreview() {
    DRPSPHCATheme {
        PostItem(
            post = PostItemUiModel(
                id = 1,
                formattedDate = "2025-02-27",
                plainTitle = "DRPS PHCA Blog Post Preview",
                plainExcerpt = "This is a sample excerpt to demonstrate how the post item looks in the feed.",
                imageUrl = null,
                tags = listOf("News", "Updates")
            ),
            navController = rememberNavController()
        )
    }
}
