__author__ = 'Ralf Quast'

import datetime
import exceptions

from pmonitor import PMonitor


class Period:
    def __init__(self, start_date, end_date):
        """

        :raise exceptions.RuntimeError: if the start date is not less than the end date
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
            raise exceptions.RuntimeError, "The start date must be less than the end date."

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
        if self.get_start_date() >= other.get_end_date():
            return None
        if other.get_start_date() >= self.get_end_date():
            return None
        start_date = self.get_start_date()
        if start_date < other.get_start_date():
            start_date = other.get_start_date()
        end_date = self.get_end_date()
        if end_date > other.get_end_date():
            end_date = other.get_end_date()
        return Period(start_date, end_date)

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
        return self.get_start_date().__hash__() + 17 * self.get_end_date().__hash__()


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
            self.periods.append(other)

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
        return self.get_name() == other.get_name()

    def __ne__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() != other.get_name()

    def __ge__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() >= other.get_name()

    def __gt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() > other.get_name()

    def __le__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() <= other.get_name()

    def __lt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() < other.get_name()

    def __hash__(self):
        return self.get_name().__hash__()


class SensorPair:
    def __init__(self, primary_sensor, secondary_sensor):
        """

        :type primary_sensor: Sensor
        :type secondary_sensor: Sensor
        """
        self.primary_sensor = primary_sensor
        self.secondary_sensor = secondary_sensor
        self.period = primary_sensor.get_period().get_intersection(secondary_sensor.get_period())

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


class Workflow:
    def __init__(self, usecase, production_period=None, simulation=False):
        """

        :type usecase: str
        :type production_period: Period
        :type simulation: bool
        """
        self.usecase = usecase
        self.production_period = production_period
        self.simulation = simulation
        self.samples_per_month = 5000000
        self.samples_skip = 0
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

    def is_simulation(self):
        """

        :rtype : bool
        """
        return self.simulation

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

    def get_samples_skip(self):
        """

        :rtype : int
        """
        return self.samples_skip

    def set_samples_skip(self, samples_skip):
        """

        :type samples_skip: int
        """
        self.samples_skip = samples_skip

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
        for p in primary_sensors:
            for s in secondary_sensors:
                if p != s:
                    sensor_pair = SensorPair(p, s)
                    if not (sensor_pair.get_period() is None):
                        sensor_pairs.add(sensor_pair)
        return sorted(list(sensor_pairs), reverse=True)

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

    def _get_data_periods_by_sensor(self):
        data_periods = dict
        sensor_pairs = self._get_sensor_pairs()
        for sensor_pair in sensor_pairs:
            sensor = sensor_pair.get_primary()
            if not sensor in data_periods:
                data_periods[sensor] = list()
            new_period = sensor_pair.get_period()
            for period in data_periods[sensor]:
                pass

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
        :return: list
        """
        production_period = self.__get_effective_production_period()
        if production_period is None:
            return preconditions
        pre_start_date = _prev_month(production_period.get_start_date())
        end_date = production_period.get_end_date()
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
        :return: list
        """
        production_period = self.get_production_period()
        for sensor_pair in self._get_sensor_pairs():
            if production_period is None:
                period = sensor_pair.get_period()
            else:
                period = sensor_pair.get_period().get_intersection(production_period)
            pre_start_date = _prev_month(period.get_start_date())
            end_date = period.get_end_date()
            preconditions.append('/smp/' + sensor_pair.get_primary() + '/' + _pathformat(pre_start_date))
            if end_date.day > 1:
                post_end_date = _next_month(end_date)
                preconditions.append('/smp/' + sensor_pair.get_primary() + '/' + _pathformat(post_end_date))
            else:
                preconditions.append('/smp/' + sensor_pair.get_primary() + '/' + _pathformat(end_date))
        return preconditions

    def _get_monitor(self, hosts, types, log_dir='trace'):
        """

        :type hosts: list
        :type types: list
        :type log_dir: str
        :rtype : PMonitor
        """
        preconditions = list()
        self._add_inp_preconditions(preconditions)
        self._add_obs_preconditions(preconditions)
        self._add_smp_preconditions(preconditions)
        if self.is_simulation():
            return PMonitor(preconditions, self.get_usecase(), hosts, types, logdir=log_dir, simulation=True)
        else:
            return PMonitor(preconditions, self.get_usecase(), hosts, types, logdir=log_dir)

    def _execute_ingestion(self, monitor):
        """

        :type monitor: PMonitor
        """
        period = self.__get_effective_production_period()
        date = period.get_start_date()
        end_date = period.get_end_date()
        while date < end_date:
            (year, month) = _year_month(date)
            monitor.execute('ingestion-start.sh',
                            ['/inp/' + year + '/' + month],
                            ['/obs/' + year + '/' + month],
                            [year, month, self.get_usecase()])
            date = _next_month(date)

    def _execute_sampling(self, monitor):
        """

        :type monitor: PMonitor
        """
        samples_per_month = self.samples_per_month
        skip = self.samples_skip
        production_period = self.get_production_period()
        for sensor_pair in self._get_sensor_pairs():
            sensor = sensor_pair.get_primary()
            if production_period is None:
                period = sensor_pair.get_period()
            else:
                period = sensor_pair.get_period().get_intersection(production_period)
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                monitor.execute('sampling-start.sh',
                                ['/obs/' + _pathformat(_prev_month(date)),
                                 '/obs/' + _pathformat(date),
                                 '/obs/' + _pathformat(_next_month(date))],
                                ['/smp/' + sensor + '/' + _pathformat(date)],
                                [year, month, sensor, str(samples_per_month), str(skip), self.get_usecase()])
                skip += samples_per_month
                date = _next_month(date)

    def _execute_clearing(self, monitor):
        """

        :param monitor: PMonitor
        """
        production_period = self.get_production_period()
        for sensor_pair in self._get_sensor_pairs():
            sensor = sensor_pair.get_primary()
            if production_period is None:
                period = sensor_pair.get_period()
            else:
                period = sensor_pair.get_period().get_intersection(production_period)
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                monitor.execute('clearsky-start.sh',
                                ['/smp/' + sensor + '/' + _pathformat(_prev_month(date)),
                                 '/smp/' + sensor + '/' + _pathformat(date),
                                 '/smp/' + sensor + '/' + _pathformat(_next_month(date))],
                                ['/clr/' + sensor + '/' + _pathformat(date)],
                                [year, month, sensor, self.get_usecase()])
                monitor.execute('mapplot-start.sh',
                                ['/clr/' + sensor + '/' + _pathformat(date)],
                                ['/plt/' + sensor + '/' + _pathformat(date)],
                                [year, month, 'dum_' + sensor, 'lonlat', self.get_usecase()])
                date = _next_month(date)

    def _execute_add_coincidences(self, monitor):
        production_period = self.get_production_period()
        for sensor_pair in self._get_sensor_pairs():
            sensor = sensor_pair.get_primary()
            if production_period is None:
                period = sensor_pair.get_period()
            else:
                period = sensor_pair.get_period().get_intersection(production_period)
            date = period.get_start_date()
            end_date = period.get_end_date()
            while date < end_date:
                (year, month) = _year_month(date)
                monitor.execute('coincidence-start.sh',
                                ['/clr/' + sensor + '/' + _pathformat(date)],
                                ['/con/' + sensor + '/' + _pathformat(date)],
                                [year, month, sensor, 'dum', self.get_usecase()])
                date = _next_month(date)

    def _execute_create_sub_mmd_files(self, monitor):
        """

        :param monitor: PMonitor
        """
        pass


def _pathformat(date):
    """

    :type date: datetime.date
    :return: str
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



