__author__ = 'Ralf Quast'

import datetime
import unittest

from workflow import Period

from workflow import Sensor
from workflow import SensorPair
from workflow import Workflow


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

    def test_period_equality(self):
        period_1 = Period((2007, 1, 1), (2008, 1, 1))
        period_2 = Period((2007, 1, 1), (2008, 1, 1))
        self.assertTrue(period_1 == period_2)

    def test_period_inequality(self):
        period_1 = Period((2007, 1, 1), (2008, 1, 1))
        period_2 = Period((2007, 1, 1), (2009, 1, 1))
        self.assertTrue(period_1 != period_2)

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

        sensor_pair = SensorPair(sensor_1, sensor_3)
        self.assertEqual('atsr.3', sensor_pair.get_primary())
        self.assertEqual('atsr.1', sensor_pair.get_secondary())
        self.assertTrue(sensor_pair.get_period() is None)

        sensor_pair = SensorPair(sensor_3, sensor_1)
        self.assertEqual('atsr.1', sensor_pair.get_primary())
        self.assertEqual('atsr.3', sensor_pair.get_secondary())
        self.assertTrue(sensor_pair.get_period() is None)

        sensor_pair = SensorPair(sensor_3, sensor_2)
        self.assertEqual('atsr.1', sensor_pair.get_primary())
        self.assertEqual('atsr.2', sensor_pair.get_secondary())
        self.assertEqual(Period((2008, 1, 1), (2008, 7, 1)), sensor_pair.get_period())

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
        sensor_pair_2 = SensorPair(sensor_1, sensor_3)
        self.assertTrue(sensor_pair_1 != sensor_pair_2)

    def test_sensor_pair_ge(self):
        sensor_1 = Sensor('atsr.3', Period((2007, 1, 1), (2008, 1, 1)))
        sensor_2 = Sensor('atsr.2', Period((2007, 7, 1), (2008, 7, 1)))
        sensor_3 = Sensor('atsr.1', Period((2008, 1, 1), (2009, 1, 1)))

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
        self.assertEqual(1, len(w.get_primary_sensors()))
        w.add_primary_sensor('atsr.3', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w.get_primary_sensors()))
        w.add_primary_sensor('atsr.2', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(2, len(w.get_primary_sensors()))
        w.add_primary_sensor('atsr.3', (2008, 1, 1), (2009, 1, 1))
        self.assertEqual(2, len(w.get_primary_sensors()))

    def test_add_secondary_sensors_to_workflow(self):
        w = Workflow('test')
        w.add_secondary_sensor('avhrr.n10', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w.get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n10', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w.get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n11', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(2, len(w.get_secondary_sensors()))
        w.add_secondary_sensor('avhrr.n12', (2008, 1, 1), (2009, 1, 1))
        self.assertEqual(3, len(w.get_secondary_sensors()))

    def test_get_sensor_pairs(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        sensor_pairs = w.get_sensor_pairs()
        self.assertEqual(2, len(sensor_pairs))
        self.assertEqual('avhrr.n10', sensor_pairs[0].get_primary())
        self.assertEqual('avhrr.n11', sensor_pairs[1].get_primary())

    def test_get_data_period(self):
        w = Workflow('test')
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        data_period = w.get_data_period()
        self.assertEqual(datetime.date(1988, 11, 8), data_period.get_start_date())
        self.assertEqual(datetime.date(1994, 12, 31), data_period.get_end_date())

    def test_get_preconditions_for_one_month(self):
        w = Workflow('test', Period('1991-01-01', '1991-02-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(1, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])

    def test_get_preconditions_for_one_month_odd(self):
        w = Workflow('test', Period('1991-01-02', '1991-02-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/02', preconditions[1])

    def test_get_preconditions_for_one_month_plus_one_day(self):
        w = Workflow('test', Period('1991-01-01', '1991-02-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(2, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/02', preconditions[1])

    def test_get_preconditions_for_one_month_minus_one_day(self):
        w = Workflow('test', Period('1991-01-02', '1991-02-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(1, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])

    def test_get_preconditions_for_one_year(self):
        w = Workflow('mms11', Period('1991-01-01', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(12, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/12', preconditions[11])

    def test_get_preconditions_for_one_year_odd(self):
        w = Workflow('mms11', Period('1991-01-02', '1992-01-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(13, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1992/01', preconditions[12])

    def test_get_preconditions_for_one_year_plus_one_day(self):
        w = Workflow('mms11', Period('1991-01-01', '1992-01-02'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(13, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1992/01', preconditions[12])

    def test_get_preconditions_for_one_year_minus_one_day(self):
        w = Workflow('mms11', Period('1991-01-02', '1992-01-01'))
        w.add_primary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_primary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_primary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))
        w.add_secondary_sensor('avhrr.n10', (1986, 11, 17), (1991, 9, 16))
        w.add_secondary_sensor('avhrr.n11', (1988, 11, 8), (1994, 12, 31))
        w.add_secondary_sensor('avhrr.n12', (1991, 9, 16), (1998, 12, 14))

        preconditions = w.get_preconditions()
        self.assertEqual(12, len(preconditions))
        self.assertEqual('/inp/1991/01', preconditions[0])
        self.assertEqual('/inp/1991/12', preconditions[11])



if __name__ == '__main__':
    unittest.main()