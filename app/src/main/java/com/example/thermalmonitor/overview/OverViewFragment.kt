package com.example.thermalmonitor.overview

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import timber.log.Timber
import java.util.Date
import java.util.Locale

//OverViewFragment.kt
class OverViewFragment : Fragment() {

    /**
     * 这个类的目的是将读取的数据保存到设备
     * */

    // view models for battery, thermal and soc data
    private val batteryViewModel: BatteryViewModel by viewModels()
    private val thermalViewModel: ThermalViewModel by viewModels()
    private val socViewModel: SocViewModel by viewModels()

    // 创建 DataProcessToSave类的实例
    private val dataProcessor = DataProcessToSave()

    // a variable to indicate whether it is in recording state, default is false
    private var isRecording = false

    // a variable to represent the timer, default is 0
    private var timer = 0

    // a two-dimensional array to store the data, default is empty
    private var data = arrayOf<Array<String>>()

    // 分别给battery，thermal，soc定义二维数组去存储数据
    private var BatteryDataStoreArray = arrayOf<Array<String>>()
    private var ThermalDataStoreArray = arrayOf<Array<String>>()
    private var SocDataStoreArray = arrayOf<Array<String>>()

    // a boolean array to store the user's choices, default is false
    private var checked = booleanArrayOf(false, false, false)

    // a job to run the coroutine for recording and updating UI
    private lateinit var job: Job


    @RequiresApi(Build.VERSION_CODES.R)
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


        /**
         * 设置开始按钮的点击事件
         * */
        binding.btnStart.setOnClickListener {
            if (!isRecording) { // if not in recording state, start recording and update UI
                isRecording = true
                job = lifecycleScope.launch(Dispatchers.Main) {
                    // Reset timer and timeString to initial values
                    timer = 0
                    val timeString = "00:00:00"
                    binding.tvTimer.text = timeString

                    while (isRecording) {

                        val allDataList = getDataFromLiveData()
                        //同时需要将返回的list类型的数据转为array 方便后续的处理
                        val batteryRow = allDataList[0].toTypedArray()
                        val thermalRow = allDataList[1].toTypedArray()
                        val socRow = allDataList[2].toTypedArray()

                        //val row = getDataFromLiveData() // get a row of data from live data
                        //data += row // add the row to the data array
                        BatteryDataStoreArray += batteryRow
                        ThermalDataStoreArray += thermalRow
                        SocDataStoreArray += socRow

                        timer++ // increase the timer by one second
                        val updatedTimeString =
                            String.format(
                                "%02d:%02d:%02d",
                                timer / 3600,
                                timer / 60,
                                timer % 60
                            ) // format the timer to hh:mm:ss
                        binding.tvTimer.text =
                            updatedTimeString // show the updated timer on the text view
                        Timber.tag("DATA").d(
                            """ 
                            batteryRow: ${batteryRow.contentDeepToString()}
                            thermalRow: ${thermalRow.contentDeepToString()} 
                            socRow: ${socRow.contentDeepToString()}
                            """.trimIndent()  //用 trimIndent() 去掉前置空格
                        )

                        // print the data array for debugging
                        delay(1000) // wait for one second
                    }
                }
            } else { // if in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "正在记录中，请勿重复点击", Toast.LENGTH_SHORT)
                    .show()
            }
        }


        /**
         * 设置中止按钮的点击事件
         * */
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

