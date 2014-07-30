__author__ = 'Ralf Quast'

import datetime


class Period:
    def __init__(self, start_date, end_date):
        if isinstance(start_date, datetime.date):
            self.start_date = start_date
        elif isinstance(start_date, basestring):
            self.start_date = self.__from_iso_format(start_date)
        else:
            self.start_date = datetime.date(start_date[0], start_date[1], start_date[2])
        if isinstance(end_date, datetime.date):
            self.end_date = end_date
        elif isinstance(end_date, basestring):
            self.end_date = self.__from_iso_format(end_date)
        else:
            self.end_date = datetime.date(end_date[0], end_date[1], end_date[2])

    def get_start_date(self):
        return self.start_date

    def get_end_date(self):
        return self.end_date

    def get_intersection(self, other):
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
        return self.name

    def get_period(self):
        return self.period

    def __eq__(self, other):
        return self.get_name() == other.get_name()

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return self.get_name().__hash__()


class SensorPair:
    def __init__(self, primary_sensor, secondary_sensor):
        self.primary_sensor = primary_sensor
        self.secondary_sensor = secondary_sensor
        self.period = primary_sensor.get_period().get_intersection(secondary_sensor.get_period())

    def get_primary(self):
        return self.primary_sensor.get_name()

    def get_secondary(self):
        return self.secondary_sensor.get_name()

    def get_period(self):
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


class Workflow:
    def __init__(self, usecase):
        self.usecase = usecase
        self.primary_sensors = set()
        self.secondary_sensors = set()
        self.years = set()

    def add_year(self, year):
        self.years.add(year)

    def get_years(self):
        return self.years

    def add_primary_sensor(self, name, start_date, end_date):
        self.primary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def get_primary_sensors(self):
        return self.primary_sensors

    def add_secondary_sensor(self, name, start_date, end_date):
        self.secondary_sensors.add(Sensor(name, Period(start_date, end_date)))

    def get_secondary_sensors(self):
        return self.secondary_sensors

    def get_usecase(self):
        return self.usecase

    def get_sensor_pairs(self):
        sensor_pairs = set()
        for p in self.primary_sensors:
            for s in self.secondary_sensors:
                if p != s:
                    sensor_pair = SensorPair(p, s)
                    if not (sensor_pair.get_period() is None):
                        sensor_pairs.add(sensor_pair)
        return sensor_pairs

