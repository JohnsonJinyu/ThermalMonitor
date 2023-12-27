package com.example.thermalmonitor.filesList

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.FragmentFileslistBinding
import java.io.File


class FilesListFragment : Fragment() {

    // 创建视图绑定
    private lateinit var binding: FragmentFileslistBinding
    private lateinit var fileAdapter: FileAdapter

    // 获取recyclerView的引用
    private val recycler by lazy { binding.recyclerViewFiles }

    private lateinit var fileList: List<ExcelFile>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFileslistBinding.inflate(inflater, container, false)
        return binding.root
    }


    // 在视图创建完成后，获取本地目录文件，并更新到视图UI中
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        fileList = getFileList()
        fileAdapter = FileAdapter(requireContext(), fileList)
        recycler.adapter = fileAdapter

        recycler.layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)
        //recycler.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
        recycler.setHasFixedSize(true)


        fileObserver.startWatching()
        // 手动调用适配器的 notifyDataSetChanged 方法来刷新视图
        fileAdapter.notifyDataSetChanged()
    }


    private fun getFileList(): List<ExcelFile> {
        val files = getRawFileList() ?: return emptyList()

        return files.map { file ->
            ExcelFile(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                fileMTTime = file.lastModified()
            )
        }.sortedByDescending { it.fileMTTime } // sort by file modification time in descending order
        // 按照文件修改时间从降序排序
    }


    private fun getRawFileList(): Array<File>? {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ThermalMonitor"
        )
        return folder.listFiles()
    }



    // Define a FileObserver to monitor the folder changes
    val folder = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "ThermalMonitor"
    )
    private val fileObserver = object : FileObserver(folder.path) {
        @SuppressLint("NotifyDataSetChanged")
        override fun onEvent(event: Int, path: String?) {
            // If a file is created, modified, or deleted, update the fileList and notify the adapter
            if (event == CREATE || event == MODIFY || event == DELETE) {
                activity?.runOnUiThread { //使用了 activity?.runOnUiThread 方法 确保文件列表和adapter的更新操作在主线程中进行
                    fileList = getFileList()
                    fileAdapter.notifyDataSetChanged()
                }
            }
        }
    }





    // Stop the FileObserver in onDestroyView()
    override fun onDestroyView() {
        super.onDestroyView()
        fileObserver.stopWatching()
    }


    /**
     * 重新进入此Fragment需要刷新文件列表
     * */

    override fun onResume() {
        super.onResume()
        refreshFileList()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshFileList() {
        fileList = getFileList()
        fileAdapter = FileAdapter(requireContext(), fileList)
        recycler.adapter = fileAdapter
        fileAdapter.notifyDataSetChanged()
    }

}