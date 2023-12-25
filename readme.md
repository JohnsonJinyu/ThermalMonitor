# ThermalMonitor版本更新记录

## 2023/12/24
### 改善点
- 为了避免后台运行的时候，抓取数据暂停进行，切位前台服务，且通知栏有提醒


## 2023/12/21
### 改善点
- UI配色
- 应用内查看已有文件，点击后可以用其他应用（wps）打开，开发中
- Excel文件中的时间格式显示为HH:mm:ss

### 存在的问题
- 后台状态下，MainActivity Stop后抓取数据不会继续，需要优化
- 文件列表无法在增加文件后刷新
- 文件列表显示style需要优化，点击用其他应用打开这个功能待实现
- 抓取完成后文件保存速度慢，等待时间较长
- recyclerView 只显示一部分的问题需要解决

## 2023/12/19
### 主要改善点
- 可以支持battery、thermal、soc数据抓取、保存

### 当前存在的问题
- UI需要改善，改为沉浸式状态栏
- 打开文件夹的跳转方式需要改为系统默认的文件管理器，而不是安卓原生的文件分享页面
- Excel文件中的时间戳的格式需要优化为excel可以识别的格式
