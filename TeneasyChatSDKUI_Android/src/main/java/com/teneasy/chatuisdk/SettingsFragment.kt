package com.teneasy.chatuisdk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.luck.picture.lib.utils.ToastUtils
import com.teneasy.chatuisdk.databinding.FragmentSettingsBinding
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_CERT
import com.teneasy.chatuisdk.ui.base.PARAM_IMAGEBASEURL
import com.teneasy.chatuisdk.ui.base.PARAM_LINES
import com.teneasy.chatuisdk.ui.base.PARAM_MAXSESSIONMINS
import com.teneasy.chatuisdk.ui.base.PARAM_MERCHANT_ID
import com.teneasy.chatuisdk.ui.base.PARAM_USERNAME
import com.teneasy.chatuisdk.ui.base.PARAM_USER_ID
import com.teneasy.chatuisdk.ui.base.PARAM_USER_LEVEL
import com.teneasy.chatuisdk.ui.base.PARAM_USER_TYPE
import com.teneasy.chatuisdk.ui.base.PARAM_XTOKEN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.chatuisdk.ui.base.toIntOrZero

/**
 * 设置页面Fragment
 * 用于配置聊天SDK的各项参数
 */
class SettingsFragment : Fragment() {
    // 视图绑定对象
    var binding: FragmentSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置返回键处理
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
        // 读取已保存的配置
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化视图绑定
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding?.apply {
            // 将已保存的配置填充到输入框中
            this.etLine?.setText(Constants.lines)           // 服务器线路
            this.etWssCert?.setText(Constants.cert)        // 访问证书
            // 商户ID（如果大于0才显示）
            if (Constants.merchantId > 0) {
                this.etMerchanId?.setText(Constants.merchantId.toString())
            }

            // 用户ID（如果大于0才显示）
            if (Constants.userId > 0) {
                this.etUserId?.setText(Constants.userId.toString())
            }

            this.etUserName?.setText(Constants.userName)           // 用户名
            this.etBaseImgUrl?.setText(Constants.baseUrlImage)    // 图片服务器地址
            this.etMaxSessionMins?.setText(Constants.maxSessionMins.toString())  // 最大会话时长
            this.etUserLevel?.setText(Constants.userLevel.toString())           // 用户等级

            // 设置用户类型下拉菜单
            val userTypeOptions = arrayOf("官方会员", "邀请好友", "合营会员")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, userTypeOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.spinnerUserType?.adapter = adapter
            
            // 设置当前选中的用户类型 (userType值为1,2,3，数组索引为0,1,2)
            when (Constants.userType) {
                1 -> this.spinnerUserType?.setSelection(0) // 官方会员
                2 -> this.spinnerUserType?.setSelection(1) // 邀请好友
                3 -> this.spinnerUserType?.setSelection(2) // 合营会员
                else -> this.spinnerUserType?.setSelection(1) // 默认选择邀请好友
            }

            // 保存按钮点击事件
            this.btnSave.setOnClickListener {
                // 保存各项配置到Constants
                Constants.lines = this.etLine.text.toString().trim()
                // 配置更改后，清除Token
                Constants.xToken = ""
                Constants.cert = this.etWssCert.text.toString().trim()
                Constants.merchantId = this.etMerchanId.text.toString().trim().toIntOrZero()
                Constants.userId = this.etUserId.text.toString().trim().toIntOrZero()
                Constants.baseUrlImage = this.etBaseImgUrl.text.toString().trim()
                Constants.userName = this.etUserName.text.toString().trim()
                Constants.maxSessionMins = this.etMaxSessionMins.text.toString().trim().toIntOrZero()
                Constants.userLevel = this.etUserLevel.text.toString().trim().toIntOrZero()
                
                // 保存用户类型 (数组索引0,1,2对应userType值1,2,3)
                Constants.userType = when (this.spinnerUserType.selectedItemPosition) {
                    0 -> 1 // 官方会员
                    1 -> 2 // 邀请好友
                    2 -> 3 // 合营会员
                    else -> 2 // 默认邀请好友
                }

                // 将配置保存到SharedPreferences
                UserPreferences().putString(PARAM_CERT, Constants.cert)
                UserPreferences().putInt(PARAM_USER_ID, Constants.userId)
                UserPreferences().putInt(PARAM_MERCHANT_ID, Constants.merchantId)
                UserPreferences().putString(PARAM_LINES, Constants.lines)
                UserPreferences().putString(PARAM_XTOKEN, Constants.xToken)
                UserPreferences().putString(PARAM_IMAGEBASEURL, Constants.baseUrlImage)
                UserPreferences().putString(PARAM_USERNAME, Constants.userName)
                UserPreferences().putInt(PARAM_MAXSESSIONMINS, Constants.maxSessionMins)
                UserPreferences().putInt(PARAM_USER_LEVEL, Constants.userLevel)
                UserPreferences().putInt(PARAM_USER_TYPE, Constants.userType)

                // 显示保存成功提示
                ToastUtils.showToast(requireContext(), "保存成功")
            }

            // 设置根视图的触摸事件，用于关闭软键盘
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
