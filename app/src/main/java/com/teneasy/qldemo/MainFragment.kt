package com.teneasy.qldemo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.teneasy.qldemo.databinding.FragmentMainBinding
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.main.KeFuActivity

class MainFragment : Fragment(){

    companion object {
        fun newInstance() = MainFragment()
    }

    var binding: FragmentMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding?.apply {
            this.btnSend.setOnClickListener {
                val keFuIntent = Intent(requireContext(), KeFuActivity :: class.java)
                startActivity(keFuIntent)
            }

            this.ivSettings.setOnClickListener {
                findNavController().navigate(R.id.frg_settings)
            }

            // Set the version name
            this.tvVersionNumber.text = "Version: ${getAppVersion(requireContext()).first}"
        }

        return binding?.root
    }

    fun getAppVersion(context: Context): Pair<String, Int> {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.versionCode
            return Pair(versionName, versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle the case where the package name is not found (should not happen in a running app)
            e.printStackTrace()
            return Pair("Unknown", -1) // Or throw an exception, depending on your needs
        }
    }
}


