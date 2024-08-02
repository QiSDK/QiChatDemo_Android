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
import com.teneasy.sdk.LineDetectDelegate
import com.teneasy.sdk.LineDetectLib
import com.teneasy.sdk.Result
import com.xuexiang.xhttp2.XHttpSDK


class SelectConsultTypeFragment : Fragment(){
    private val viewModel: SelectConsultTypeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SelectConsultTypeAdapter
    private var binding: FragmentSelectConsultTypeBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //读取从设置页面设置的值
        Utils().readConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectConsultTypeBinding.inflate(inflater, container, false)
        binding?.apply {
            adapter = SelectConsultTypeAdapter(ArrayList())
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

            this.ivBack.setOnClickListener {
               requireActivity().finish()
            }
        }
        return  binding?.root
    }

    override fun onResume() {
        super.onResume()

        if (Constants.lines.isEmpty() || Constants.cert.isEmpty() || Constants.baseUrlImage.isEmpty() || Constants.merchantId == 0 || Constants.userId == 0){
            binding?.tvLine?.text = "* 请在设置页面设置好参数 *";
            binding?.tvEmpty?.text = ""
            return
        }

        //检测线路地址，以逗号分开；放在onResume来确保每次到这个页面都会检测一次
        //初始化检测库，商户号必须正确，不然会导致线路检测失败
        val lineLib = LineDetectLib(Constants.lines,  object :
            LineDetectDelegate {
            override fun useTheLine(line: String) {
                Constants.domain = line
                UserPreferences().putString(line, PARAM_DOMAIN)
                //设置网络请求的全局基础地址
                XHttpSDK.setBaseUrl(Constants.baseUrlApi())
                activity?.runOnUiThread {
                    binding?.tvLine?.text = "当前线路：" + line
                    //获取线路之后，获取咨询类型列表
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
        //调用检测函数
        lineLib.getLine()
    }
}