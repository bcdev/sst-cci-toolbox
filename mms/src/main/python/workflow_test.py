__author__ = 'Ralf Quast'

import datetime
import exceptions
import unittest

from workflow import Period, MultiPeriod
from workflow import Sensor
from workflow import SensorPair
from workflow import Workflow


# noinspection PyProtectedMember
class WorkflowTests(unittest.TestCase):
    def test_period_construction(self):
        period_1 = Period((2007, 1, 1), '2008-01-01')
        period_2 = Period('2007-01-01', (2008, 1, 1))
        period_3 = Period((2007, 1, 1), datetime.date(2008, 1, 1))
        period_4 = Period(datetime.date(2007, 1, 1), (2008, 1, 1))
        self.assertTrue(period_1 == period_2)
        self.assertTrue(period_2 == period_3)
        self.assertTrue(period_3 == period_4)
        self.assertTrue(period_4 == period_1)

    def test_get_period_intersection(self):
        period_1 = Period('2007-01-01', '2008-01-01')
        period_2 = Period('2007-07-01', '2008-07-01')
        period_3 = Period('2007-10-01', '2007-11-01')
        period_4 = Period('2001-07-01', '2002-07-01')
        self.assertEqual(period_1, period_1.get_intersection(period_1))
        self.assertEqual(period_2, period_2.get_intersection(period_2))
        self.assertEqual(Period('2007-07-01', '2008-01-01'), period_1.get_intersection(period_2))
        self.assertEqual(Period('2007-07-01', '2008-01-01'), period_2.get_intersection(period_1))
        self.assertEqual(period_3, period_1.get_intersection(period_3))
        self.assertTrue(period_1.get_intersection(period_4) is None)

    def test_period_equality(self):
        period_1 = Period((2007, 1, 1), (2008, 1, 1))
        period_2 = Period((2007, 1, 1), (2008, 1, 1))
        self.assertTrue(period_1 == period_2)

    def test_period_inequality(self):
        period_1 = Period((2007, 1, 1), (2008, 1, 1))
        period_2 = Period((2007, 1, 1), (2009, 1, 1))
        self.assertTrue(period_1 != period_2)

    def test_multi_period(self):
        multi_period = MultiPeriod()
        period_1 = Period('2007-01-01', '2007-02-01')
        period_2 = Period('2007-02-01', '2007-03-01')
        period_3 = Period('2006-12-01', '2007-01-01')
        period_4 = Period('2006-11-01', '2007-01-01')
        period_5 = Period('2007-02-01', '2007-04-01')
        period_6 = Period('2007-05-01', '2007-06-01')
        period_7 = Period('2007-04-01', '2007-05-01')
        period_8 = Period('2007-11-01', '2007-12-01')
        period_9 = Period('2007-09-01', '2007-10-01')

        multi_period.add(period_1)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(period_1, periods[0])

        multi_period.add(period_1)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(period_1, periods[0])

        multi_period.add(period_2)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2007-01-01', '2007-03-01'), periods[0])

        multi_period.add(period_3)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2006-12-01', '2007-03-01'), periods[0])

        multi_period.add(period_4)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-03-01'), periods[0])

        multi_period.add(period_5)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-04-01'), periods[0])

        multi_period.add(period_6)
        periods = multi_period.get_periods()
        self.assertEqual(2, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-04-01'), periods[0])
        self.assertEqual(period_6, periods[1])

        multi_period.add(period_7)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-06-01'), periods[0])

        multi_period.add(period_5)
        periods = multi_period.get_periods()
        self.assertEqual(1, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-06-01'), periods[0])

        multi_period.add(period_8)
        periods = multi_period.get_periods()
        self.assertEqual(2, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-06-01'), periods[0])
        self.assertEqual(period_8, periods[1])

        multi_period.add(period_9)
        periods = multi_period.get_periods()
        self.assertEqual(3, len(periods))
        self.assertEqual(Period('2006-11-01', '2007-06-01'), periods[0])
        self.assertEqual(period_9, periods[1])
        self.assertEqual(period_8, periods[2])

    def test_get_sensor_name(self):
        sensor = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        self.assertEqual('atsr.3', sensor.get_name())

    def test_get_sensor_period(self):
        sensor = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        self.assertEqual('2007-01-01', sensor.get_period().get_start_date().isoformat())
        self.assertEqual('2008-01-01', sensor.get_period().get_end_date().isoformat())

    def test_sensor_equality(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.3', Period((2008, 1, 1), (2009, 1, 1)))
        self.assertTrue(sensor_1 == sensor_2)

    def test_sensor_inequality(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2008, 1, 1), (2009, 1, 1)))
        self.assertTrue(sensor_1 != sensor_2)

    def test_sensor_ge(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2008, 1, 1), (2009, 1, 1)))
        self.assertTrue(sensor_1 >= sensor_2)

        sensor_3 = Sensor('atsr.2', Period((2009, 1, 1), (2010, 1, 1)))
        self.assertTrue(sensor_2 >= sensor_3)
        self.assertTrue(sensor_3 >= sensor_2)

    def test_sensor_pair_construction(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2007, 7, 1), (2008, 7, 1)))
        sensor_3 = Sensor('atsr.1', Period((2008, 1, 1), (2009, 1, 1)))

        sensor_pair = SensorPair(sensor_1, sensor_2)
        self.assertEqual('atsr.3', sensor_pair.get_primary())
        self.assertEqual('atsr.2', sensor_pair.get_secondary())
        self.assertEqual(Period((2007, 7, 1), (2008, 1, 1)), sensor_pair.get_period())

        sensor_pair = SensorPair(sensor_3, sensor_2)
        self.assertEqual('atsr.1', sensor_pair.get_primary())
        self.assertEqual('atsr.2', sensor_pair.get_secondary())
        self.assertEqual(Period((2008, 1, 1), (2008, 7, 1)), sensor_pair.get_period())

        try:
            SensorPair(sensor_1, sensor_3)
            self.fail()
        except exceptions.ValueError:
            pass

        try:
            SensorPair(sensor_3, sensor_1)
        except exceptions.ValueError:
            pass

    def test_sensor_pair_equality(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2007, 7, 1), (2008, 7, 1)))

        sensor_pair_1 = SensorPair(sensor_1, sensor_2)
        sensor_pair_2 = SensorPair(sensor_2, sensor_1)
        self.assertTrue(sensor_pair_1 == sensor_pair_2)

    def test_sensor_pair_inequality(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2007, 7, 1), (2008, 7, 1)))
        sensor_3 = Sensor('atsr.1', Period((2008, 1, 1), (2009, 1, 1)))

        sensor_pair_1 = SensorPair(sensor_1, sensor_2)
        sensor_pair_2 = SensorPair(sensor_2, sensor_3)
        self.assertTrue(sensor_pair_1 != sensor_pair_2)
        self.assertTrue(sensor_pair_2 != sensor_pair_1)

    def test_sensor_pair_ge(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2007, 7, 1), (2008, 7, 1)))
        sensor_3 = Sensor('atsr.1', Period((2007, 10, 1), (2008, 10, 1)))

        sensor_pair_1 = SensorPair(sensor_1, sensor_2)
        sensor_pair_2 = SensorPair(sensor_2, sensor_3)
        sensor_pair_3 = SensorPair(sensor_1, sensor_3)
        self.assertTrue(sensor_pair_1 >= sensor_pair_2)
        self.assertTrue(sensor_pair_1 >= sensor_pair_3)

    def test_get_workflow_usecase(self):
        w = Workflow('test')
        self.assertEqual('test', w.get_usecase())

    def test_add_primary_sensors_to_workflow(self):
        w = Workflow('test')
        w.add_primary_sensor('atsr.3', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w._get_primary_sensors()))
        w.add_primary_sensor('atsr.3', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w._get_primary_sensors()))
        w.add_primary_sensor('atsr.2', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(2, len(w._get_primary_sensors()))
        w.add_primary_sensor('atsr.3', (2008, 1, 1), (2009, 1, 1))
        self.assertEqual(2, len(w._get_primary_sensors()))

    def test_add_secondary_sensors_to_workflow(self):
        w = Workflow('test')
        w.add_secondary_sensor('avhrr.n10', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w._get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n10', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w._get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n11', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(2, len(w._get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n12', (2008, 1, 1), (2009, 1, 1))
        self.assertEqual(3, len(w._get_secondary_sensors()))

    def test_get_sensor_pairs(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        sensor_pairs = w._get_sensor_pairs()
        self.assertEqual(2, len(sensor_pairs))
        self.assertEqual('avhrr.n12', sensor_pairs[0].get_primary())
        self.assertEqual('avhrr.n11', sensor_pairs[1].get_primary())

    def test_get_all_sensors_by_period(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        sensors = w._get_all_sensors_by_period()
        """:type : list"""
        self.assertEqual(3, len(sensors))
        sensor_1 = sensors[0]
        """:type : Sensor"""
        sensor_2 = sensors[1]
        """:type : Sensor"""
        sensor_3 = sensors[2]
        """:type : Sensor"""
        self.assertEqual("avhrr.n10", sensor_1.get_name())
        self.assertEqual(Period('1988-11-08', '1991-09-16'), sensor_1.get_period())
        self.assertEqual("avhrr.n11", sensor_2.get_name())
        self.assertEqual(Period('1988-11-08', '1994-12-31'), sensor_2.get_period())
        self.assertEqual("avhrr.n12", sensor_3.get_name())
        self.assertEqual(Period('1991-09-16', '1994-12-31'), sensor_3.get_period())

    def test_get_primary_sensors_by_period(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        sensors = w._get_primary_sensors_by_period()
        """:type : list"""
        self.assertEqual(2, len(sensors))
        sensor_1 = sensors[0]
        """:type : Sensor"""
        sensor_2 = sensors[1]
        """:type : Sensor"""
        self.assertEqual("avhrr.n11", sensor_1.get_name())
        self.assertEqual(Period('1988-11-08', '1991-09-16'), sensor_1.get_period())
        self.assertEqual("avhrr.n12", sensor_2.get_name())
        self.assertEqual(Period('1991-09-16', '1994-12-31'), sensor_2.get_period())

    def test_get_data_period(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        data_period = w._get_data_period()
        self.assertEqual(datetime.date(1988, 11, 8), data_period.get_start_date())
        self.assertEqual(datetime.date(1994, 12, 31), data_period.get_end_date())

    def test_get_inp_preconditions_for_one_month(self):
        w = Workflow('test', Period('1991-01-01', '1991-02-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(1, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])

    def test_get_inp_preconditions_for_one_month_odd(self):
        w = Workflow('test', Period('1991-01-02', '1991-02-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/02', preconditions[1])

    def test_get_inp_preconditions_for_one_month_plus_one_day(self):
        w = Workflow('test', Period('1991-01-01', '1991-02-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/02', preconditions[1])

    def test_get_inp_preconditions_for_one_month_minus_one_day(self):
        w = Workflow('test', Period('1991-01-02', '1991-02-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(1, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])

    def test_get_inp_preconditions_for_all_years(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(74, len(preconditions))
        self.assertEqual('/inp/1988/11', preconditions[0])
        self.assertEqual('/inp/1994/12', preconditions[73])

    def test_get_input_preconditions_for_one_year(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(12, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/12', preconditions[11])

    def test_get_input_preconditions_for_one_year_odd(self):
        w = Workflow('test', Period('1991-01-02', '1992-01-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(13, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1992/01', preconditions[12])

    def test_get_input_preconditions_for_one_year_plus_one_day(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(13, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1992/01', preconditions[12])

    def test_get_inp_preconditions_for_one_year_minus_one_day(self):
        w = Workflow('test', Period('1991-01-02', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = []
        preconditions = w._add_inp_preconditions(preconditions)
        self.assertEqual(12, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/12', preconditions[11])

    def test_add_obs_preconditions_for_all_years(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = list()
        preconditions = w._add_obs_preconditions(preconditions)
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/obs/1988/10', preconditions[0])
        self.assertEqual('/obs/1995/01', preconditions[1])

    def test_add_obs_preconditions_for_one_year(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = list()
        preconditions = w._add_obs_preconditions(preconditions)
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/obs/1990/12', preconditions[0])
        self.assertEqual('/obs/1992/01', preconditions[1])

    def test_add_obs_preconditions_for_one_year_and_one_day(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = list()
        preconditions = w._add_obs_preconditions(preconditions)
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/obs/1990/12', preconditions[0])
        self.assertEqual('/obs/1992/02', preconditions[1])

    def test_add_smp_preconditions_for_all_years(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = list()
        preconditions = w._add_smp_preconditions(preconditions)
        self.assertEqual(4, len(preconditions))
        self.assertEqual('/smp/avhrr.n12,avhrr.n11/1991/08', preconditions[0])
        self.assertEqual('/smp/avhrr.n12,avhrr.n11/1995/01', preconditions[1])
        self.assertEqual('/smp/avhrr.n11,avhrr.n10/1988/10', preconditions[2])
        self.assertEqual('/smp/avhrr.n11,avhrr.n10/1991/10', preconditions[3])

    def test_add_smp_preconditions_for_one_year(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = list()
        preconditions = w._add_smp_preconditions(preconditions)
        self.assertEqual(4, len(preconditions))
        self.assertEqual('/smp/avhrr.n12,avhrr.n11/1991/08', preconditions[0])
        self.assertEqual('/smp/avhrr.n12,avhrr.n11/1992/01', preconditions[1])
        self.assertEqual('/smp/avhrr.n11,avhrr.n10/1990/12', preconditions[2])
        self.assertEqual('/smp/avhrr.n11,avhrr.n10/1991/10', preconditions[3])

    def test_run(self):
        w = Workflow('test', Period('1991-01-01', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        hosts = [('localhost', 60)]
        calls = [('ingestion-start.sh', 30),
                 ('sampling-start.sh', 30),
                 ('clearsky-start.sh', 30),
                 ('mmd-start.sh', 30),
                 ('coincidence-start.sh', 30),
                 ('nwp-start.sh', 30),
                 ('matchup-nwp-start.sh', 30),
                 ('gbcs-start.sh', 30),
                 ('matchup-reingestion-start.sh', 30),
                 ('reingestion-start.sh', 30)]
        mmd_type = 'mmd'
        w.run(mmd_type, hosts, calls, simulation=True)


if __name__ == '__main__':
    unittest.main()