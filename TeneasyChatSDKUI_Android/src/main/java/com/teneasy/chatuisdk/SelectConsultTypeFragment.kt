package com.teneasy.chatuisdk

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teneasy.chatuisdk.databinding.FragmentSelectConsultTypeBinding
import com.teneasy.chatuisdk.ui.MyAdapter
import com.teneasy.chatuisdk.ui.main.BaseBindingFragment

class SelectConsultTypeFragment : Fragment(){
    private val viewModel: SelectConsultTypeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private var binding: FragmentSelectConsultTypeBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                adapter.updateData(it)
                if (!it.isEmpty()) {
                    this.tvEmpty.visibility = View.GONE
                }
            }

            viewModel.queryEntrance()
        }
        return  binding?.root
    }
}