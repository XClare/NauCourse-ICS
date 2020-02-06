package tool.naucourse.ics.caculate

import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.RandomUidGenerator
import tool.naucourse.ics.Constants
import tool.naucourse.ics.contents.beans.Course
import tool.naucourse.ics.contents.beans.Exam
import tool.naucourse.ics.contents.beans.TermDate
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Locale


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
        var startTime: Date
        var endTime: Date
        var event: VEvent

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
                    for (i in week.start..week.end) {
                        for (time in courseTime.courseNum) {
                            startTime = getStartTime(
                                i,
                                courseTime.weekDay,
                                time.start,
                                termDate
                            )
                            endTime = getEndTime(
                                i,
                                courseTime.weekDay,
                                time.end,
                                termDate
                            )
                            event = createEvent(
                                startTime,
                                endTime,
                                courseText,
                                courseTime.location,
                                courseSummary
                            )
                            ics.components.add(event)
                        }
                    }
                }
            }
        }

        FileOutputStream(output).use {
            CalendarOutputter().output(ics, it)
        }
    }

    private fun createEvent(
        startTime: Date,
        endTime: Date,
        text: String,
        location: String,
        summary: String
    ): VEvent {
        val event = VEvent()
        with(event.properties) {
            add(DtStart(net.fortuna.ical4j.model.DateTime(startTime.time)))
            add(DtEnd(net.fortuna.ical4j.model.DateTime(endTime.time)))
            add(Summary(text))
            add(uidGenerator.generateUid())
            add(Description(summary))
            add(Location(location))
        }
        return event
    }

    private fun getStartTime(weekNum: Int, weekDay: Short, courseStartNum: Int, termDate: TermDate): Date {
        val calendar = java.util.Calendar.getInstance(Locale.CHINA)
        calendar.time = termDate.startDate
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.add(
            java.util.Calendar.DATE,
            calendar.firstDayOfWeek - calendar.get(java.util.Calendar.DAY_OF_WEEK) + (weekNum - 1) * 7 + weekDay - 1
        )
        calendar.set(java.util.Calendar.HOUR_OF_DAY, Constants.Course.COURSE_TIME[courseStartNum - 1][0])
        calendar.set(java.util.Calendar.MINUTE, Constants.Course.COURSE_TIME[courseStartNum - 1][1])
        calendar.set(java.util.Calendar.SECOND, 0)
        return calendar.time
    }

    private fun getEndTime(weekNum: Int, weekDay: Short, courseEndNum: Int, termDate: TermDate): Date {
        val calendar = java.util.Calendar.getInstance(Locale.CHINA)
        calendar.time = termDate.startDate
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.add(
            java.util.Calendar.DATE,
            calendar.firstDayOfWeek - calendar.get(java.util.Calendar.DAY_OF_WEEK) + (weekNum - 1) * 7 + weekDay - 1
        )
        calendar.set(java.util.Calendar.HOUR_OF_DAY, Constants.Course.COURSE_TIME[courseEndNum - 1][2])
        calendar.set(java.util.Calendar.MINUTE, Constants.Course.COURSE_TIME[courseEndNum - 1][3])
        calendar.set(java.util.Calendar.SECOND, 0)
        return calendar.time
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