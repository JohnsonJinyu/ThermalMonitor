package com.example.thermalmonitor.overview

import org.apache.poi.xssf.usermodel.XSSFWorkbook

class DataProcessToSave {
    /**
     * 这个类的作用是分别对数组中抓取的battery thermal soc 数据处理，写入excel的sheet页
     * */



    /**
     *这部分是对电池数据的处理
     * */
    fun processBatteryData(workbook: XSSFWorkbook, data: Array<Array<String>>) {
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

    fun processThermalData(workbook: XSSFWorkbook, data: Array<Array<String>>) {
        val sheetThermal =
            workbook.createSheet("TMData-Thermal") // create a sheet for thermal data

        val titleRowThermal =
            sheetThermal.createRow(0) // create a title row for thermal data
        val titleArrayThermal =
            arrayOf("时间戳") + thermalViewModel.thermalList.value!!.map { it.type } // create a title array for thermal data by adding the timestamp and the types of thermal zones
        for (i in titleArrayThermal.indices) { // loop through the title array and write each title to the title row
            val cell = titleRowThermal.createCell(i)
            cell.setCellValue(titleArrayThermal[i])
        }
        for (i in 1..data.size) { // loop through the data array and write each row of thermal data to the sheet
            val row = sheetThermal.createRow(i)
            val cell = row.createCell(0)
            cell.setCellValue(data[i - 1][0]) // write the timestamp to the first cell
            for (j in 1 until titleArrayThermal.size) {
                val cell = row.createCell(j)
                cell.setCellValue(data[i - 1][j]) // write the temperatures to the rest cells, skipping the first 6 columns of battery data
            }
        }
    }

    fun processSocData(workbook: XSSFWorkbook, data: Array<Array<String>>) {
        val sheetSoc = workbook.createSheet("TMData-Soc") // create a sheet for soc data

        val titleRowSoc = sheetSoc.createRow(0) // create a title row for soc data
        val titleArraySoc =
            arrayOf("时间戳") + socViewModel.dynamicInfo.value!!.map { "number${it.coreNumber}" } // create a title array for soc data by adding the timestamp and the numbers of cores
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
}