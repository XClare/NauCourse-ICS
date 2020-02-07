package tool.naucourse.ics.caculate

import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.RandomUidGenerator
import tool.naucourse.ics.Constants
import tool.naucourse.ics.contents.beans.Course
import tool.naucourse.ics.contents.beans.CourseTime
import tool.naucourse.ics.contents.beans.Exam
import tool.naucourse.ics.contents.beans.TermDate
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Date
import kotlin.math.abs


object ICSConverter {
    private val uidGenerator = RandomUidGenerator()

    private fun getInitCal(): Calendar {
        val ics = Calendar()

        ics.properties.add(ProdId("-//Author: ${Constants.AUTHOR}//Produced By: ${Constants.APPLICATION_NAME}//Version: ${Constants.VERSION}"))
        ics.properties.add(Version.VERSION_2_0)
        ics.properties.add(CalScale.GREGORIAN)

        val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
        val timezone: TimeZone? = try {
            registry.getTimeZone("Asia/Shanghai")
        } catch (e: Exception) {
            null
        }
        val vTimeZone: VTimeZone? = timezone?.vTimeZone

        if (vTimeZone != null) ics.properties.add(vTimeZone.timeZoneId)

        return ics
    }

    fun convertCourse(courseSet: Set<Course>, termDate: TermDate, output: File) {
        val ics = getInitCal()

        var courseSummary: String
        var courseText: String

        for (course in courseSet) {
            courseSummary = "ID：${course.id}\n" +
                    "名称：${course.name}\n" +
                    "合班号：${course.courseClass ?: "无"}\n" +
                    "教学班：${course.teachClass}\n" +
                    "老师：${course.teacher}\n" +
                    "学分：${course.credit}\n" +
                    "课程性质：${course.property ?: "无"}\n" +
                    "课程类型：${course.type}"

            for (courseTime in course.timeSet!!) {
                courseText = "${course.name}@${courseTime.location}"

                for (week in courseTime.weeks) {
                    if (week.end == week.start) {
                        addEventByCourseTime(ics, courseTime, week.start, termDate, courseText, courseSummary)
                    } else {
                        val rRule = getRRule(courseTime, week)
                        val weekNum = getFirstWeekNum(courseTime, week)
                        addEventByCourseTime(ics, courseTime, weekNum, termDate, courseText, courseSummary, rRule)
                    }
                }
            }
        }

        FileOutputStream(output).use {
            CalendarOutputter().output(ics, it)
        }
    }

    private fun getFirstWeekNum(courseTime: CourseTime, week: CourseTime.Time): Int = when (courseTime.weeksMode) {
        CourseTime.WeeksMode.DOUBLE -> {
            if (week.start % 2 == 0) {
                week.start
            } else {
                week.start + 1
            }
        }
        CourseTime.WeeksMode.SINGLE -> {
            if (week.start % 2 == 0) {
                week.start + 1
            } else {
                week.start
            }
        }
        else -> week.start
    }

    private fun getRRule(courseTime: CourseTime, week: CourseTime.Time): RRule? =
        when (courseTime.weeksMode) {
            CourseTime.WeeksMode.DOUBLE -> {
                val recur = Recur.Builder().apply {
                    frequency(Recur.Frequency.WEEKLY)
                    count(getEvenAmountInRange(week.start, week.end))
                    interval(2)
                    dayList(WeekDayList(getWeekDay(courseTime.weekDay)))
                    weekStartDay(WeekDay.Day.MO)
                }.build()
                RRule(recur)
            }
            CourseTime.WeeksMode.SINGLE -> {
                val recur = Recur.Builder().apply {
                    frequency(Recur.Frequency.WEEKLY)
                    count(getOddAmountInRange(week.start, week.end))
                    interval(2)
                    dayList(WeekDayList(getWeekDay(courseTime.weekDay)))
                    weekStartDay(WeekDay.Day.MO)
                }.build()
                RRule(recur)
            }
            else -> {
                val recur = Recur.Builder().apply {
                    frequency(Recur.Frequency.WEEKLY)
                    count(week.end - week.start + 1)
                    interval(1)
                    dayList(WeekDayList(getWeekDay(courseTime.weekDay)))
                    weekStartDay(WeekDay.Day.MO)
                }.build()
                RRule(recur)
            }
        }

