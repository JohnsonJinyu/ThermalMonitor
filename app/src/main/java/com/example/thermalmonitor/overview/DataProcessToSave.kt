package com.example.thermalmonitor.overview

import android.util.Log
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class DataProcessToSave(
    private val thermalViewModel: ThermalViewModel,
    private val socViewModel: SocViewModel
) {
    /**
     * 这个类的作用是分别对数组中抓取的battery thermal soc 数据处理，写入excel的sheet页
     * 在类的构造函数中接收viewModel的参数传递
     * */


    /**
     *这部分是对电池数据的处理
     * */
    fun processBatteryData(workbook: XSSFWorkbook, data: Array<Array<String>>) {

        if (data.isEmpty()) {
            Log.e("DataCaptureService", "Battery data array is empty")
            return
        }
        val sheetBattery = workbook.createSheet("TMData-Battery") // 创建电池数据的sheet页
        val titleRowBattery = sheetBattery.createRow(0) // 创建电池数据的标题行
        val titleArrayBattery = arrayOf(
            "时间戳",
            "level",
            "status",
            "current",
            "temperature",
            "voltage",
            "source"
        ) // 创建电池数据的title array

        // loop through the title array and write each title to the title row
        for (i in titleArrayBattery.indices) {
            val cell = titleRowBattery.createCell(i)
            cell.setCellValue(titleArrayBattery[i])
        }

        // loop through the data array and write each row of battery data to the sheet
        for (i in 1..data.size) {
            val row = sheetBattery.createRow(i + 1)
            for (j in 0..6) {
                val cell = row.createCell(j)
                cell.setCellValue(data[i - 1][j])
            }
        }

    }

    /**
     *这部分是对Thermal数据的处理
     * */
    fun processThermalData(workbook: XSSFWorkbook, data: Array<Array<String>>) {

        // 为thermal数据创建sheet页
        val sheetThermal = workbook.createSheet("TMData-Thermal")
        // 为thermal data 创建 title row
        val titleRowThermal = sheetThermal.createRow(0)
        // create a title array for thermal data by adding the timestamp and the types of thermal zones
        val titleArrayThermal = arrayOf("时间戳") + thermalViewModel.thermalList.value!!.map { it.type }
        // loop through the title array and write each title to the title row
        for (i in titleArrayThermal.indices) {
            val cell = titleRowThermal.createCell(i)
            cell.setCellValue(titleArrayThermal[i])
        }
        for (i in 1..data.size) { // loop through the data array and write each row of thermal data to the sheet
            val row = sheetThermal.createRow(i)
            val cell = row.createCell(0)
            cell.setCellValue(data[i - 1][0]) // write the timestamp to the first cell
            for (j in 1 until titleArrayThermal.size) {
                val cell1 = row.createCell(j)
                cell1.setCellValue(data[i - 1][j]) // write the temperatures to the rest cells, skipping the first 6 columns of battery data
            }
        }
    }

    /**
     *这部分是对Soc数据的处理
     * */
    fun processSocData(workbook: XSSFWorkbook, data: Array<Array<String>>) {
        // create a sheet for soc data
        val sheetSoc = workbook.createSheet("TMData-Soc")
        // create a title row for soc data
        val titleRowSoc = sheetSoc.createRow(0)
        // create a title array for soc data by adding the timestamp and the numbers of cores
        val titleArraySoc = arrayOf("时间戳") + socViewModel.dynamicInfo.value!!.map { "number${it.coreNumber}" }
        // loop through the title array and write each title to the title row
        for (i in titleArraySoc.indices) {
            val cell = titleRowSoc.createCell(i)
            cell.setCellValue(titleArraySoc[i])
        }
        // loop through the data array and write each row of soc data to the sheet
        for (i in 1..data.size) {
            val row = sheetSoc.createRow(i)
            val cell = row.createCell(0)
            cell.setCellValue(data[i - 1][0]) // write the timestamp to the first cell
            for (j in 1 until titleArraySoc.size) {
                val cell2 = row.createCell(j)
                // write the frequencies to the rest cells
                cell2.setCellValue(data[i - 1][j])
            }
        }
    }
}