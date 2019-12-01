# pylint: disable=super-init-not-called
"""
This module provides functions for date and time conversions. The functions in
this module can accept any of the following date types:

.. code-block::

    java.time.ZonedDateTime
    java.time.LocalDateTime
    java.util.Calendar
    java.util.Date
    org.joda.time.DateTime
    datetime.datetime (Python)
    org.eclipse.smarthome.core.library.types.DateTimeType
    org.openhab.core.library.types.DateTimeType
"""
__all__ = [
    "format_date", "days_between", "hours_between", "minutes_between",
    "seconds_between", "to_java_zoneddatetime", "to_java_calendar",
    "to_python_datetime", "to_joda_datetime", "human_readable_seconds"
]

import sys
import datetime

from java.time import LocalDateTime, ZonedDateTime
from java.time import ZoneId, ZoneOffset
from java.time.format import DateTimeFormatter
from java.time.temporal.ChronoUnit import DAYS, HOURS, MINUTES, SECONDS
from java.util import Calendar, Date, TimeZone
from org.joda.time import DateTime, DateTimeZone
from org.eclipse.smarthome.core.library.types import DateTimeType as eclipseDateTime

if 'org.eclipse.smarthome.automation' in sys.modules or 'org.openhab.core.automation' in sys.modules:
    # Workaround for Jython JSR223 bug where dates and datetimes are converted
    # to java.sql.Date and java.sql.Timestamp
    def remove_java_converter(clazz):
        if hasattr(clazz, '__tojava__'):
            del clazz.__tojava__
    remove_java_converter(datetime.date)
    remove_java_converter(datetime.datetime)

try:
    # if the compat1x bundle is not installed, the OH 1.x DateTimeType is not available
    from org.openhab.core.library.types import DateTimeType as LEGACY_DATETIME
except:
    LEGACY_DATETIME = None

def format_date(value, format_string="yyyy-MM-dd'T'HH:mm:ss.SSxx"):
    """
    Returns string of ``value`` formatted according to ``format_string``.

    This function can be used when updating Items in openHAB or to format any
    date value for output. The default format string follows the same ISO8601
    format used in openHAB. If ``value`` does not have timezone information,
    the system default will be used.

    Examples:
        .. code-block::

            events.sendCommand("date_item", format_date(date_value))
            log.info("The time is currently: {}".format(format_date(ZonedDateTime.now())))

    Args:
        value: the value to convert
        format_string (str): the pattern to format ``value`` with.
            See `java.time.format.DateTimeFormatter <https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html>`_
            for format string tokens.

    Returns:
        str: the converted value
    """
    return to_java_zoneddatetime(value).format(DateTimeFormatter.ofPattern(format_string))

def days_between(value_from, value_to, calendar_days=False):
    """
    Returns the number of days between ``value_from`` and ``value_to``.
    Will return a negative number if ``value_from`` is after ``value__to``.

    Examples:
        .. code-block::

            span_days = days_between(items["date_item"], ZonedDateTime.now())

    Args:
        value_from: value to start from
        value_to: value to measure to
        calendar_days (bool): if ``True``, the value returned will be the
            number of calendar days rather than 24-hour periods (default)

    Returns:
        int: the number of days between ``value_from`` and ``value_to``
    """
    if calendar_days:
        return DAYS.between(to_java_zoneddatetime(value_from).toLocalDate().atStartOfDay(), to_java_zoneddatetime(value_to).toLocalDate().atStartOfDay())
    else:
        return DAYS.between(to_java_zoneddatetime(value_from), to_java_zoneddatetime(value_to))

def hours_between(value_from, value_to):
    """
    Returns the number of hours between ``value_from`` and ``value_to``.
    Will return a negative number if ``value_from`` is after ``value__to``.

    Examples:
        .. code-block::

            span_hours = hours_between(items["date_item"], ZonedDateTime.now())

    Args:
        value_from: value to start from
        value_to: value to measure to

    Returns:
        int: the number of hours between ``value_from`` and ``value_to``
    """
    return HOURS.between(to_java_zoneddatetime(value_from), to_java_zoneddatetime(value_to))

