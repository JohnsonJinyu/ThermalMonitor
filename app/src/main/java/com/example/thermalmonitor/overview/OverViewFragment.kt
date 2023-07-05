package com.example.thermalmonitor.overview

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.thermalmonitor.battery.BatteryData
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.soc.DynamicInfo
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalData
import com.example.thermalmonitor.thermal.ThermalViewModel
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


//OverViewFragment.kt
class OverViewFragment : Fragment() {

    //获取view model实例
    private val batteryViewModel: BatteryViewModel by activityViewModels()
    private val socViewModel: SocViewModel by activityViewModels()
    private val thermalViewModel: ThermalViewModel by activityViewModels()

    //定义一些变量
    private var isRecording = false //是否在记录状态
    private var timer: CountDownTimer? = null //计时器
    private var recordTime = 0L //记录时间
    private var dataMatrix = mutableListOf<MutableList<String>>() //二维数组，用于保存数据
    private var checkedItems = booleanArrayOf(false, false, false) //用于记录用户勾选的项目
    private var startTime = "" //开始记录时间，用于生成文件名
    private var stopTime = "" //停止记录时间，用于生成文件名

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentOverviewBinding.inflate(inflater)

        //设置checkbox的点击事件，记录用户勾选的项目
        binding.apply {
            checkBoxBattery.setOnCheckedChangeListener { _, isChecked ->
                checkedItems[0] = isChecked
            }
            checkBoxThermal.setOnCheckedChangeListener { _, isChecked ->
                checkedItems[1] = isChecked
            }
            checkBoxSoc.setOnCheckedChangeListener { _, isChecked ->
                checkedItems[2] = isChecked
            }
        }

        //设置开始按钮的点击事件，开始或重新开始记录数据，并开始计时
        binding.buttonStart.setOnClickListener {
            if (!isRecording) { //如果不在记录状态，则开始记录，并开始计时
                isRecording = true
                startRecord()
                startTimer(binding.textViewTime)
                startTime = getCurrentTime() //获取当前时间作为开始记录时间
            } else { //如果在记录状态，则Toast提醒用户正在记录
                Toast.makeText(requireContext(), "正在记录中，请勿重复点击", Toast.LENGTH_SHORT).show()
            }
        }

