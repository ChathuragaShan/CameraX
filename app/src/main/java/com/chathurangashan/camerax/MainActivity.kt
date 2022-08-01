package com.chathurangashan.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.chathurangashan.camerax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navigationController : NavController
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initialization()
    }

    private fun initialization() {

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navigationController = navHostFragment.navController
    }

    override fun onSupportNavigateUp(): Boolean {

        if(navigationController.currentDestination?.id == R.id.selfie_capture_fragment){
            moveTaskToBack(true)
        }else{
            navigationController.navigateUp()
        }

        return true
    }

    override fun onBackPressed() {
        onSupportNavigateUp()
    }
}