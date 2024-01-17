package com.example.thermalmonitor.filesList

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemFileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class FileAdapter(
    private val context: Context,
    private var filesList: List<ExcelFile>
) :
    RecyclerView.Adapter<FileAdapter.ViewHolder>() {


    inner class ViewHolder(val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val file = filesList[adapterPosition]
                openFile(file)
            }
        }

        private fun openFile(file: ExcelFile) {
            val intent = Intent(Intent.ACTION_VIEW)
            // Use FileProvider to get content URI
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(file.filePath))
            intent.setDataAndType(uri, "application/vnd.ms-excel")
            // Add flag to grant read permission
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filesList.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = filesList[position]
        holder.binding.apply {
            tvFileName.text = file.fileName
            tvFileSize.text = getReadableFileSize(file.fileSize)
            tvFileModificationTime.text = getReadableDateTime(file.fileMTTime)
        }
    }

    // 转换文件大小为可读格式
    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return "%.1f %s".format(size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    // 转换Unix时间格式为可读格式
    private fun getReadableDateTime(time: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(time))
    }


    fun updateFilesList(newFilesList: List<ExcelFile>) {
        filesList = newFilesList
        notifyDataSetChanged()
    }

}