    private fun getWeekDay(weekDay: Short): WeekDay =
        when (weekDay.toInt()) {
            1 -> WeekDay.MO
            2 -> WeekDay.TU
            3 -> WeekDay.WE
            4 -> WeekDay.TH
            5 -> WeekDay.FR
            6 -> WeekDay.SA
            7 -> WeekDay.SU
            else -> throw IllegalArgumentException()
        }

    private fun addEventByCourseTime(
        ics: Calendar,
        courseTime: CourseTime,
        weekNum: Int,
        termDate: TermDate,
        courseText: String,
        courseSummary: String,
        rRule: RRule? = null
    ) {
        var startTime: Date
        var endTime: Date
        var event: VEvent
        for (time in courseTime.courseNum) {
            startTime = getCourseTime(
                weekNum,
                courseTime.weekDay,
                time.start,
                termDate,
                true
            )
            endTime = getCourseTime(
                weekNum,
                courseTime.weekDay,
                time.end,
                termDate,
                false
            )
            event = createEvent(
                startTime,
                endTime,
                courseText,
                courseTime.location,
                courseSummary,
                rRule
            )
            ics.components.add(event)
        }
    }

    private fun createEvent(
        startTime: Date,
        endTime: Date,
        text: String,
        location: String,
        summary: String,
        rRule: RRule? = null
    ): VEvent {
        val event = VEvent()
        with(event.properties) {
            add(DtStart(DateTime(startTime.time)))
            add(DtEnd(DateTime(endTime.time)))
            add(Summary(text))
            add(uidGenerator.generateUid())
            add(Description(summary))
            add(Location(location))
            if (rRule != null) add(rRule)
        }
        return event
    }

    private fun getWeekDay(weekNum: Int, weekDay: Short, termDate: TermDate): Date {
        val calendar = java.util.Calendar.getInstance(Locale.CHINA)
        calendar.time = termDate.startDate
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.add(
            java.util.Calendar.DATE,
            calendar.firstDayOfWeek - calendar.get(java.util.Calendar.DAY_OF_WEEK) + (weekNum - 1) * 7 + weekDay - 1
        )
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        return calendar.time
    }

    private fun getCourseTime(
        weekNum: Int,
        weekDay: Short,
        courseNum: Int,
        termDate: TermDate,
        isStart: Boolean
    ): Date {
        val calendar = java.util.Calendar.getInstance(Locale.CHINA)
        calendar.time = getWeekDay(weekNum, weekDay, termDate)
        if (isStart) {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, Constants.Course.COURSE_TIME[courseNum - 1][0])
            calendar.set(java.util.Calendar.MINUTE, Constants.Course.COURSE_TIME[courseNum - 1][1])
        } else {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, Constants.Course.COURSE_TIME[courseNum - 1][2])
            calendar.set(java.util.Calendar.MINUTE, Constants.Course.COURSE_TIME[courseNum - 1][3])
        }
        calendar.set(java.util.Calendar.SECOND, 0)
        return calendar.time
    }

    //获取指定闭区间范围内奇数数量
    private fun getEvenAmountInRange(rangeStart: Int, rangeEnd: Int): Int {
        val total = abs(rangeEnd - rangeStart + 1)
        return when {
            rangeStart % 2 == 0 -> total / 2 + 1
            else -> total / 2
        }
    }

    //获取指定闭区间范围内奇数数量
    private fun getOddAmountInRange(rangeStart: Int, rangeEnd: Int): Int {
        val total = rangeEnd - rangeStart + 1
        return when {
            rangeStart % 2 == 0 || total % 2 == 0 -> total / 2
            else -> total / 2 + 1
        }
    }

    fun convertExam(examData: Array<Exam>, output: File) {
        val ics = getInitCal()

        var examText: String
        var examSummary: String
        var event: VEvent

        examData.forEach {
            examText = "[考试] ${it.name}"
            examSummary = "ID: ${it.courseId}\n" +
                    "名称：${it.name}\n" +
                    "教学班：${it.teachClass}\n" +
                    "地点：${it.location}\n" +
                    "学分：${it.credit}\n" +
                    "课程性质：${it.property}\n" +
                    "课程类别：${it.type}"
            event = createEvent(it.startDate, it.endDate, examText, it.location, examSummary)
            ics.components.add(event)
        }

        FileOutputStream(output).use {
            CalendarOutputter().output(ics, it)
        }
    }
}