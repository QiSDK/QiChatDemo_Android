package com.teneasy.chatuisdk

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.databinding.FragmentSelectConsultTypeBinding
import com.teneasy.chatuisdk.ui.MyAdapter
import com.teneasy.chatuisdk.ui.base.Constants
import com.teneasy.chatuisdk.ui.base.PARAM_DOMAIN
import com.teneasy.chatuisdk.ui.base.UserPreferences
import com.teneasy.chatuisdk.ui.base.Utils
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK

class SelectConsultTypeFragment : Fragment(){
    private val viewModel: SelectConsultTypeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private var binding: FragmentSelectConsultTypeBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化配置
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectConsultTypeBinding.inflate(inflater, container, false)
        binding?.apply {
            adapter = MyAdapter(ArrayList())
            recyclerView = this.rvList
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            viewModel.consultList.observe(viewLifecycleOwner) {
                if (!it.isEmpty()) {
                    adapter.updateData(it)
                    this.tvEmpty.visibility = View.GONE
                }else{
                    this.tvEmpty.text = "暂无数据"
                }
            }

            this.ivSettings.setOnClickListener {
                it.findNavController().navigate(R.id.frg_settings)
            }
        }
        return  binding?.root
    }

    override fun onResume() {
        super.onResume()

        //binding?.tvLine?.text = "当前线路："
        //binding?.tvEmpty?.text = "连接客服中，请稍后..."

        //检测线路地址，以逗号分开
        val lineLib = LineDetectLib(Constants.lines,  object :
            LineDetectDelegate {
            override fun useTheLine(line: String) {
                Constants.domain = line
                UserPreferences().putString(line, PARAM_DOMAIN)
                //设置网络请求的全局基础地址
                XHttpSDK.setBaseUrl(Constants.baseUrlApi)
                //initChatSDK(line)
                activity?.runOnUiThread {
                    binding?.tvLine?.text = "当前线路：" + line
                    viewModel.queryEntrance()
                }
            }
            override fun lineError(error: Result) {
                println(error.msg)
                activity?.runOnUiThread {
                    binding?.tvLine?.text = "无可用线路"
                }
            }
        }, Constants.merchantId) //123是商户号
        lineLib.getLine()
    }
}