        //设置中止按钮的点击事件，停止记录状态，计时停止并归零，已经记录的数据清空不保存
        binding.buttonAbort.setOnClickListener {
            if (isRecording) { //如果在记录状态，则弹窗让用户二次确认是否中止
                AlertDialog.Builder(requireContext())
                    .setTitle("提示")
                    .setMessage("确定要中止记录吗？已经记录的数据将不会被保存。")
                    .setPositiveButton("确定") { _, _ ->
                        isRecording = false
                        stopRecord()
                        stopTimer()
                        resetTimer(binding.textViewTime)
                        clearData()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else { //如果不在记录状态，则Toast提醒用户未开始记录
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT).show()
            }
        }

        //设置停止并保存按钮的点击事件，停止记录以及计时，保存数据到Excel文件，并toast提醒用户是否保存成功，以及保存路径
        binding.buttonStop.setOnClickListener {
            if (isRecording) { //如果在记录状态，则停止记录以及计时，并保存数据到Excel文件
                isRecording = false
                stopRecord()
                stopTimer()
                stopTime = getCurrentTime() //获取当前时间作为停止记录时间
                saveDataToExcel()
            } else { //如果不在记录状态，则toast提醒用户未开始记录
                Toast.makeText(requireContext(), "未开始记录，请先点击开始按钮", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    //开始记录数据的方法，根据用户勾选的项目，观察相应的LiveData，并将数据添加到二维数组中，并打印日志方便调试
    private fun startRecord() {
        if (checkedItems[0]) { //如果勾选了电池，则观察batteryData，并将数据添加到二维数组中，并打印日志方便调试
            batteryViewModel.batteryData.observe(viewLifecycleOwner) { data ->
                addBatteryData(data)
                Log.d("BatteryData", dataMatrix.last().toString())
            }
        }
        if (checkedItems[1]) { //如果勾选了温度，则观察thermalList，并将数据添加到二维数组中，并打印日志方便调试
            thermalViewModel.thermalList.observe(viewLifecycleOwner) { list ->
                addThermalData(list)
                Log.d("ThermalData", dataMatrix.last().toString())
            }
        }
        if (checkedItems[2]) { //如果勾选了Soc，则观察dynamicInfo，并将数据添加到二维数组中，并打印日志方便调试
            socViewModel.dynamicInfo.observe(viewLifecycleOwner) { list ->
                addSocData(list)
                Log.d("SocData", dataMatrix.last().toString())
            }
        }
    }

    //停止记录数据的方法，移除对LiveData的观察
    private fun stopRecord() {
        if (checkedItems[0]) { //如果勾选了电池，则移除对batteryData的观察
            batteryViewModel.batteryData.removeObservers(viewLifecycleOwner)
        }
        if (checkedItems[1]) { //如果勾选了温度，则移除对thermalList的观察
            thermalViewModel.thermalList.removeObservers(viewLifecycleOwner)
        }
        if (checkedItems[2]) { //如果勾选了Soc，则移除对dynamicInfo的观察
            socViewModel.dynamicInfo.removeObservers(viewLifecycleOwner)
        }
    }

    //添加电池数据到二维数组的方法，根据BatteryData的属性，将数据添加到相应的位置
    private fun addBatteryData(data: BatteryData) {
        if (dataMatrix.isEmpty()) { //如果二维数组为空，则先添加第一行作为标题
            dataMatrix.add(
                mutableListOf(
                    "时间戳",
                    "level",
                    "status",
                    "current",
                    "temperature",
                    "voltage",
                    "source"
                )
            )
        }
        //获取当前时间作为时间戳
        val timeStamp = getCurrentTime()
        //将电池数据添加到二维数组的最后一行
        dataMatrix.add(
            mutableListOf(
                timeStamp,
                data.level.toString(),
                data.status,
                data.current.toString(),
                data.temperature.toString(),
                data.voltage.toString(),
                data.source
            )
        )
    }

    //添加温度数据到二维数组的方法，根据ThermalData的属性，将数据添加到相应的位置
    private fun addThermalData(list: List<ThermalData>) {
        if (dataMatrix.isEmpty()) { //如果二维数组为空，则先添加第一行作为标题，包括时间戳和温度类型
            val firstRow = mutableListOf("时间戳")
            for (data in list) {
                firstRow.add(data.type)
            }
            dataMatrix.add(firstRow)
        }
        //获取当前时间作为时间戳
        val timeStamp = getCurrentTime()
        //将温度数据添加到二维数组的最后一行，只需要添加温度值，不需要添加类型
        val lastRow = mutableListOf(timeStamp)
        for (data in list) {
            lastRow.add(data.temp)
        }
        dataMatrix.add(lastRow)
    }

    //添加Soc数据到二维数组的方法，根据DynamicInfo的属性，将数据添加到相应的位置
    private fun addSocData(list: List<DynamicInfo>) {
        if (dataMatrix.isEmpty()) { //如果二维数组为空，则先添加第一行作为标题，包括时间戳和核心编号
            val firstRow = mutableListOf("时间戳")
            for (data in list) {
                firstRow.add("number${data.coreNumber}")
            }
            dataMatrix.add(firstRow)
        }
        //获取当前时间作为时间戳
        val timeStamp = getCurrentTime()
        //将Soc数据添加到二维数组的最后一行，只需要添加核心频率，不需要添加核心编号
        val lastRow = mutableListOf(timeStamp)
        for (data in list) {
            lastRow.add(data.coreFrequency.toString())
        }
        dataMatrix.add(lastRow)
    }

    //开始计时的方法，使用CountDownTimer实现计时器，并显示在TextView上
    private fun startTimer(textView: TextView) {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                recordTime += 1000 //每隔一秒钟，记录时间增加1000毫秒
                textView.text = formatTime(recordTime) //将记录时间格式化并显示在TextView上
            }

            override fun onFinish() {

            }
        }.start()
    }

    //停止计时的方法，取消计时器
    private fun stopTimer() {
        timer?.cancel()
    }

    //重置计时的方法，将记录时间归零，并显示在TextView上
    private fun resetTimer(textView: TextView) {
        recordTime = 0L
        textView.text = formatTime(recordTime)
    }

    //格式化时间的方法，将毫秒数转换为时分秒的形式
    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    //获取当前时间的方法，返回一个字符串，格式为月日时分秒
    private fun getCurrentTime(): String {
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    //保存数据到Excel文件的方法，使用Apache POI库操作Excel文件
    private fun saveDataToExcel() {
        //创建一个工作簿对象
        val workbook = XSSFWorkbook()
        //创建一个工作表对象
        val sheet = workbook.createSheet("TMData")
        //遍历二维数组，将每一行数据写入到工作表中
        for ((rowIndex, rowList) in dataMatrix.withIndex()) {
            //创建一行对象
            val row = sheet.createRow(rowIndex)
            //遍历每一行的数据，将每个数据写入到单元格中
            for ((cellIndex, cellData) in rowList.withIndex()) {
                //创建一个单元格对象
                val cell = row.createCell(cellIndex)
                //设置单元格的值
                cell.setCellValue(cellData)
            }
        }
        //生成文件名，格式为TMData-开始记录时间-停止记录时间.xlsx
        val fileName = "TMData-$startTime-$stopTime.xlsx"
        //获取应用目录下的ThermalMonitor文件夹，如果不存在则创建一个
        val dir =
            File(requireContext().getExternalFilesDir(null), "ThermalMonitor").apply { mkdirs() }
        //获取文件对象，如果存在则删除旧文件
        val file = File(dir, fileName).apply { delete() }
        try {
            //创建一个文件输出流，将工作簿对象写入到文件中
            val fos = FileOutputStream(file)
            workbook.write(fos)
            //关闭输出流和工作簿对象
            fos.close()
            workbook.close()
            //Toast提醒用户保存成功，并显示保存路径
            Toast.makeText(
                requireContext(),
                "保存成功，文件路径为${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            //如果发生异常，则Toast提醒用户保存失败，并打印异常信息
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    //清空数据的方法，将二维数组清空
    private fun clearData() {
        dataMatrix.clear()
    }
}


