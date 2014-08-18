__author__ = 'Ralf Quast'

import datetime
import exceptions

from pmonitor import PMonitor


class Period:
    def __init__(self, start_date, end_date):
        """

        :raise exceptions.ValueError: If the start date is not less than the end date.
        """
        if isinstance(start_date, datetime.date):
            a = start_date
        elif isinstance(start_date, str):
            a = self.__from_iso_format(start_date)
        else:
            a = datetime.date(start_date[0], start_date[1], start_date[2])
        if isinstance(end_date, datetime.date):
            b = end_date
        elif isinstance(end_date, str):
            b = self.__from_iso_format(end_date)
        else:
            b = datetime.date(end_date[0], end_date[1], end_date[2])
        assert a < b
        if a < b:
            self.start_date = a
            """:type : datetime.date"""
            self.end_date = b
            """:type : datetime.date"""
        else:
            raise exceptions.ValueError, "The start date must be less than the end date."

    def get_start_date(self):
        """

        :rtype : datetime.date
        """
        return self.start_date

    def get_end_date(self):
        """

        :rtype : datetime.date
        """
        return self.end_date

    def get_intersection(self, other):
        """

        :type other: Period
        :rtype : Period
        """
        if self.is_intersecting(other):
            start_date = self.get_start_date()
            if start_date < other.get_start_date():
                start_date = other.get_start_date()
            end_date = self.get_end_date()
            if end_date > other.get_end_date():
                end_date = other.get_end_date()
            intersection = Period(start_date, end_date)
        else:
            intersection = None
        return intersection

    def is_including(self, other):
        """

        :type other: Period
        :rtype : bool
        """
        return self.get_start_date() <= other.get_start_date() and self.get_end_date() >= other.get_end_date()

    def is_intersecting(self, other):
        """

        :type other: Period
        :rtype : bool
        """
        return self.get_start_date() < other.get_end_date() and self.get_end_date() > other.get_start_date()

    def is_connecting(self, other):
        """

        :type other: Period
        :rtype : bool
        """
        return self.get_start_date() == other.get_end_date() or self.get_end_date() == other.get_start_date()

    def grow(self, other):
        """

        :type other: Period
        :rtype : bool
        """
        grown = False
        if self.is_intersecting(other) or self.is_connecting(other):
            if other.get_start_date() < self.get_start_date():
                self.start_date = other.get_start_date()
                grown = True
            if other.get_end_date() > self.get_end_date():
                self.end_date = other.get_end_date()
                grown = True
        return grown

    @staticmethod
    def __from_iso_format(iso_string):
        """

        :type iso_string: str
        :rtype: datetime.date
        """
        iso_parts = iso_string.split('-')
        return datetime.date(int(iso_parts[0]), int(iso_parts[1]), int(iso_parts[2]))

    def __eq__(self, other):
        """

        :type other: Period
        :rtype : bool
        """
        return self.get_start_date() == other.get_start_date() and self.get_end_date() == other.get_end_date()

    def __ne__(self, other):
        """

        :type other: Period
        :rtype: bool
        """
        return not self.__eq__(other)

    def __gt__(self, other):
        """

        :type other: Period
        :rtype: bool
        """
        return self.get_start_date() > other.get_start_date() or (
            self.get_start_date() == other.get_start_date() and self.get_end_date() > other.get_end_date())

    def __ge__(self, other):
        """

        :type other: Period
        :rtype: bool
        """
        return self.__eq__(other) or self.__gt__(other)

    def __lt__(self, other):
        """

        :type other: Period
        :rtype: bool
        """
        return self.get_start_date() < other.get_start_date() or (
            self.get_start_date() == other.get_start_date() and self.get_end_date() < other.get_end_date())

    def __le__(self, other):
        """

        :type other: Period
        :rtype: bool
        """
        return self.__eq__(other) or self.__lt__(other)

    def __hash__(self):
        return self.get_start_date().__hash__() + 31 * self.get_end_date().__hash__()