def minutes_between(value_from, value_to):
    """
    Returns the number of minutes between ``value_from`` and ``value_to``.
    Will return a negative number if ``value_from`` is after ``value__to``.

    Examples:
        .. code-block::

            span_minutes = minutes_between(items["date_item"], ZonedDateTime.now())

    Args:
        value_from: value to start from
        value_to: value to measure to

    Returns:
        int: the number of minutes between ``value_from`` and ``value_to``
    """
    return MINUTES.between(to_java_zoneddatetime(value_from), to_java_zoneddatetime(value_to))

def seconds_between(value_from, value_to):
    """
    Returns the number of seconds between ``value_from`` and ``value_to``.
    Will return a negative number if ``value_from`` is after ``value__to``.

    Examples:
        .. code-block::

            span_seconds = seconds_between(items["date_item"], ZonedDateTime.now())

    Args:
        value_from: value to start from
        value_to: value to measure to

    Returns:
        int: the number of seconds between ``value_from`` and ``value_to``
    """
    return SECONDS.between(to_java_zoneddatetime(value_from), to_java_zoneddatetime(value_to))

def to_java_zoneddatetime(value):
    """
    Converts any of the supported date types to ``java.time.ZonedDateTime``. If
    ``value`` does not have timezone information, the system default will be
    used.

    Examples:
        .. code-block::

            java_time = to_java_zoneddatetime(items["date_item"])

    Args:
        value: the value to convert

    Returns:
        java.time.ZonedDateTime: the converted value

    Raises:
        TypeError: if the type of ``value`` is not supported by this module
    """
    if isinstance(value, ZonedDateTime):
        return value
    timezone_id = ZoneId.systemDefault()
    # java.time.LocalDateTime
    if isinstance(value, LocalDateTime):
        return value.atZone(timezone_id)
    # python datetime
    if isinstance(value, datetime.datetime):
        if value.tzinfo is not None:
            timezone_id = ZoneId.ofOffset("GMT", ZoneOffset.ofTotalSeconds(int(value.utcoffset().total_seconds())))
        return ZonedDateTime.of(
            value.year,
            value.month,
            value.day,
            value.hour,
            value.minute,
            value.second,
            value.microsecond * 1000,
            timezone_id
        )
    # java.util.Calendar
    if isinstance(value, Calendar):
        return ZonedDateTime.ofInstant(value.toInstant(), ZoneId.of(value.getTimeZone().getID()))
    # java.util.Date
    if isinstance(value, Date):
        return ZonedDateTime.ofInstant(value.toInstant(), ZoneId.ofOffset("GMT", ZoneOffset.ofHours(0 - value.getTimezoneOffset() / 60)))
    # Joda DateTime
    if isinstance(value, DateTime):
        return value.toGregorianCalendar().toZonedDateTime()
    # openHAB DateTimeType
    if isinstance(value, eclipseDateTime):
        return to_java_zoneddatetime(value.calendar)
    # openHAB 1.x DateTimeType
    if LEGACY_DATETIME and isinstance(value, LEGACY_DATETIME):
        return to_java_zoneddatetime(value.calendar)

    raise TypeError("Unknown type: {}".format(str(type(value))))

def to_python_datetime(value):
    """
    Converts any of the supported date types to Python ``datetime.datetime``.
    If ``value`` does not have timezone information, the system default will be
    used.

    Examples:
        .. code-block::

            python_time = to_python_datetime(items["date_item"])

    Args:
        value: the value to convert

    Returns:
        datetime.datetime: the converted value

    Raises:
        TypeError: if the type of ``value`` is not supported by this module
    """
    if isinstance(value, datetime.datetime):
        return value

    value_zoneddatetime = to_java_zoneddatetime(value)
    return datetime.datetime(
        value_zoneddatetime.getYear(),
        value_zoneddatetime.getMonthValue(),
        value_zoneddatetime.getDayOfMonth(),
        value_zoneddatetime.getHour(),
        value_zoneddatetime.getMinute(),
        value_zoneddatetime.getSecond(),
        int(value_zoneddatetime.getNano() / 1000),
        _pythonTimezone(int(value_zoneddatetime.getOffset().getTotalSeconds() / 60))
    )

class _pythonTimezone(datetime.tzinfo):

    def __init__(self, offset=0, name=""):
        """
        Python tzinfo with ``offset`` in minutes and name ``name``.

        Args:
            offset (int): Timezone offset from UTC in minutes.
            name (str): Display name of this instance.
        """
        self.__offset = offset
        self.__name = name
        #super(_pythonTimezone, self).__init__()

    def utcoffset(self, value):
        return datetime.timedelta(minutes=self.__offset)

    def tzname(self, value):
        return self.__name

    def dst(self, value):
        return datetime.timedelta(0)

