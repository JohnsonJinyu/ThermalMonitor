package com.example.thermalmonitor.overview

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

//OverViewFragment.kt
class OverViewFragment : Fragment() {







    // view models for battery, thermal and soc data
    private val batteryViewModel: BatteryViewModel by viewModels()
    private val thermalViewModel: ThermalViewModel by viewModels()
    private val socViewModel: SocViewModel by viewModels()

    // a variable to indicate whether it is in recording state, default is false
    private var isRecording = false

    // a variable to represent the timer, default is 0
    private var timer = 0

    // a two-dimensional array to store the data, default is empty
    private var data = arrayOf<Array<String>>()

    // a boolean array to store the user's choices, default is false
    private var checked = booleanArrayOf(false, false, false)

    // a job to run the coroutine for recording and updating UI
    private lateinit var job: Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {




        // Inflate the layout for this fragment
        val binding = FragmentOverviewBinding.inflate(inflater)
        binding.lifecycleOwner = this



        // set the view models for the binding
        binding.batteryViewModel = batteryViewModel
        binding.thermalViewModel = thermalViewModel
        binding.socViewModel = socViewModel

        // set the onCheckedChangeListeners for the checkboxes
        binding.cbBattery.setOnCheckedChangeListener { _, isChecked ->
            checked[0] = isChecked
        }
        binding.cbThermal.setOnCheckedChangeListener { _, isChecked ->
            checked[1] = isChecked
        }
        binding.cbSoc.setOnCheckedChangeListener { _, isChecked ->
            checked[2] = isChecked
        }

        // set the onClickListener for the start button
        binding.btnStart.setOnClickListener {
            if (!isRecording) { // if not in recording state, start recording and update UI
                isRecording = true
                job = lifecycleScope.launch(Dispatchers.Main) {
                    while (isRecording) {
                        val row = getDataFromLiveData() // get a row of data from live data
                        data += row // add the row to the data array
                        timer++ // increase the timer by one second
                        val timeString =
                            String.format("%02d:%02d:%02d", timer / 3600, timer / 60, timer % 60) // format the timer to hh:mm:ss
                        binding.tvTimer.text = timeString // show the timer on the text view
                        Log.d("DATA", data.contentDeepToString()) // print the data array for debugging
                        delay(1000) // wait for one second
                    }
                }
            } else { // if in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "正在记录中，请勿重复点击", Toast.LENGTH_SHORT).show()
            }
        }


        // set the onClickListener for the abort button
        binding.btnAbort.setOnClickListener {
            if (isRecording) { // if in recording state, show a dialog to confirm with the user
                AlertDialog.Builder(requireContext())
                    .setTitle("中止记录")
                    .setMessage("确定要中止记录吗？已经记录的数据将不会被保存。")
                    .setPositiveButton("确定") { _, _ ->
                        isRecording = false // stop recording state
                        job.cancel() // cancel the coroutine
                        timer = 0 // reset the timer to zero
                        binding.tvTimer.text = "00:00:00" // reset the text view to zero
                        data = arrayOf() // clear the data array
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else { // if not in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT).show()
            }
        }



        // set the onClickListener for the stop and save button
        binding.btnStopAndSave.setOnClickListener {
            if (isRecording) { // if in recording state, stop recording and save data to excel file
                isRecording = false // stop recording state
                job.cancel() // cancel the coroutine

                // get the current time as a string in yyyyMMddHHmm format
                val currentTime = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())


                // generate a file name based on the start and end time
                val fileName = "TMData-${data[0][0]}-$currentTime.xlsx"


                val result = saveDataToExcel(fileName) // save data to excel file and get a boolean result, and pass the fileName as a parameter

                if (result) {

                    // 弹窗提示保存路径
                    AlertDialog.Builder(requireContext())
                        .setTitle("保存成功")
                        .setMessage("文件已保存到:/storage/emulated/0/ThermalMonitor/files/$fileName")
                        .setPositiveButton("打开文件夹") { _, _ ->

                            // 打开文件夹
                            val folder = File(requireContext().getExternalFilesDir(null), "ThermalMonitor")
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.fromFile(folder), "*/*")
                            startActivity(intent)

                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()

                } else {

                    Toast.makeText(requireContext(), "保存失败,请重试", Toast.LENGTH_SHORT).show()

                }
            } else { // if not in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root

    }


    /**
     * A function to save data to excel file and return a boolean result.
     * @param fileName the file name to save the data
     */
    private fun saveDataToExcel(fileName: String): Boolean {

        return try {
            val workbook = XSSFWorkbook() // create a workbook object

            if (checked[0]) { // if battery is checked, create a sheet for battery data and write data to it

                val sheetBattery = workbook.createSheet("TMData-Battery") // create a sheet for battery data

                val titleRowBattery = sheetBattery.createRow(0) // create a title row for battery data
                val titleArrayBattery = arrayOf("时间戳", "level", "status", "current", "temperature", "voltage", "source") // create a title array for battery data
                for (i in titleArrayBattery.indices) { // loop through the title array and write each title to the title row
                    val cell = titleRowBattery.createCell(i)
                    cell.setCellValue(titleArrayBattery[i])
                }

                for (i in 1 .. data.size) { // loop through the data array and write each row of battery data to the sheet
                    val row = sheetBattery.createRow(i+1)
                    for (j in 0..6) {
                        val cell = row.createCell(j)
                        cell.setCellValue(data[i - 1][j])
                    }
                }


            }

            if (checked[1]) { // if thermal is checked, create a sheet for thermal data and write data to it

                val sheetThermal = workbook.createSheet("TMData-Thermal") // create a sheet for thermal data

                val titleRowThermal = sheetThermal.createRow(0) // create a title row for thermal data
                val titleArrayThermal = arrayOf("时间戳") + thermalViewModel.thermalList.value!!.map { it.type } // create a title array for thermal data by adding the timestamp and the types of thermal zones
                for (i in titleArrayThermal.indices) { // loop through the title array and write each title to the title row
                    val cell = titleRowThermal.createCell(i)
                    cell.setCellValue(titleArrayThermal[i])
                }
                for (i in 1..data.size) { // loop through the data array and write each row of thermal data to the sheet
                    val row = sheetThermal.createRow(i)
                    val cell = row.createCell(0)
                    cell.setCellValue(data[i - 1][0]) // write the timestamp to the first cell
                    for (j in 1..titleArrayThermal.size) {
                        val cell = row.createCell(j)
                        cell.setCellValue(data[i - 1][j + 6]) // write the temperatures to the rest cells, skipping the first 6 columns of battery data
                    }
                }
            }


            if (checked[2]) { // if soc is checked, create a sheet for soc data and write data to it

                val sheetSoc = workbook.createSheet("TMData-Soc") // create a sheet for soc data

                val titleRowSoc = sheetSoc.createRow(0) // create a title row for soc data
                val titleArraySoc = arrayOf("时间戳") + socViewModel.dynamicInfo.value!!.map { "number${it.coreNumber}" } // create a title array for soc data by adding the timestamp and the numbers of cores
                for (i in titleArraySoc.indices) { // loop through the title array and write each title to the title row
                    val cell = titleRowSoc.createCell(i)
                    cell.setCellValue(titleArraySoc[i])
                }
                for (i in 1..data.size) { // loop through the data array and write each row of soc data to the sheet
                    val row = sheetSoc.createRow(i)
                    val cell = row.createCell(0)
                    cell.setCellValue(data[i - 1][0]) // write the timestamp to the first cell
                    for (j in 1..titleArraySoc.size) {
                        val cell = row.createCell(j)
                        cell.setCellValue(data[i - 1][j + 6 + checked[1].toInt() * thermalViewModel.thermalList.value!!.size]) // write the frequencies to the rest cells, skipping the first 6 columns of battery data and the columns of thermal data if checked
                    }
                }
            }


            // 获取文件的 MIME 类型，这里假设是 Excel 文件
            val mimeType = "application/vnd.ms-excel"

            // 获取文件的显示名称，这里假设是 fileName
            val displayName = fileName

            // 获取文件的相对路径，这里指定为 files/ThermalMonitor 目录
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ThermalMonitor"

            // 创建一个 ContentValues 对象，用于设置文件的属性
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1) // 设置为待处理状态，防止其他应用访问
            }

            // 通过调用 requireContext() 方法获取到对应的 Context，然后使用它的 contentResolver 属性
            // 通过 contentResolver 向 MediaStore 插入一条新记录，返回一个 Uri 对象
            val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            // 通过 contentResolver 打开一个输出流，写入文件内容
            if (uri != null) {
                requireContext().contentResolver.openOutputStream(uri).use { os ->
                    workbook.write(os) // 将 workbook 写入输出流
                }
            }

            // 更新 ContentValues 对象，将文件状态设置为可用
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)

            // 通过 contentResolver 更新 MediaStore 中的记录
            if (uri != null) {
                requireContext().contentResolver.update(uri, values, null, null)
            }

            // 关闭 workbook
            workbook.close()


            saveDataToExcel(fileName) // return true if no exception occurs, and pass the fileName as a parameter
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SAVE_DATA", "保存数据失败，原因：${e.message}") // print the exception message to the logcat
            false // return false if any exception occurs
        }



    }


