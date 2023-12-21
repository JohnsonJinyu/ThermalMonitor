package com.example.thermalmonitor.filesList

data class ExcelFile(
    val fileName: String, // file name
    val fileMTTime: Long, // file modification time
    val fileSize: Long, // file size
    val filePath: String
)