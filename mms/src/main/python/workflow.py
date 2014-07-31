__author__ = 'Ralf Quast'

import datetime
import exceptions


class Period:
    def __init__(self, start_date, end_date):
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
            self.end_date = b
        else:
            raise exceptions.RuntimeError, "The start date must be less than the end date"

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

    def __from_iso_format(self, iso_string):
        iso_parts = iso_string.split('-')
        return datetime.date(int(iso_parts[0]), int(iso_parts[1]), int(iso_parts[2]))

    def __eq__(self, other):
        return self.get_start_date() == other.get_start_date() and self.get_end_date() == other.get_end_date()

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return self.get_start_date().__hash__() + 17 * self.get_end_date().__hash__()


class Sensor:
    def __init__(self, name, period=None):
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
        return self.get_name() == other.get_name()

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return self.get_name().__hash__()

    def __ge__(self, other):
        return self.get_name() >= other.get_name()


class SensorPair:
    def __init__(self, primary_sensor, secondary_sensor):
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
        return (self.get_primary() == other.get_primary() and self.get_secondary() == other.get_secondary()) or (
            self.get_primary() == other.get_secondary() and self.get_secondary() == other.get_primary())

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        if self.get_primary() < self.get_secondary():
            return (self.get_primary() + self.get_secondary()).__hash__()
        else:
            return (self.get_secondary() + self.get_primary()).__hash__()

    def __ge__(self, other):
        return self == other or self.get_primary() > other.get_primary() or (
            self.get_primary() == other.get_primary() and self.get_secondary() > other.get_secondary())


class Workflow:
    def __init__(self, usecase, production_period=None):
        self.usecase = usecase
        self.production_period = production_period
        self.primary_sensors = set()
        self.secondary_sensors = set()

    def add_primary_sensor(self, name, start_date, end_date):
        self.primary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def add_secondary_sensor(self, name, start_date, end_date):
        self.secondary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def get_primary_sensors(self):
        """

        :rtype : list
        """
        return sorted(list(self.primary_sensors))

    def get_secondary_sensors(self):
        """

        :rtype : list
        """
        return sorted(list(self.secondary_sensors))

    def get_usecase(self):
        """

        :rtype : str
        """
        return self.usecase

    def get_sensor_pairs(self):
        """

        :rtype : list
        """
        sensor_pairs = set()
        primary_sensors = self.get_primary_sensors()
        secondary_sensors = self.get_secondary_sensors()
        for p in primary_sensors:
            for s in secondary_sensors:
                if p != s:
                    sensor_pair = SensorPair(p, s)
                    if not (sensor_pair.get_period() is None):
                        sensor_pairs.add(sensor_pair)
        return sorted(list(sensor_pairs))

    def get_sensor_data_period(self):
        """

        :rtype : Period
        """
        sensor_pairs = self.get_sensor_pairs()
        start_date = datetime.date.max
        end_date = datetime.date.min
        for sensor_pair in sensor_pairs:
            period = sensor_pair.get_period()
            if period.get_start_date() < start_date:
                start_date = period.get_start_date()
            if period.get_end_date() > end_date:
                end_date = period.get_end_date()
        if start_date < end_date:
            return Period(start_date, end_date)
        else:
            return None

    def get_preconditions(self):
        pass


