package com.teneasy.chatuisdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.databinding.FragmentSelectConsultTypeBinding
import com.teneasy.chatuisdk.ui.SelectConsultTypeAdapter
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK

/**
 * 咨询类型选择页面
 * 用于展示可用的咨询类型列表，并处理线路检测等功能
 */
class SelectConsultTypeFragment : Fragment() {
    companion object {
        private const val TAG = "SelectConsultTypeFragment"
    }

    // ViewModel实例
    private val viewModel: SelectConsultTypeViewModel by viewModels()
    
    // 视图绑定相关
    private var _binding: FragmentSelectConsultTypeBinding? = null
    private val binding get() = _binding!!
    
    // RecyclerView相关
    private lateinit var consultTypeAdapter: SelectConsultTypeAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取配置信息
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectConsultTypeBinding.inflate(inflater, container, false)
        initViews()
        setupObservers()
        return binding.root
    }

    /**
     * 初始化视图组件
     */
    private fun initViews() {
        with(binding) {
            // 初始化RecyclerView
            consultTypeAdapter = SelectConsultTypeAdapter(ArrayList())
            recyclerView = rvList.apply {
                adapter = consultTypeAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            // 设置按钮点击事件
            ivSettings.setOnClickListener {
                it.findNavController().navigate(R.id.frg_settings)
            }

            // 返回按钮点击事件
            ivBack.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    /**
     * 设置观察者，监听数据变化
     */
    private fun setupObservers() {
        viewModel.consultList.observe(viewLifecycleOwner) { consultList ->
            with(binding) {
                if (consultList.isNotEmpty()) {
                    consultTypeAdapter.updateData(consultList)
                    tvEmpty.visibility = View.GONE
                } else {
                    tvEmpty.text = "暂无数据"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkConfiguration()
    }

    /**
     * 检查配置是否完整
     * @return 配置是否有效
     */
    private fun checkConfiguration(): Boolean {
        with(Constants) {
            if (lines.isEmpty() || cert.isEmpty() || baseUrlImage.isEmpty() || 
                merchantId == 0 || userId == 0) {
                binding.apply {
                    tvLine.text = "* 请在设置页面设置好参数 *"
                    tvEmpty.text = ""
                }
                return false
            }
        }
        initLineDetection()
        return true
    }

    /**
     * 初始化线路检测
     * 检测可用的服务器线路
     */
    private fun initLineDetection() {
        val lineLib = LineDetectLib(
            Constants.lines,
            object : LineDetectDelegate {
                override fun useTheLine(line: String) {
                    handleValidLine(line)
                }

                override fun lineError(error: Result) {
                    handleLineError(error)
                }
            },
            Constants.merchantId
        )
        lineLib.getLine()
    }

    /**
     * 处理有效线路
     * @param line 可用的服务器线路
     */
    private fun handleValidLine(line: String) {
        Constants.domain = line
        UserPreferences().putString(line, PARAM_DOMAIN)

        //设置网络请求的全局基础地址
        XHttpSDK.setBaseUrl(Constants.baseUrlApi())
        
        activity?.runOnUiThread {
            binding.tvLine.text = "当前线路：$line"
            viewModel.queryEntrance()
        }
    }

    /**
     * 处理线路错误
     * @param error 错误信息
     */
    private fun handleLineError(error: Result) {
        activity?.runOnUiThread {
            binding.tvLine.text = "无可用线路"
            if (error.code == 1008) {
                viewModel.logError(
                    error.code,
                    Constants.lines,
                    "",
                    error.msg,
                    Constants.lines
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
