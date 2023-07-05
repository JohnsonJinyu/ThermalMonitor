package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.R
import com.example.thermalmonitor.battery.BatteryData
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.DynamicInfo
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalData
import com.example.thermalmonitor.thermal.ThermalViewModel
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat

class OverViewFragment : Fragment() {

    // 定义一些常量
    companion object {
        const val FILE_PREFIX = "TMData-" // 文件名前缀
        const val FILE_SUFFIX = ".xlsx" // 文件名后缀
        const val SHEET_BATTERY = "TMData-Battery" // 电池数据的sheet名字
        const val SHEET_THERMAL = "TMData-Thermal" // 温度数据的sheet名字
        const val SHEET_SOC = "TMData-Soc" // Soc数据的sheet名字
    }

    // 定义一些变量
    private lateinit var batteryViewModel: BatteryViewModel // 电池数据的view model
    private lateinit var thermalViewModel: ThermalViewModel // 温度数据的view model
    private lateinit var socViewModel: SocViewModel // Soc数据的view model
    private var isRecording = false // 是否正在记录的标志位
    private var startTime = 0L // 记录开始的时间戳，单位毫秒
    private var endTime = 0L // 记录结束的时间戳，单位毫秒
    private var batteryDataList = mutableListOf<BatteryData>() // 用于存储电池数据的列表
    private var thermalDataList = mutableListOf<List<ThermalData>>() // 用于存储温度数据的列表
    private var socDataList = mutableListOf<List<DynamicInfo>>() // 用于存储Soc数据的列表

    private lateinit var textViewTime : TextView // 用于显示记录时长的TextView

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 加载布局文件
        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        // 获取view model的实例
        batteryViewModel = ViewModelProvider(this)[BatteryViewModel::class.java]
        thermalViewModel = ViewModelProvider(this)[ThermalViewModel::class.java]
        socViewModel = ViewModelProvider(this)[SocViewModel::class.java]

        // 获取界面上的控件
        val checkBoxBattery = view.findViewById<CheckBox>(R.id.checkBox_battery)
        val checkBoxThermal = view.findViewById<CheckBox>(R.id.checkBox_thermal)
        val checkBoxSoc = view.findViewById<CheckBox>(R.id.checkBox_soc)
        val buttonStart = view.findViewById<Button>(R.id.button_start)
        val buttonStop = view.findViewById<Button>(R.id.button_stop)
        val buttonSave = view.findViewById<Button>(R.id.button_save)
        textViewTime = view.findViewById(R.id.textView_time)

        // 设置按钮的点击事件
        buttonStart.setOnClickListener {
            if (isRecording) { // 如果已经在记录，就提示用户
                Toast.makeText(requireContext(), "正在记录", Toast.LENGTH_SHORT).show()
            } else { // 如果没有在记录，就开始记录，并设置标志位和开始时间
                isRecording = true
                startTime = System.currentTimeMillis()
                Toast.makeText(requireContext(), "开始记录", Toast.LENGTH_SHORT).show()
            }
        }

