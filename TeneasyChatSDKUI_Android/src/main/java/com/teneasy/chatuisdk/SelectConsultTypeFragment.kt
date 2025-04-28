package com.teneasy.chatuisdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
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
import com.teneasy.chatuisdk.ui.http.bean.ErrorReport
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK

/**
 * 咨询类型选择页面的Fragment
 * 用于显示不同类型的客服咨询选项
 */
class SelectConsultTypeFragment : Fragment(){
    // ViewModel实例，用于处理业务逻辑
    private val viewModel: SelectConsultTypeViewModel by viewModels()
    // RecyclerView用于显示咨询类型列表
    private lateinit var recyclerView: RecyclerView
    // 咨询类型列表适配器
    private lateinit var adapter: SelectConsultTypeAdapter
    // 视图绑定对象
    private var binding: FragmentSelectConsultTypeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取配置信息
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化视图绑定
        binding = FragmentSelectConsultTypeBinding.inflate(inflater, container, false)
        binding?.apply {
            // 初始化RecyclerView和适配器
            adapter = SelectConsultTypeAdapter(ArrayList())
            recyclerView = this.rvList
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // 观察咨询类型列表数据变化
            viewModel.consultList.observe(viewLifecycleOwner) {
                if (!it.isEmpty()) {
                    adapter.updateData(it)
                    this.tvEmpty.visibility = View.GONE
                }else{
                    this.tvEmpty.text = "暂无数据"
                }
            }

            // 设置按钮点击事件
            this.ivSettings.setOnClickListener {
                it.findNavController().navigate(R.id.frg_settings)
            }

            // 返回按钮点击事件
            this.ivBack.setOnClickListener {
                requireActivity().finish()
            }
        }
        return binding?.root
    }

    override fun onResume() {
        super.onResume()

        // 检查必要参数是否已配置
        if (Constants.lines.isEmpty() || Constants.cert.isEmpty() || Constants.baseUrlImage.isEmpty() || Constants.merchantId == 0 || Constants.userId == 0){
            binding?.tvLine?.text = "* 请在设置页面设置好参数 *";
            binding?.tvEmpty?.text = ""
            return
        }

        // 初始化线路检测库
        // 检测线路地址，以逗号分开；放在onResume来确保每次到这个页面都会检测一次
        val lineLib = LineDetectLib(Constants.lines,  object :
            LineDetectDelegate {
            // 线路检测成功回调
            override fun useTheLine(line: String) {
                Constants.domain = line
                UserPreferences().putString(line, PARAM_DOMAIN)
                // 设置网络请求的全局基础地址
                XHttpSDK.setBaseUrl(Constants.baseUrlApi())
                activity?.runOnUiThread {
                    binding?.tvLine?.text = "当前线路：" + line
                    // 获取线路之后，获取咨询类型列表
                    viewModel.queryEntrance()
                }
            }

            // 线路检测失败回调
            override fun lineError(error: Result) {
                println(error.msg)
                activity?.runOnUiThread {
                    binding?.tvLine?.text = "无可用线路"
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
        }, Constants.merchantId) // merchantId为商户号
        
        // 开始检测线路
        lineLib.getLine()
    }
}
