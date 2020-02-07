package tool.naucourse.ics

import tool.naucourse.ics.caculate.ICSConverter
import tool.naucourse.ics.contents.base.ContentErrorReason
import tool.naucourse.ics.contents.base.ContentResult
import tool.naucourse.ics.contents.beans.CourseSet
import tool.naucourse.ics.contents.beans.Exam
import tool.naucourse.ics.contents.beans.StudentPersonalInfo
import tool.naucourse.ics.contents.beans.TermDate
import tool.naucourse.ics.contents.methods.*
import tool.naucourse.ics.network.NauNetworkManager
import tool.naucourse.ics.network.clients.base.LoginInfo
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*


object Main {
    private val outputPath = getJarDirFile().absolutePath

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size >= 3) {
            val userId = args[0].trim()
            val userPw = args[1].trim()
            val type = when (args[2].trim()) {
                "1" -> NetDataType.THIS_TERM_COURSE
                "2" -> NetDataType.NEXT_TERM_COURSE
                "3" -> NetDataType.EXAM
                else -> throw IllegalArgumentException("未知数据类型参数：${args[2]}")
            }
            if (type == NetDataType.NEXT_TERM_COURSE && args.size >= 5) {
                var startDay: Date? = null
                var endDay: Date? = null
                try {
                    startDay = Constants.Time.DATE_FORMAT_YMD.parse(args[3])!!
                    endDay = Constants.Time.DATE_FORMAT_YMD.parse(args[4])!!
                } catch (e: Exception) {
                    println("输入日期的格式有误！")
                }
                runProcess(userId, userPw, type, startDay, endDay)
            } else {
                runProcess(userId, userPw, type)
            }
        } else {
            runProcess()
        }
        finishRunning()
    }

    private fun runProcess(
        userId: String? = null,
        userPw: String? = null,
        type: NetDataType? = null,
        startDay: Date? = null,
        endDay: Date? = null
    ) {
        showBeforeLoginText()
        if (askForLogin(userId, userPw)) {
            val courseDataResult: ContentResult<CourseSet>
            val termDateResult: ContentResult<TermDate>
            val examArrangeList: ContentResult<Array<Exam>>
            try {
                val studentInfo = StudentIndex.getContentData()
                when (askCourseTableType(type)) {
                    NetDataType.THIS_TERM_COURSE -> {
                        println("\n正在从教务获取学期数据... ...")
                        termDateResult = TermInfo.getContentData()
                        println("正在从教务获取本学期课程数据... ...")
                        courseDataResult = MyCourseScheduleTable.getContentData()
                        convertCourse(studentInfo, courseDataResult, termDateResult)
                    }
                    NetDataType.NEXT_TERM_COURSE -> {
                        termDateResult = askForTermDate(startDay, endDay)
                        println("\n正在从教务获取下学期课程数据... ...")
                        courseDataResult = MyCourseScheduleTableNext.getContentData()
                        if (!courseDataResult.isSuccess
                            && courseDataResult.contentErrorResult == ContentErrorReason.PARSE_FAILED
                            || courseDataResult.contentData!!.courses.isEmpty()
                        ) {
                            println("下学期课程数据不完善，目前无法进行转换")
                        } else {
                            convertCourse(studentInfo, courseDataResult, termDateResult)
                        }
                    }
                    NetDataType.EXAM -> {
                        println("\n正在从教务获取学期数据... ...")
                        termDateResult = TermInfo.getContentData()
                        println("正在从教务获取考试日程数据... ...")
                        examArrangeList = MyExamArrangeList.getContentData()
                        convertExam(studentInfo, examArrangeList, termDateResult)
                    }
                }
            } catch (e: IOException) {
                println("网络请求时出现错误！请重试！")
            } catch (e: Exception) {
                e.printStackTrace()
                println("出现未知错误！请反馈后重试！")
            }

            println("\n正在注销登录... ...")
            NauNetworkManager.getJwcClient().logout()
        }
    }

    private fun convertCourse(
        studentInfo: ContentResult<StudentPersonalInfo>,
        courseDataResult: ContentResult<CourseSet>,
        termDateResult: ContentResult<TermDate>
    ) {
        if (studentInfo.isSuccess) {
            println("当前用户：${studentInfo.contentData!!.name.second}")
        } else {
            println("未知用户！")
        }
        if (courseDataResult.isSuccess && termDateResult.isSuccess) {
            val courseData = courseDataResult.contentData!!
            val termDate = termDateResult.contentData!!

            val icsFileName = if (studentInfo.isSuccess) {
                "${studentInfo.contentData!!.name.second} ${courseData.term}学期课表"
            } else {
                "${courseData.term}学期课表"
            }

            if (termDate.getTerm() == courseData.term) {
                val outputFile = File(outputPath + File.separator + "$icsFileName.ics")
                println("\n正在转换课程中... ...")
                ICSConverter.convertCourse(courseData.courses, termDate, outputFile)
                println("课程转换完成！文件被输出到：${outputFile.absolutePath}")
            } else {
                println("课程学期设置错误，获取到的课程学期与学期的开始与结束时间不匹配！")
            }
        } else if (!courseDataResult.isSuccess) {
            println("获取课程时出现错误，错误原因：${courseDataResult.contentErrorResult}")
        } else {
            println("获取学期开始与结束时间时出现错误，错误原因：${termDateResult.contentErrorResult}")
        }
    }

    private fun convertExam(
        studentInfo: ContentResult<StudentPersonalInfo>,
        examArr: ContentResult<Array<Exam>>,
        termDateResult: ContentResult<TermDate>
    ) {
        if (examArr.isSuccess) {
            val examData = examArr.contentData!!
            val termDate = termDateResult.contentData!!

            if (examData.isEmpty()) {
                println("当前考试日程为空！")
            } else {
                val icsFileName = if (studentInfo.isSuccess) {
                    "${studentInfo.contentData!!.name.second} ${termDate.getTerm()}学期考试日程安排"
                } else {
                    "${termDate.getTerm()}学期考试日程安排"
                }

                val outputFile = File(outputPath + File.separator + "$icsFileName.ics")
                println("\n正在转换考试日程中... ...")
                ICSConverter.convertExam(examData, outputFile)
                println("考试日程转换完成！文件被输出到：${outputFile.absolutePath}")
            }
        } else if (!examArr.isSuccess) {
            println("获取考试日程时出现错误，错误原因：${examArr.contentErrorResult}")
        } else {
            println("获取学期开始与结束时间时出现错误，错误原因：${termDateResult.contentErrorResult}")
        }
    }

    private fun showBeforeLoginText() {
        println("欢迎使用${Constants.APPLICATION_NAME}课程转换工具")
        println("作者：${Constants.AUTHOR}")
        println("版本：${Constants.VERSION}")
        println("本应用基于GPL3.0开源协议开源")
        println("开源地址：https://github.com/XClare/NauCourse-ICS")
        println("\n-> 使用前须知：由于学校教考日程变动，因此可能会导致时间不准确，如出现问题，后果自负 <-")
        println("-> 使用时请全程保持网络可用状态！ <-\n")
    }

    private fun askForLogin(userId: String? = null, userPw: String? = null): Boolean {
        val loginId: String
        val loginPw: String
        if (userId != null && userPw != null) {
            loginId = userId
            loginPw = userPw
        } else {
            println("请输入您的用户登录信息，并以回车表示结束")
            print("学号：")
            loginId = readLine()!!
            print("SSO登录密码：")
            loginPw = readLine()!!

        }

        println("正在登录中... ...")
        NauNetworkManager.setLoginInfo(
            LoginInfo(
                loginId,
                loginPw
            )
        )
        val loginResponse = NauNetworkManager.getJwcClient().login()
        return if (loginResponse.isSuccess) {
            println("登录成功")
            true
        } else {
            println("登录时出现错误，错误原因：${loginResponse.loginErrorResult}")
            false
        }
    }

    private fun askForTermDate(startDay: Date? = null, endDay: Date? = null): ContentResult<TermDate> {
        val calendar = Calendar.getInstance(Locale.CHINA)
        if (startDay != null && endDay != null) {
            calendar.time = startDay
            if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                println("学期开始日期不是周一!")
                return ContentResult(false, contentErrorResult = ContentErrorReason.OPERATION)
            }

            calendar.time = endDay
            if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                println("学期结束日期不是周日!")
                return ContentResult(false, contentErrorResult = ContentErrorReason.OPERATION)
            }

            return ContentResult(
                true,
                contentData = TermDate(0, startDay, endDay)
            )
        } else {
            println()
            println("请输入学期开始时间与学期结束时间，并以回车表示结束")
            println("输入格式示例：2020-01-01")

            var startDate: Date? = null
            while (startDate == null) {
                print("学期开始日期(必须是周一)：")
                val dateStr = readLine()!!
                try {
                    startDate = Constants.Time.DATE_FORMAT_YMD.parse(dateStr)!!
                    calendar.time = startDate
                    if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        startDate = null
                        println("学期开始日期不是周一，请重新输入")
                    }
                } catch (e: Exception) {
                    println("日期格式错误，请重新输入")
                }
            }

            var endDate: Date? = null
            while (endDate == null) {
                print("学期结束日期(必须是周日)：")
                val dateStr = readLine()!!
                try {
                    endDate = Constants.Time.DATE_FORMAT_YMD.parse(dateStr)!!
                    calendar.time = endDate
                    if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        endDate = null
                        println("学期结束日期不是周日，请重新输入")
                    } else if (startDate.time >= endDate.time) {
                        endDate = null
                        println("学期开始日期大于结束日期，请重新输入")
                    }
                } catch (e: Exception) {
                    println("日期格式错误，请重新输入")
                }
            }
            return ContentResult(
                true,
                contentData = TermDate(0, startDate, endDate)
            )
        }
    }

    private fun askCourseTableType(type: NetDataType? = null): NetDataType {
        if (type == null) {
            println()
            println("请选择您想要获取的课程类别：")
            println("1. 本学期课程")
            println("2. 下学期课程")
            println("3. 本学期考试日程")
            println("请输入类别编号（如 1）后以回车表示确定")
            var output: NetDataType? = null
            while (output == null) {
                print("请选择：")
                when (readLine()!!.toInt()) {
                    1 -> output = NetDataType.THIS_TERM_COURSE
                    2 -> output = NetDataType.NEXT_TERM_COURSE
                    3 -> output = NetDataType.EXAM
                    else -> println("你输入了错误的参数！")
                }
            }
            return output
        } else {
            return type
        }
    }

    private fun finishRunning() {
        println()
        println("<结束运行>")
    }

    private fun getJarDirFile(): File {
        var path = javaClass.protectionDomain.codeSource.location.path
        var startIndex = 0
        if (System.getProperty("os.name").trim().toLowerCase().startsWith("win")) {
            if (path.startsWith("/")) {
                startIndex = 1
            }
        }
        path = path.substring(startIndex)
        try {
            path = URLDecoder.decode(path, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        val file = File(path)
        return file.parentFile
    }

    private enum class NetDataType {
        THIS_TERM_COURSE,
        NEXT_TERM_COURSE,
        EXAM
    }
}