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
import com.teneasy.chatuisdk.ui.base.PARAM_LINES
import com.teneasy.chatuisdk.ui.base.PARAM_MERCHANT_ID
import com.teneasy.chatuisdk.ui.base.PARAM_USER_ID
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.base.toIntOrZero

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
var binding: FragmentSettingsBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_settings, container, false)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

//        binding?.tvBack?.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
        binding?.apply {
            this.etLine?.setText(Constants.lines)
            this.etXToken?.setText(Constants.xToken)
            this.etWssCert?.setText(Constants.cert)
            this.etMerchanId?.setText(Constants.merchantId.toString())
            this.etUserId?.setText(Constants.userId.toString())

            this.btnSave.setOnClickListener {
                Constants.lines = this.etLine.text.toString()
                Constants.xToken = ""
                Constants.cert = this.etWssCert.text.toString()

                Constants.merchantId =  this.etMerchanId.text.toString().toIntOrZero()
                Constants.userId =  this.etUserId.text.toString().toIntOrZero()

                UserPreferences().putString(PARAM_CERT, Constants.cert)
                UserPreferences().putInt(PARAM_USER_ID, Constants.userId)
                UserPreferences().putInt(PARAM_MERCHANT_ID, Constants.merchantId)
                UserPreferences().putString(PARAM_LINES, Constants.lines)
                UserPreferences().putString(PARAM_XTOKEN, Constants.xToken)

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

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}