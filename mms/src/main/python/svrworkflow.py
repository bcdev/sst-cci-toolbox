__author__ = 'Ralf Quast'

import os
import datetime
import exceptions

from svrrunner import SvrRunner

from workflow import Period
from workflow import MultiPeriod
from workflow import Sensor
from workflow import Job
from workflow import Monitor


class SvrWorkflow:
    def __init__(self, usecase, version, archive_root, verification_period=None):
        """

        :type usecase: str
        :type version: str
        :type archive_root: str
        :type verification_period: Period
        """
        self.archive_root = archive_root
        self.version = version
        self.usecase = usecase
        self.verification_period = verification_period
        self.sensors = set()

    def get_usecase(self):
        """

        :rtype : str
        """
        return self.usecase

    def get_verification_period(self):
        """

        :rtype : Period
        """
        return self.verification_period

    def add_sensor(self, name, start_date, end_date):
        """

        :type name: str
        """
        period = Period(start_date, end_date)
        for sensor in self._get_sensors():
            if sensor.get_name() == name and sensor.get_period().is_intersecting(period):
                raise exceptions.ValueError, "Periods of sensor '" + name + "' must not intersect."
        self.sensors.add(Sensor(name, period))

    def run(self, hosts=list([('localhost', 60)]), calls=list(), log_dir='trace',
            simulation=False):
        """

        :type hosts: list
        :type calls: list
        :type log_dir: str
        :type simulation: bool
        """
        m = self.__get_monitor(hosts, calls, log_dir, simulation)

        production_period = self.__get_effective_verification_period()
        date = production_period.get_start_date()

        while date < production_period.get_end_date():
            chunk = Period(date, _next_year_start(date))
            self._execute_verification(m, chunk)
            date = _next_year_start(date)
        self._execute_report_accumulation(m)
        m.wait_for_completion_and_terminate()

    def _get_sensors(self):
        """

        :rtype : list
        """
        return sorted(list(self.sensors), reverse=True)

    def _get_sensors_by_period(self):
        """

        :rtype : list
        """
        multi_periods = dict()
        for sensor in self._get_sensors():
            sensor_name = sensor.get_name()
            period = sensor.get_period()
            SvrWorkflow.__add_period(multi_periods, sensor_name, period)
        sensors = list()
        for name in sorted(multi_periods.keys()):
            for period in multi_periods[name].get_periods():
                sensors.append(Sensor(name, period))
        return sensors

    @staticmethod
    def __add_period(multi_periods, sensor, period):
        """

        :type multi_periods: dict
        :type period: Period
        :type sensor: str
        """
        if sensor not in multi_periods:
            multi_periods[sensor] = MultiPeriod()
        multi_period = multi_periods[sensor]
        """:type : MultiPeriod"""
        multi_period.add(period)

    def _get_data_period(self):
        """

        :rtype : Period
        """
        start_date = datetime.date.max
        end_date = datetime.date.min
        for sensor in self._get_sensors():
            period = sensor.get_period()
            if period.get_start_date() < start_date:
                start_date = period.get_start_date()
            if period.get_end_date() > end_date:
                end_date = period.get_end_date()
        if start_date < end_date:
            return Period(start_date, end_date)
        else:
            return None

    def __get_monitor(self, hosts, calls, log_dir, simulation):
        """

        :type hosts: list
        :type calls: list
        :type log_dir: str
        :type simulation: bool
        :rtype : Monitor
        """
        preconditions = list()
        return Monitor(preconditions, self.get_usecase(), hosts, calls, log_dir, simulation)

    def __get_effective_verification_period(self):
        """

        :rtype : Period
        """
        data_period = self._get_data_period()
        if data_period is None:
            return None
        verification_period = self.get_verification_period()
        if verification_period is None:
            return data_period
        else:
            return verification_period.get_intersection(data_period)

    def _execute_verification(self, monitor, chunk):
        """

        :type monitor: Monitor
        :type chunk: Period
        """
        for sensor in self._get_sensors_by_period():
            sensor_name = sensor.get_name()
            period = sensor.get_period().get_intersection(chunk)
            if period is not None:
                date = period.get_start_date()
                end_date = period.get_end_date()
                while date < end_date:
                    (year, month) = _year_month(date)
                    job = Job('svr-start' + '-' + year + '-' + month + '-' + sensor_name,
                              'svr-start.sh',
                              list(),
                              ['/svr/' + sensor_name],
                              [year, month, sensor_name, self.usecase, self.version, self.archive_root])
                    monitor.execute(job)
                    date = _next_month(date)

    def _execute_report_accumulation(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_sensors_by_period():
            sensor_name = sensor.get_name()
            report_dirpath = SvrRunner.get_report_dirpath(self.archive_root, self.version, self.usecase, sensor_name)
            summary_report_pathname = os.path.join(report_dirpath, sensor_name + '-summary.json')
            job = Job('svr-accumulate-start' + sensor_name,
                      'svr-accumulate-start.sh',
                      ['/svr/' + sensor_name],
                      ['/sum/' + sensor_name],
                      [report_dirpath, summary_report_pathname])
            monitor.execute(job)


def _pathformat(date):
    """

    :type date: datetime.date
    :rtype: str
    """
    return date.isoformat()[:7].replace('-', '/')


def _next_month(date):
    """

    :type date: datetime.date
    :rtype : datetime.date
    """
    if date.month != 12:
        return datetime.date(date.year, date.month + 1, 1)
    else:
        return datetime.date(date.year + 1, 1, 1)


def _next_year_start(date):
    """

    :type date: datetime.date
    :rtype : datetime.date
    """
    return datetime.date(date.year + 1, 1, 1)


def _year_month(date):
    """

    :type date: datetime.date
    :rtype: tuple
    """
    return _pathformat(date).split('/', 1)