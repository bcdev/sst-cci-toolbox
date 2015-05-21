__author__ = 'Gerrit Holl'
# This is effectively forked from svrworkflow_test.py

import os
import datetime
import exceptions
import unittest

from workflow import Period
from regridworkflow import RegridWorkflow


class RegridWorkflowTests(unittest.TestCase):
    def test_get_workflow_usecase(self):
        w = RegridWorkflow('test', '1.0', '.', '.')
        self.assertEqual('test', w.get_usecase())

    def test_add_sensors_to_workflow(self):
        w = RegridWorkflow('test', '1.0', '.', '.')
        w.add_sensor('AATSR', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(1, len(w._get_sensors()))

        try:
            w.add_sensor('AATSR', (2007, 1, 1), (2008, 1, 1))
            self.fail()
        except exceptions.ValueError:
            pass

        try:
            w.add_sensor('AATSR', (2007, 7, 1), (2008, 1, 1))
            self.fail()
        except exceptions.ValueError:
            pass

        w.add_sensor('ATSR2', (2007, 1, 1), (2008, 1, 1))
        self.assertEqual(2, len(w._get_sensors()))
        w.add_sensor('AATSR', (2008, 1, 1), (2009, 1, 1))
        self.assertEqual(3, len(w._get_sensors()))

    def test_get_sensors_by_period(self):
        w = RegridWorkflow('test', '1.0', '.', '.')
        w.add_sensor('AVHRR10_G', (1986, 11, 1), (1991, 9, 30))
        w.add_sensor('AVHRR11_G', (1988, 11, 1), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 1), (1998, 12, 31))

        sensors = w._get_sensors_by_period()
        """:type : list"""
        self.assertEqual(3, len(sensors))
        sensor_1 = sensors[0]
        """:type : Sensor"""
        sensor_2 = sensors[1]
        """:type : Sensor"""
        sensor_3 = sensors[2]
        """:type : Sensor"""
        self.assertEqual("AVHRR10_G", sensor_1.get_name())
        self.assertEqual(Period('1986-11-01', '1991-09-30'), sensor_1.get_period())
        self.assertEqual("AVHRR11_G", sensor_2.get_name())
        self.assertEqual(Period('1988-11-01', '1994-12-31'), sensor_2.get_period())
        self.assertEqual("AVHRR12_G", sensor_3.get_name())
        self.assertEqual(Period('1991-09-01', '1998-12-31'), sensor_3.get_period())

    def test_get_data_period(self):
        w = RegridWorkflow('test', '1.0', '.', '.')
        w.add_sensor('AVHRR10_G', (1986, 11, 17), (1991, 9, 16))
        w.add_sensor('AVHRR11_G', (1988, 11, 8), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 16), (1998, 12, 14))

        data_period = w._get_data_period()
        self.assertEqual(datetime.date(1986, 11, 17), data_period.get_start_date())
        self.assertEqual(datetime.date(1998, 12, 14), data_period.get_end_date())

    def test_run_regrid_usecase(self):
        usecase = 'regrid'
        #version = 'v2.1.8'
        archive_root = "/neodc/esacci_sst/data/lt/"
        target_root = "/group_workspaces/cems2/esacci_sst/scratch/2015_05_regridded_sst"

        w = RegridWorkflow(usecase, archive_root, target_root, Period('1991-01-01', '1992-01-01'))
        w.add_sensor('AVHRR10_G', (1986, 11, 1), (1991, 9, 16))
        w.add_sensor('AVHRR11_G', (1988, 11, 1), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 1), (1998, 12, 14))
        w.run(log_dir='.', simulation=True)

        with open('regrid.status', 'r') as status:
            self.assertEqual('25 created, 0 running, 0 backlog, 25 processed, 0 failed\n', status.readline())
        with open('regrid.report', 'r') as report:
            self.assertEqual(25, len(report.readlines()))

        os.remove('regrid.status')
        os.remove('regrid.report')


if __name__ == '__main__':
    unittest.main()
