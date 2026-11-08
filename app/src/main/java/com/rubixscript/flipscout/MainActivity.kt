package com.rubixscript.flipscout

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rubixscript.flipscout.ui.FlipApp
import com.rubixscript.flipscout.ui.home.HomeViewModel
import com.rubixscript.flipscout.ui.search.SearchViewModel
import com.rubixscript.flipscout.ui.sellers.SellersViewModel
import com.rubixscript.flipscout.ui.theme.FlipTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory(applicationContext) }
    private val sellersViewModel: SellersViewModel by viewModels { SellersViewModel.factory(applicationContext) }
    private val searchViewModel: SearchViewModel by viewModels { SearchViewModel.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val shared = sharedText(intent)
        setContent { FlipTheme { FlipApp(homeViewModel, sellersViewModel, searchViewModel, shared) } }
    }

    private fun sharedText(intent: Intent?): String? =
        intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
}