class MultiPeriod:
    def __init__(self):
        self.periods = list()

    def add(self, other):
        """

        :type other: Period
        """
        added = False
        for period in self.periods:
            added = period.is_including(other)
            if added:
                break
            else:
                added = period.grow(other)
                if added:
                    self.__maintain_disjunctive_state(period)
                    break
        if not added:
            self.periods.append(Period(other.get_start_date(), other.get_end_date()))

    def get_periods(self):
        """

        :rtype : list
        """
        return sorted(self.periods)

    def __maintain_disjunctive_state(self, g):
        """

        :param g: The grown period.
        :type g: Period
        """
        trash = list()
        for p in self.periods:
            if g != p:
                if g.is_intersecting(p) or g.is_connecting(p):
                    g.grow(p)
                    trash.append(p)
        for p in trash:
            self.periods.remove(p)


class Sensor:
    def __init__(self, name, period=None):
        """

        :type name: str
        :type period: Period
        """
        self.name = name
        self.period = period

    def get_name(self):
        """

        :rtype : str
        """
        return self.name

    def get_period(self):
        """

        :rtype : Period
        """
        return self.period

    def __eq__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() == other.get_name() and self.get_period() == other.get_period()

    def __ne__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() != other.get_name() or self.get_period() != other.get_period()

    def __ge__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() >= other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() >= other.get_period())

    def __gt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() > other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() > other.get_period())

    def __le__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() <= other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() <= other.get_period())

    def __lt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() < other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() < other.get_period())

    def __hash__(self):
        return self.get_name().__hash__() * 31 + self.get_period().__hash__()


class SensorPair:
    def __init__(self, primary_sensor, secondary_sensor, production_period=None):
        """

        :type primary_sensor: Sensor
        :type secondary_sensor: Sensor
        :type production_period: Period
        :raise exceptions.ValueError: If the periods of primary and secondary sensors do not overlap.
        """
        if not primary_sensor.get_period().is_intersecting(secondary_sensor.get_period()):
            raise exceptions.ValueError, "The periods of primary and secondary sensors do not overlap."
        if not production_period is None:
            if not primary_sensor.get_period().is_intersecting(production_period):
                raise exceptions.ValueError, "The periods of primary sensor and production do not overlap."
            if not secondary_sensor.get_period().is_intersecting(production_period):
                raise exceptions.ValueError, "The periods of secondary sensor and production do not overlap."
        self.primary_sensor = primary_sensor
        self.secondary_sensor = secondary_sensor
        if primary_sensor.get_name() != secondary_sensor.get_name():
            self.name = primary_sensor.get_name() + ',' + secondary_sensor.get_name()
        else:
            self.name = primary_sensor.get_name()
        self.period = primary_sensor.get_period().get_intersection(secondary_sensor.get_period())
        if not production_period is None:
            self.period = self.period.get_intersection(production_period)

    def get_name(self):
        """

        :rtype : str
        """
        return self.name

    def get_primary(self):
        """

        :rtype : str
        """
        return self.primary_sensor.get_name()

    def get_secondary(self):
        """

        :rtype : str
        """
        return self.secondary_sensor.get_name()

    def get_period(self):
        """

        :rtype : Period
        """
        return self.period

    def __eq__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return (self.get_primary() == other.get_primary() and self.get_secondary() == other.get_secondary()) or (
            self.get_primary() == other.get_secondary() and self.get_secondary() == other.get_primary())

    def __ne__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return not self.__eq__(other)

    def __ge__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return self.__eq__(other) or self.__gt__(other)

    def __gt__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return self.__ne__(other) and (self.get_primary() > other.get_primary() or (
            self.get_primary() == other.get_primary() and self.get_secondary() > other.get_secondary()))

    def __le__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return self.__eq__(other) or self.__lt__(other)

    def __lt__(self, other):
        """

        :type other: SensorPair
        :return: bool
        """
        return self.__ne__(other) and (self.get_primary() < other.get_primary() or (
            self.get_primary() == other.get_primary() and self.get_secondary() < other.get_secondary()))

    def __hash__(self):
        if self.get_primary() < self.get_secondary():
            return (self.get_primary() + self.get_secondary()).__hash__()
        else:
            return (self.get_secondary() + self.get_primary()).__hash__()