                        //data = arrayOf() // clear the data array
                        //中止清除各个数组的数据
                        BatteryDataStoreArray = arrayOf()
                        ThermalDataStoreArray = arrayOf()
                        SocDataStoreArray = arrayOf()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else { // if not in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT)
                    .show()
            }
        }


        /**
         * 设置停止按钮的点击事件
         * */
        binding.btnStopAndSave.setOnClickListener {
            if (isRecording) { // if in recording state, stop recording and save data to excel file
                isRecording = false // stop recording state
                job.cancel() // cancel the coroutine

                // get the current time as a string in yyyyMMddHHmm format
                val currentTime =
                    SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())

                /**
                 * 需要将时间戳的字符串转换为 Date 对象，然后再进行格式化。
                 * 可以使用 SimpleDateFormat 将时间戳的字符串解析为 Date 对象，然后再格式化为你想要的格式。
                 * */
                val startTime = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(
                    SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(data[0][0])
                )
                val endTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                val fileName = "TMData-$startTime-$endTime.xlsx"


                val result =
                    saveDataToExcel(fileName) // save data to excel file and get a boolean result, and pass the fileName as a parameter

                if (result) {

                    // 弹窗提示保存路径
                    AlertDialog.Builder(requireContext())
                        .setTitle("保存成功")
                        .setMessage("文件已保存到:/storage/emulated/0/Download/ThermalMonitor/$fileName")
                        .setPositiveButton("打开文件夹") { _, _ ->

                            /**
                             * 打开保存文件的目录
                             * 目前还是有些问题
                             * 后续再继续优化
                             * */

                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            val uri =
                                Uri.parse(Environment.getExternalStorageDirectory().path + "/Download/ThermalMonitor/")
                            intent.setDataAndType(uri, "*/*")
                            startActivity(Intent.createChooser(intent, "Open folder"))

                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                else
                {
                    Toast.makeText(requireContext(), "保存失败,请重试", Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                // if not in recording state, toast a message to remind the user
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT)
                    .show()
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

            // 如果电池的checkBox是Checked的状态，则对数据进行处理
            if (checked[0])
            {
                dataProcessor.processBatteryData(workbook, BatteryDataStoreArray)
            }

            // 如果温度的checkBox是checked的状态，则对数据进行处理
            if (checked[1])
            {
                dataProcessor.processThermalData(workbook,ThermalDataStoreArray)
            }


            // 如果芯片的checkBox是checked的状态，则对数据进行处理
            if (checked[2]) { // if soc is checked, create a sheet for soc data and write data to it

                dataProcessor.processSocData(workbook,SocDataStoreArray)
            }


            // 指定文件的MIME类型

            val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"


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
            val uri = requireContext().contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

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

            true  // 文件保存成功后返回 true

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                "SAVE_DATA",
                "保存数据失败，原因：${e.message}"
            ) // print the exception message to the logcat
            false // return false if any exception occurs
        }


    }


    /**
     * A function to get data from live data and return a one-dimensional array.
     * 获取listData的数据，并最终返回一个包含所有可变列表的大列表
     */

    private fun getDataFromLiveData(): List<List<String>> {
        val result = mutableListOf<String>() // create a mutable list to store the result

        // 分别创建三个数据的可变列表
        val batteryDataMutableList = mutableListOf<String>()
        val thermalDataMutableList = mutableListOf<String>()
        val socDataMutableList = mutableListOf<String>()

        // get the current time as a string in HHmmss format
        val timeString = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())


        // add the time string to the result list
        result.add(timeString)

        /**
         * 根据checkBox的状态，来保存数据到可变列表
         * */

        if (checked[0]) { // if battery is checked, get data from batteryData and add to the result list
            val battery = batteryViewModel.batteryData.value
            if (battery != null) { // check if battery is not null
                batteryDataMutableList.add(timeString) // 首个添加时间戳
                batteryDataMutableList.add(battery.level.toString())
                batteryDataMutableList.add(battery.status)
                batteryDataMutableList.add(battery.current.toString())
                batteryDataMutableList.add(battery.temperature.toString())
                batteryDataMutableList.add(battery.voltage.toString())
                batteryDataMutableList.add(battery.source)
            }
        }

        if (checked[1]) { // if thermal is checked, get data from thermalList and add to the result list
            val thermal = thermalViewModel.thermalList.value
            if (thermal != null) { // check if thermal is not null
                thermalDataMutableList.add(timeString) // 首个添加时间戳
                for (t in thermal) {

                    thermalDataMutableList.add(t.temp)
                }
            }
        }

        if (checked[2]) { // if soc is checked, get data from dynamicInfo and add to the result list
            val soc = socViewModel.dynamicInfo.value
            if (soc != null) { // check if soc is not null
                socDataMutableList.add(timeString) // 首个添加时间戳
                for (s in soc) {
                    result.add(s.coreFrequency.toString())
                }
            }
        }

        // 最终返回一个包含所有列表的大列表
        return listOf(
            batteryDataMutableList,
            thermalDataMutableList,
            socDataMutableList
        )
        //return result.toTypedArray() // convert the result list to a typed array and return it
    }


    /**
     * A helper function to convert a boolean value to an int value, 0 for false and 1 for true.
     */
    private fun Boolean.toInt() = if (this) 1 else 0

}