    /**
     * A function to get data from live data and return a one-dimensional array.
     */

    private fun getDataFromLiveData(): Array<String> {
        val result = mutableListOf<String>() // create a mutable list to store the result

        // get the current time as a string in HHmmss format
        val timeString = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())

        // add the time string to the result list
        result.add(timeString)

        if (checked[0]) { // if battery is checked, get data from batteryData and add to the result list
            val battery = batteryViewModel.batteryData.value
            if (battery != null) { // check if battery is not null
                result.add(battery.level.toString())
                result.add(battery.status)
                result.add(battery.current.toString())
                result.add(battery.temperature.toString())
                result.add(battery.voltage.toString())
                result.add(battery.source)
            }
        }

        if (checked[1]) { // if thermal is checked, get data from thermalList and add to the result list
            val thermal = thermalViewModel.thermalList.value
            if (thermal != null) { // check if thermal is not null
                for (t in thermal) {
                    result.add(t.temp)
                }
            }
        }

        if (checked[2]) { // if soc is checked, get data from dynamicInfo and add to the result list
            val soc = socViewModel.dynamicInfo.value
            if (soc != null) { // check if soc is not null
                for (s in soc) {
                    result.add(s.coreFrequency.toString())
                }
            }
        }

        return result.toTypedArray() // convert the result list to a typed array and return it
    }


    /**
     * A helper function to convert a boolean value to an int value, 0 for false and 1 for true.
     */
    private fun Boolean.toInt() = if (this) 1 else 0

}






