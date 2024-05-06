package com.teneasy.chatuisdk

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.databinding.FragmentKefuBinding
import com.teneasy.chatuisdk.databinding.FragmentSelectConsultTypeBinding
import com.teneasy.chatuisdk.ui.MyAdapter
import com.teneasy.chatuisdk.ui.main.BaseBindingFragment

class SelectConsultTypeFragment : BaseBindingFragment<FragmentSelectConsultTypeBinding>() {

    private val viewModel: SelectConsultTypeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding?.let {
            recyclerView = it.rvList
            adapter = MyAdapter(viewModel.consultList.value ?: ArrayList())
            recyclerView.adapter = adapter
            viewModel.consultList.observe(this) {
                adapter.updateData(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_select_consult_type, container, false)
    }

    override fun onCreateViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): FragmentSelectConsultTypeBinding {
        return FragmentSelectConsultTypeBinding.inflate(layoutInflater, parent, false)
    }
}