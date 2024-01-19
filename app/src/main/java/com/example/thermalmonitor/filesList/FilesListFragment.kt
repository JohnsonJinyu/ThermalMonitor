package com.example.thermalmonitor.filesList

import android.annotation.SuppressLint
import android.database.ContentObserver
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.FragmentFileslistBinding
import java.io.File

/**
 *这个Fragment 是显示本地已经保存的数据的Excel文件，并支持直接跳转到用其他应用打开
 * */
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


        // 注册内容观察者
        requireContext().contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true,
            observer
        )
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
    private val observer = object : ContentObserver(null) {

        override fun onChange(selfChange: Boolean) {
            refreshFileList()
        }

    }


    // Stop the FileObserver in onDestroyView()
    override fun onDestroyView() {
        // 取消注册内容观察者
        requireContext().contentResolver.unregisterContentObserver(observer)
        super.onDestroyView()
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
        // 在UI线程中更新数据
        activity?.runOnUiThread {
            val newFileList = getFileList()
            if (::fileAdapter.isInitialized) {
                fileAdapter.updateFilesList(newFileList)
                fileAdapter.notifyDataSetChanged()
            } else {
                fileAdapter = FileAdapter(requireContext(), newFileList)
                recycler.adapter = fileAdapter


            }
        }


    }


}