        buttonStop.setOnClickListener {
            if (isRecording) { // 如果在记录，就弹出一个对话框，让用户确认是否中止记录
                AlertDialog.Builder(requireContext())
                    .setTitle("中止记录")
                    .setMessage("确定要中止记录吗？这将清空已经记录的数据。")
                    .setPositiveButton("确定") { _, _ ->
                        // 如果用户确定，就停止记录，并重置标志位、时间和数据列表
                        isRecording = false
                        startTime = 0L
                        endTime = 0L
                        batteryDataList.clear()
                        thermalDataList.clear()
                        socDataList.clear()
                        textViewTime.text = "00:00" // 重置显示的时长
                        Toast.makeText(requireContext(), "已中止记录", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null) // 如果用户取消，就什么都不做
                    .show()
            } else { // 如果不在记录，就提示用户未开始记录
                Toast.makeText(requireContext(), "未开始记录", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSave.setOnClickListener {
            if (isRecording) { // 如果在记录，就停止记录，并设置结束时间和标志位
                isRecording = false
                endTime = System.currentTimeMillis()
                Toast.makeText(requireContext(), "停止记录", Toast.LENGTH_SHORT).show()

                // 调用一个函数来保存数据到Excel文件，并提示用户是否保存成功和保存路径
                saveDataToExcel(
                    checkBoxBattery.isChecked,
                    checkBoxThermal.isChecked,
                    checkBoxSoc.isChecked
                )
            } else { // 如果不在记录，就提示用户未开始记录
                Toast.makeText(requireContext(), "未开始记录", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 在界面创建后，观察view model中的live data，并根据是否在记录来更新数据列表和显示的时长

        batteryViewModel.batteryData.observe(viewLifecycleOwner) { batteryData ->
            if (isRecording) { // 如果在记录，就把电池数据添加到列表中，并更新显示的时长
                batteryDataList.add(batteryData)
                updateTextViewTime()
                Timber.tag("BatteryData")
                    .d("Added $batteryData to the list") // 打印日志，显示添加了哪个电池数据对象
            }
        }

        thermalViewModel.thermalList.observe(viewLifecycleOwner) { thermalList ->
            if (isRecording) { // 如果在记录，就把温度数据添加到列表中，并更新显示的时长
                thermalDataList.add(thermalList)
                updateTextViewTime()
            }
        }

        socViewModel.dynamicInfo.observe(viewLifecycleOwner) { dynamicInfo ->
            if (isRecording) { // 如果在记录，就把Soc数据添加到列表中，并更新显示的时长
                socDataList.add(dynamicInfo)
                updateTextViewTime()
            }
        }
    }

    // 定义一个函数，用于更新显示的时长
    private fun updateTextViewTime() {
        // 获取当前时间和开始时间的差值，单位毫秒
        val duration = SystemClock.elapsedRealtime() - startTime
        // 将差值转换为分钟和秒数
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 % 60
        // 格式化为两位数的字符串，如01:23
        val timeString = String.format("%02d:%02d", minutes, seconds)
        // 更新界面上的TextView
        textViewTime.text = timeString
    }


    // 定义一个函数，用于保存数据到Excel文件
    private fun saveDataToExcel(
        isBatteryChecked: Boolean, // 是否勾选了电池数据
        isThermalChecked: Boolean, // 是否勾选了温度数据
        isSocChecked: Boolean // 是否勾选了Soc数据
    ) {
        try {
            // 创建一个Excel工作簿对象
            val workbook = HSSFWorkbook()

            // 如果勾选了电池数据，就调用一个函数来创建一个sheet页并填充数据
            if (isBatteryChecked) {
                createBatterySheet(workbook)
            }

            // 如果勾选了温度数据，就调用一个函数来创建一个sheet页并填充数据
            if (isThermalChecked) {
                createThermalSheet(workbook)
            }

            // 如果勾选了Soc数据，就调用一个函数来创建一个sheet页并填充数据
            if (isSocChecked) {
                createSocSheet(workbook)
            }

            // 根据开始和结束时间来生成文件名，格式为TMData-开始时间-结束时间.xls，如TMData-07月05日11点36分-07月05日11点40分.xls
            val fileName =
                FILE_PREFIX + formatTime(startTime) + "-" + formatTime(endTime) + FILE_SUFFIX

            //获取应用目录下的ThermalMonitor文件夹，如果不存在则创建一个
            val folder =
                File(requireContext().getExternalFilesDir(null), "ThermalMonitor").apply { mkdirs() }

            // 在文件夹中创建一个文件对象，并获取它的输出流
            val file = File(folder, fileName)
            val outputStream = file.outputStream()

            // 将Excel工作簿对象写入到输出流中，并关闭输出流和工作簿对象
            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            // 提示用户保存成功，并显示保存路径
            Toast.makeText(
                requireContext(),
                "保存成功，路径为${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) { // 如果发生异常，就提示用户保存失败，并打印异常信息
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // 定义一个函数，用于创建电池数据的sheet页并填充数据
    private fun createBatterySheet(workbook: HSSFWorkbook) {
        // 在工作簿中创建一个sheet页，并命名为TMData-Battery
        val sheet = workbook.createSheet(SHEET_BATTERY)

        // 在sheet页中创建第一行，并设置标题为时间戳、level、status、current、temperature、voltage、source
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("时间戳")
        titleRow.createCell(1).setCellValue("level")
        titleRow.createCell(2).setCellValue("status")
        titleRow.createCell(3).setCellValue("current")
        titleRow.createCell(4).setCellValue("temperature")
        titleRow.createCell(5).setCellValue("voltage")
        titleRow.createCell(6).setCellValue("source")

        // 遍历电池数据列表，从第二行开始，每一行对应一个电池数据对象，每一列对应一个属性值
        for ((index, batteryData) in batteryDataList.withIndex()) {
            val dataRow = sheet.createRow(index + 1) // 创建一行
            dataRow.createCell(0)
                .setCellValue(formatTime(startTime + index * 1000)) // 设置时间戳，假设每个数据间隔1秒
            dataRow.createCell(1).setCellValue(batteryData.level.toDouble()) // 设置level
            dataRow.createCell(2).setCellValue(batteryData.status) // 设置status
            dataRow.createCell(3).setCellValue(batteryData.current.toDouble()) // 设置current
            dataRow.createCell(4).setCellValue(batteryData.temperature.toDouble()) // 设置temperature
            dataRow.createCell(5).setCellValue(batteryData.voltage.toDouble()) // 设置voltage
            dataRow.createCell(6).setCellValue(batteryData.source) // 设置source
        }
    }

    // 定义一个函数，用于创建温度数据的sheet页并填充数据
    private fun createThermalSheet(workbook: HSSFWorkbook) {
        // 在工作簿中创建一个sheet页，并命名为TMData-Thermal
        val sheet = workbook.createSheet(SHEET_THERMAL)

        // 在sheet页中创建第一行，并设置第一列的标题为时间戳，其他列的标题为温度数据列表中第一个元素的type值，如type1、type2等
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("时间戳")
        val firstThermalList = thermalDataList[0] // 获取温度数据列表中第一个元素，它是一个包含若干ThermalData对象的列表
        for ((index, thermalData) in firstThermalList.withIndex()) {
            titleRow.createCell(index + 1).setCellValue(thermalData.type) // 设置每一列的标题为type值
        }

        // 遍历温度数据列表，从第二行开始，每一行对应一个包含若干ThermalData对象的列表，每一列对应一个temp值
        for ((index, thermalList) in thermalDataList.withIndex()) {
            val dataRow = sheet.createRow(index + 1) // 创建一行
            dataRow.createCell(0)
                .setCellValue(formatTime(startTime + index * 1000)) // 设置时间戳，假设每个数据间隔1秒
            for ((column, thermalData) in thermalList.withIndex()) {
                dataRow.createCell(column + 1)
                    .setCellValue(thermalData.temp.toDouble()) // 设置每一列的temp值
            }
        }
    }

    // 定义一个函数，用于创建Soc数据的sheet页并填充数据
    private fun createSocSheet(workbook: HSSFWorkbook) {
        // 在工作簿中创建一个sheet页，并命名为TMData-Soc
        val sheet = workbook.createSheet(SHEET_SOC)

        // 在sheet页中创建第一行，并设置第一列的标题为时间戳，其他列的标题为Soc数据列表中第一个元素的coreNumber值，如number1、number2等
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("时间戳")
        val firstSocList = socDataList[0] // 获取Soc数据列表中第一个元素，它是一个包含若干DynamicInfo对象的列表
        for ((index, dynamicInfo) in firstSocList.withIndex()) {
            titleRow.createCell(index + 1)
                .setCellValue("number${dynamicInfo.coreNumber}") // 设置每一列的标题为coreNumber值
        }

        // 遍历Soc数据列表，从第二行开始，每一行对应一个包含若干DynamicInfo对象的列表，每一列对应一个coreFrequency值
        for ((index, socList) in socDataList.withIndex()) {
            val dataRow = sheet.createRow(index + 1) // 创建一行
            dataRow.createCell(0)
                .setCellValue(formatTime(startTime + index * 1000)) // 设置时间戳，假设每个数据间隔1秒
            for ((column, dynamicInfo) in socList.withIndex()) {
                dataRow.createCell(column + 1)
                    .setCellValue(dynamicInfo.coreFrequency.toDouble()) // 设置每一列的coreFrequency值
            }
        }
    }

    // 定义一个函数，用于格式化时间戳为字符串，格式为几月几日几点几分，如07月05日11点36分
    @SuppressLint("SimpleDateFormat")
//    private fun formatTime(time: Long): String {
//        val dateFormat = SimpleDateFormat("MM月dd日HH点mm分ss秒")
//        return dateFormat.format(time)
//    }
    //格式化时间的方法，将毫秒数转换为时分秒的形式
    private fun formatTime(time: Long): String {
        val hours = time / (1000 * 60 * 60)
        val minutes = (time % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (time % (1000 * 60)) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