class Job:
    def __init__(self, name, call, preconditions, postconditions, parameters):
        """

        :type name: str
        :type call: str
        :type preconditions: list
        :type postconditions: list
        :type parameters: list
        """
        self.name = name
        self.call = call
        self.preconditions = preconditions
        self.postconditions = postconditions
        self.parameters = list()
        for p in parameters:
            if isinstance(p, str):
                self.parameters.append(p)
            else:
                self.parameters.append(str(p))

    def get_name(self):
        """

        :rtype : str
        """
        return self.name

    def get_call(self):
        """

        :rtype : str
        """
        return self.call

    def get_preconditions(self):
        """

        :rtype : list
        """
        return self.preconditions

    def get_postconditions(self):
        """

        :rtype : list
        """
        return self.postconditions

    def get_parameters(self):
        """

        :rtype : list
        """
        return self.parameters


class Monitor:
    def __init__(self, preconditions, usecase, hosts, calls, log_dir, simulation):
        """

        :type preconditions: list
        :type usecase: str
        :type hosts: list
        :type calls: list
        :type log_dir: str
        :type simulation: bool
        """
        self.pm = PMonitor(preconditions, usecase, hosts, calls, log_dir=log_dir, simulation=simulation)

    def execute(self, job):
        """

        :type job: Job
        """
        self.pm.execute(job.get_call(), job.get_preconditions(), job.get_postconditions(), job.get_parameters(),
                        log_prefix=job.get_name())

    def wait_for_completion(self):
        self.pm.wait_for_completion()

    def wait_for_completion_and_terminate(self):
        self.pm.wait_for_completion_and_terminate()


# archiving rules
# mms/archive/atsr.3/v2.1/2003/01/17/ATS_TOA_1P...N1
# mms/archive/mms2/smp/atsr.3/2003/atsr.3-smp-2003-01-b.txt
# mms/archive/mms2/sub/atsr.3/2003/atsr.3-sub-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwp-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwpAn-2003-01.nc
# mms/archive/mms2/nwp/atsr.3/2003/atsr.3-nwpFc-2003-01.nc
# mms/archive/mms2/arc/atsr.3/2003/atsr.3-arc-2003-01.nc
# mms/archive/mms2/mmd/atsr.3/2003/atsr.3-mmd-2003-01.nc

