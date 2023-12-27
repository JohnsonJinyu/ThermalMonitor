package com.example.thermalmonitor.filesList

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemFileBinding
import java.io.File

class FileAdapter(
    private val context: Context,
    private val filesList: List<ExcelFile>
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
        //return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_file),parent,false)
    }

    override fun getItemCount(): Int {
        return filesList.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = filesList[position]
        holder.binding.apply {
            tvFileName.text = file.fileName
            tvFileSize.text = file.fileSize.toString()
            tvFileModificationTime.text = file.fileMTTime.toString()
        }
    }

}