def to_joda_datetime(value):
    """
    Converts any of the supported date types to ``org.joda.time.DateTime``. If
    ``value`` does not have timezone information, the system default will be
    used.

    Examples:
        .. code-block::

            joda_time = to_joda_datetime(items["date_item"])

    Args:
        value: the value to convert

    Returns:
        org.joda.time.DateTime: the converted value

    Raises:
        TypeError: if the type of ``value`` is not suported by this package
    """
    if isinstance(value, DateTime):
        return value

    value_zoneddatetime = to_java_zoneddatetime(value)
    return DateTime(
        value_zoneddatetime.toInstant().toEpochMilli(),
        DateTimeZone.forID(value_zoneddatetime.getZone().getId())
    )

def to_java_calendar(value):
    """
    Converts any of the supported date types to ``java.util.Calendar``. If
    ``value`` does not have timezone information, the system default will be
    used.

    Examples:
        .. code-block::

            calendar_time = to_java_calendar(items["date_item"])

    Args:
        value: the value to convert

    Returns:
        java.util.Calendar: the converted value

    Raises:
        TypeError: if the type of ``value`` is not supported by this package
    """
    if isinstance(value, Calendar):
        return value

    value_zoneddatetime = to_java_zoneddatetime(value)
    new_calendar = Calendar.getInstance(TimeZone.getTimeZone(value_zoneddatetime.getZone().getId()))
    new_calendar.set(Calendar.YEAR, value_zoneddatetime.getYear())
    new_calendar.set(Calendar.MONTH, value_zoneddatetime.getMonthValue() - 1)
    new_calendar.set(Calendar.DAY_OF_MONTH, value_zoneddatetime.getDayOfMonth())
    new_calendar.set(Calendar.HOUR_OF_DAY, value_zoneddatetime.getHour())
    new_calendar.set(Calendar.MINUTE, value_zoneddatetime.getMinute())
    new_calendar.set(Calendar.SECOND, value_zoneddatetime.getSecond())
    new_calendar.set(Calendar.MILLISECOND, int(value_zoneddatetime.getNano() / 1000000))
    return new_calendar

def human_readable_seconds(seconds):
    """
    Converts seconds into a human readable string of days, hours, minutes and
    seconds.

    Examples:
        .. code-block::

            message = human_readable_seconds(55555)
            # 15 hours, 25 minutes and 55 seconds

    Args:
        seconds: the number of seconds

    Returns:
        str: a string in the format ``{} days, {} hours, {} minutes and {}
        seconds``
    """
    seconds = int(round(seconds))
    number_of_days = seconds//86400
    number_of_hours = (seconds%86400)//3600
    number_of_minutes = (seconds%3600)//60
    number_of_seconds = (seconds%3600)%60

    days_string = "{} day{}".format(number_of_days, "s" if number_of_days > 1 else "")
    hours_string = "{} hour{}".format(number_of_hours, "s" if number_of_hours > 1 else "")
    minutes_string = "{} minute{}".format(number_of_minutes, "s" if number_of_minutes > 1 else "")
    seconds_string = "{} second{}".format(number_of_seconds, "s" if number_of_seconds > 1 else "")

    return "{}{}{}{}{}{}{}".format(
        days_string if number_of_days > 0 else "",
        "" if number_of_days == 0 or (number_of_hours == 0 and number_of_minutes == 0) else (
            " and " if (number_of_hours > 0 and number_of_minutes == 0 and number_of_seconds == 0) or (number_of_hours == 0 and number_of_minutes > 0 and number_of_seconds == 0) else ", "
        ),
        hours_string if number_of_hours > 0 else "",
        "" if number_of_hours == 0 or number_of_minutes == 0 else (
            " and " if number_of_minutes > 0 and number_of_seconds == 0 else ", "
        ),
        minutes_string if number_of_minutes > 0 else "",
        " and " if number_of_seconds > 0 and (number_of_minutes > 0 or number_of_hours > 0 or number_of_days > 0) else "",
        seconds_string if number_of_seconds > 0 else ""
    )