class Workflow:
    def __init__(self, usecase, production_period=None):
        """

        :type usecase: str
        :type production_period: Period
        """
        self.usecase = usecase
        self.production_period = production_period
        self.samples_per_month = 300000
        self.primary_sensors = set()
        self.secondary_sensors = set()

    def get_usecase(self):
        """

        :rtype : str
        """
        return self.usecase

    def get_production_period(self):
        """

        :rtype : Period
        """
        return self.production_period

    def get_samples_per_month(self):
        """

        :rtype : int
        """
        return self.samples_per_month

    def set_samples_per_month(self, samples_per_month):
        """

        :type samples_per_month: int
        """
        self.samples_per_month = samples_per_month

    def add_primary_sensor(self, name, start_date, end_date):
        """

        :type name: str
        """
        self.primary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def add_secondary_sensor(self, name, start_date, end_date):
        """

        :type name: str
        """
        self.secondary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def run(self, mmdtype, hosts=list([('localhost', 60)]), calls=list(), log_dir='trace', with_history=False,
            simulation=False):
        """

        :type mmdtype: str
        :type hosts: list
        :type calls: list
        :type log_dir: str
        :type with_history: bool
        :type simulation: bool
        """
        if with_history:
            sampling_prefix = 'his'
        else:
            sampling_prefix = 'dum'
        m = self.__get_monitor(hosts, calls, log_dir, simulation)
        self._execute_ingest_sensor_data(m)
        m.wait_for_completion()
        self._execute_sampling(m)
        m.wait_for_completion()
        self._execute_clearing(m)
        self._execute_plotting(m, sampling_prefix)
        m.wait_for_completion()
        self._execute_ingest_coincidences(m, sampling_prefix)
        m.wait_for_completion()
        self._execute_create_sub_mmd_files(m)
        m.wait_for_completion()
        self._execute_create_nwp_mmd_files(m)
        m.wait_for_completion()
        self._execute_create_matchup_nwp_mmd_files(m)
        m.wait_for_completion()
        self._execute_create_arc_mmd_files(m)
        m.wait_for_completion()
        self._execute_ingest_sub_mmd_files(m)
        m.wait_for_completion()
        self._execute_ingest_nwp_mmd_files(m)
        m.wait_for_completion()
        self._execute_ingest_matchup_nwp_mmd_files(m)
        m.wait_for_completion()
        self._execute_ingest_arc_mmd_files(m)
        m.wait_for_completion()
        self._execute_create_final_mmd_files(m, mmdtype)
        m.wait_for_completion_and_terminate()

    def _get_primary_sensors(self):
        """

        :rtype : list
        """
        return sorted(list(self.primary_sensors), reverse=True)

    def _get_secondary_sensors(self):
        """

        :rtype : list
        """
        return sorted(list(self.secondary_sensors), reverse=True)

    def _get_sensor_pairs(self):
        """

        :rtype : list
        """
        sensor_pairs = set()
        primary_sensors = self._get_primary_sensors()
        secondary_sensors = self._get_secondary_sensors()
        if len(secondary_sensors) > 0:
            for p in primary_sensors:
                for s in secondary_sensors:
                    if p != s:
                        try:
                            sensor_pair = SensorPair(p, s, self.get_production_period())
                            sensor_pairs.add(sensor_pair)
                        except exceptions.ValueError:
                            pass
        else:
            for p in primary_sensors:
                try:
                    sensor_pair = SensorPair(p, p, self.get_production_period())
                    sensor_pairs.add(sensor_pair)
                except exceptions.ValueError:
                    pass
        return sorted(list(sensor_pairs), reverse=True)

    def _get_all_sensors_by_period(self):
        """

        :rtype : list
        """
        multi_periods = dict()
        for sensor_pair in self._get_sensor_pairs():
            sensor = sensor_pair.get_primary()
            period = sensor_pair.get_period()
            Workflow.__add_period(multi_periods, sensor, period)
            sensor = sensor_pair.get_secondary()
            Workflow.__add_period(multi_periods, sensor, period)
        sensors = list()
        for name in sorted(multi_periods.keys()):
            for period in multi_periods[name].get_periods():
                sensors.append(Sensor(name, period))
        return sensors

    def _get_primary_sensors_by_period(self):
        """

        :rtype : list
        """
        multi_periods = dict()
        for sensor_pair in self._get_sensor_pairs():
            sensor = sensor_pair.get_primary()
            period = sensor_pair.get_period()
            Workflow.__add_period(multi_periods, sensor, period)
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
        if not sensor in multi_periods:
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
        for sensor_pair in self._get_sensor_pairs():
            period = sensor_pair.get_period()
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
        self._add_inp_preconditions(preconditions)
        self._add_obs_preconditions(preconditions)
        self._add_smp_preconditions(preconditions)
        return Monitor(preconditions, self.get_usecase(), hosts, calls, log_dir, simulation)

    def __get_effective_production_period(self):
        """

        :rtype : Period
        """
        data_period = self._get_data_period()
        if data_period is None:
            return None
        production_period = self.get_production_period()
        if production_period is None:
            return data_period
        else:
            return production_period.get_intersection(data_period)

    def _add_inp_preconditions(self, preconditions):
        """

        :type preconditions: list
        :rtype : list
        """
        period = self.__get_effective_production_period()
        if period is None:
            return preconditions
        date = period.get_start_date()
        end_date = period.get_end_date()
        while date < end_date:
            preconditions.append('/inp/' + _pathformat(date))
            date = _next_month(date)
        return preconditions

    def _add_obs_preconditions(self, preconditions):
        """

        :type preconditions: list
        :rtype : list
        """
        period = self.__get_effective_production_period()
        if period is None:
            return preconditions
        pre_start_date = _prev_month(period.get_start_date())
        end_date = period.get_end_date()
        preconditions.append('/obs/' + _pathformat(pre_start_date))
        if end_date.day > 1:
            post_end_date = _next_month(end_date)
            preconditions.append('/obs/' + _pathformat(post_end_date))
        else:
            preconditions.append('/obs/' + _pathformat(end_date))
        return preconditions

    def _add_smp_preconditions(self, preconditions):
        """

        :type preconditions: list
        :rtype : list
        """
        for sensor_pair in self._get_sensor_pairs():
            name = sensor_pair.get_name()
            period = sensor_pair.get_period()
            start_date = period.get_start_date()
            end_date = period.get_end_date()
            preconditions.append('/smp/' + name + '/' + _pathformat(_prev_month(start_date)))
            if end_date.day > 1:
                preconditions.append('/smp/' + name + '/' + _pathformat(_next_month(end_date)))
            else:
                preconditions.append('/smp/' + name + '/' + _pathformat(end_date))
        return preconditions

    def _execute_ingest_sensor_data(self, monitor):
        """

        :type monitor: Monitor
        """
        period = self.__get_effective_production_period()
        date = period.get_start_date()
        end_date = period.get_end_date()
        while date < end_date:
            (year, month) = _year_month(date)
            job = Job('ingestion-start' + '-' + year + '-' + month,
                      'ingestion-start.sh',
                      ['/inp/' + _pathformat(date)],
                      ['/obs/' + _pathformat(date),
                       '/obs'],
                      [year, month, self.get_usecase()])
            monitor.execute(job)
            date = _next_month(date)

    @staticmethod
    def __compute_samples_index(name, date, m):
        """


        :type name: str
        :type date: datetime.date
        :type m: int
        :rtype : int
        """
        return abs((name.__hash__() * 31 + date.year * 31) + date.month) * m

    def _execute_sampling(self, monitor):
        """

        :type monitor: Monitor
        """
        m = self.samples_per_month

        for sensor_pair in self._get_sensor_pairs():
            name = sensor_pair.get_name()
            """:type : str"""
            period = sensor_pair.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                n = self.__compute_samples_index(name, date, m)
                job = Job('sampling-start' + '-' + year + '-' + month + '-' + name,
                          'sampling-start.sh',
                          ['/obs/' + _pathformat(_prev_month(date)),
                           '/obs/' + _pathformat(date),
                           '/obs/' + _pathformat(_next_month(date)),
                           '/obs'],
                          ['/smp/' + name + '/' + _pathformat(date),
                           '/smp'],
                          [year, month, name, m, n, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_clearing(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor_pair in self._get_sensor_pairs():
            sensor_1 = sensor_pair.get_primary()
            sensor_2 = sensor_pair.get_secondary()
            name = sensor_pair.get_name()
            period = sensor_pair.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('clearsky-start' + '-' + year + '-' + month + '-' + name,
                          'clearsky-start.sh',
                          ['/smp/' + name + '/' + _pathformat(_prev_month(date)),
                           '/smp/' + name + '/' + _pathformat(date),
                           '/smp/' + name + '/' + _pathformat(_next_month(date)),
                           '/smp'],
                          ['/clr/' + name + '/' + _pathformat(date),
                           '/clr/' + sensor_1 + '/' + _pathformat(date),
                           '/clr/' + sensor_2 + '/' + _pathformat(date),
                           '/clr'],
                          [year, month, name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_plotting(self, monitor, sampling_prefix):
        """

        :type monitor: Monitor
        :type sampling_prefix: str
        """
        for sensor in self._get_primary_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('mapplot-start' + '-' + year + '-' + month + '-' + name,
                          'mapplot-start.sh',
                          ['/clr/' + name + '/' + _pathformat(date)],
                          ['/plt/' + name + '/' + _pathformat(date)],
                          [year, month, sampling_prefix + '_' + name, 'lonlat', self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_ingest_coincidences(self, monitor, sampling_prefix):
        """

        :type monitor: Monitor
        :type sampling_prefix: str
        """
        for sensor in self._get_primary_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('coincidence-start' + '-' + year + '-' + month + '-' + name,
                          'coincidence-start.sh',
                          ['/clr/' + name + '/' + _pathformat(date),
                           '/clr'],
                          ['/con/' + name + '/' + _pathformat(date)],
                          [year, month, sampling_prefix + '_' + name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_create_sub_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('sub-start' + '-' + year + '-' + month + '-' + name,
                          'sub-start.sh',
                          ['/clr/' + name + '/' + _pathformat(date),
                           '/clr'],
                          ['/sub/' + name + '/' + _pathformat(date),
                           '/sub'],
                          [year, month, name, 'sub', self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_create_nwp_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('nwp-start' + '-' + year + '-' + month + '-' + name,
                          'nwp-start.sh',
                          ['/sub/' + name + '/' + _pathformat(date),
                           '/sub'],
                          ['/nwp/' + name + '/' + _pathformat(date),
                           '/nwp'],
                          [year, month, name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_create_matchup_nwp_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_primary_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('matchup_nwp-start' + '-' + year + '-' + month + '-' + name,
                          'matchup-nwp-start.sh',
                          ['/sub/' + name + '/' + _pathformat(date),
                           '/sub'],
                          ['/nwp/' + name + '/' + _pathformat(date),
                           '/nwp'],
                          [year, month, name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_create_arc_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('gbcs-start' + '-' + year + '-' + month + '-' + name,
                          'gbcs-start.sh',
                          ['/nwp/' + name + '/' + _pathformat(date),
                           '/nwp'],
                          ['/arc/' + name + '/' + _pathformat(date),
                           '/arc'],
                          [year, month, name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_ingest_sub_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        mmdtype = 'sub'
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('reingestion-start' + '-' + year + '-' + month + '-' + name + '-' + mmdtype,
                          'reingestion-start.sh',
                          ['/sub/' + name + '/' + _pathformat(date),
                           '/sub'],
                          ['/con/' + name + '/' + _pathformat(date)],
                          [year, month, name, mmdtype, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_ingest_nwp_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        mmdtype = 'nwp'
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('reingestion-start' + '-' + year + '-' + month + '-' + name + '-' + mmdtype,
                          'reingestion-start.sh',
                          ['/nwp/' + name + '/' + _pathformat(date),
                           '/nwp'],
                          ['/con/' + name + '/' + _pathformat(date)],
                          [year, month, name, mmdtype, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_ingest_matchup_nwp_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        for sensor in self._get_primary_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('matchup-reingestion-start' + '-' + year + '-' + month + '-' + name,
                          'matchup-reingestion-start.sh',
                          ['/nwp/' + name + '/' + _pathformat(date),
                           '/nwp'],
                          ['/con/' + name + '/' + _pathformat(date)],
                          [year, month, name, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_ingest_arc_mmd_files(self, monitor):
        """

        :type monitor: Monitor
        """
        mmdtype = 'arc'
        for sensor in self._get_all_sensors_by_period():
            name = sensor.get_name()
            period = sensor.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('reingestion-start' + '-' + year + '-' + month + '-' + name + '-' + mmdtype,
                          'reingestion-start.sh',
                          ['/arc/' + name + '/' + _pathformat(date),
                           '/arc'],
                          ['/con/' + name + '/' + _pathformat(date)],
                          [year, month, name, mmdtype, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)

    def _execute_create_final_mmd_files(self, monitor, mmdtype):
        """

        :type monitor: Monitor
        :type mmdtype: str
        """
        for sensor_pair in self._get_sensor_pairs():
            sensor_1 = sensor_pair.get_primary()
            sensor_2 = sensor_pair.get_secondary()
            name = sensor_pair.get_name()
            period = sensor_pair.get_period()
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                job = Job('mmd-start' + '-' + year + '-' + month + '-' + name + '-' + mmdtype,
                          'mmd-start.sh',
                          ['/con/' + sensor_1 + '/' + _pathformat(date),
                           '/con/' + sensor_2 + '/' + _pathformat(date)],
                          ['/mmd/' + name + '/' + _pathformat(date)],
                          [year, month, name, mmdtype, self.get_usecase()])
                monitor.execute(job)
                date = _next_month(date)


def _pathformat(date):
    """

    :type date: datetime.date
    :rtype: str
    """
    return date.isoformat()[:7].replace('-', '/')


def _prev_month(date):
    """

    :type date: datetime.date
    :rtype : datetime.date
    """
    if date.month != 1:
        return datetime.date(date.year, date.month - 1, 1)
    else:
        return datetime.date(date.year - 1, 12, 1)


def _next_month(date):
    """

    :type date: datetime.date
    :rtype : datetime.date
    """
    if date.month != 12:
        return datetime.date(date.year, date.month + 1, 1)
    else:
        return datetime.date(date.year + 1, 1, 1)


def _year_month(date):
    """

    :type date: datetime.date
    :rtype: tuple
    """
    return _pathformat(date).split('/', 1)



