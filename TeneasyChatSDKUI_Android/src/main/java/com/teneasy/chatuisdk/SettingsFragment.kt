package com.teneasy.chatuisdk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.databinding.FragmentSettingsBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_CERT
import com.teneasy.chatuisdk.ui.base.PARAM_IMAGEBASEURL
import com.teneasy.chatuisdk.ui.base.PARAM_LINES
import com.teneasy.chatuisdk.ui.base.PARAM_MERCHANT_ID
import com.teneasy.chatuisdk.ui.base.PARAM_USERNAME
import com.teneasy.chatuisdk.ui.base.PARAM_USER_ID
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.base.toIntOrZero

class SettingsFragment : Fragment() {
var binding: FragmentSettingsBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding?.apply {
            this.etLine?.setText(Constants.lines)
            this.etWssCert?.setText(Constants.cert)

            if (Constants.merchantId > 0) {
                this.etMerchanId?.setText(Constants.merchantId.toString())
            }

            if (Constants.userId > 0) {
                this.etUserId?.setText(Constants.userId.toString())
            }

            this.etUserName?.setText(Constants.userName)

            this.etBaseImgUrl?.setText(Constants.baseUrlImage)
            this.btnSave.setOnClickListener {
                Constants.lines = this.etLine.text.toString().trim()
                //配置更改后，要清除Token
                Constants.xToken = ""
                Constants.cert = this.etWssCert.text.toString().trim()
                Constants.merchantId =  this.etMerchanId.text.toString().trim().toIntOrZero()
                Constants.userId =  this.etUserId.text.toString().trim().toIntOrZero()
                Constants.baseUrlImage = this.etBaseImgUrl.text.toString().trim()
                Constants.userName = this.etUserName.text.toString().trim()

                UserPreferences().putString(PARAM_CERT, Constants.cert)
                UserPreferences().putInt(PARAM_USER_ID, Constants.userId)
                UserPreferences().putInt(PARAM_MERCHANT_ID, Constants.merchantId)
                UserPreferences().putString(PARAM_LINES, Constants.lines)
                UserPreferences().putString(PARAM_XTOKEN, Constants.xToken)
                UserPreferences().putString(PARAM_IMAGEBASEURL, Constants.baseUrlImage)
                UserPreferences().putString(PARAM_USERNAME, Constants.userName)
                ToastUtils.showToast(requireContext(), "保存成功")
            }

            this.root.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                       Utils().closeSoftKeyboard(v)
                    }
                    true
            }
        }

        return binding?.root
    }
}