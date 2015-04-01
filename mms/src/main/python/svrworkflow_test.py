__author__ = 'Ralf Quast'

import os
import datetime
import exceptions
import unittest

from workflow import Period
from svrworkflow import SvrWorkflow


class SvrWorkflowTests(unittest.TestCase):
    def test_get_workflow_usecase(self):
        w = SvrWorkflow('test', '1.0', '.')
        self.assertEqual('test', w.get_usecase())

    def test_add_sensors_to_workflow(self):
        w = SvrWorkflow('test', '1.0', '.')
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
        w = SvrWorkflow('test', '1.0', '.')
        w.add_sensor('AVHRR10_G', (1986, 11, 17), (1991, 9, 16))
        w.add_sensor('AVHRR11_G', (1988, 11, 8), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 16), (1998, 12, 14))

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
        self.assertEqual(Period('1986-11-17', '1991-09-16'), sensor_1.get_period())
        self.assertEqual("AVHRR11_G", sensor_2.get_name())
        self.assertEqual(Period('1988-11-08', '1994-12-31'), sensor_2.get_period())
        self.assertEqual("AVHRR12_G", sensor_3.get_name())
        self.assertEqual(Period('1991-09-16', '1998-12-14'), sensor_3.get_period())

    def test_get_data_period(self):
        w = SvrWorkflow('test', '1.0', '.')
        w.add_sensor('AVHRR10_G', (1986, 11, 17), (1991, 9, 16))
        w.add_sensor('AVHRR11_G', (1988, 11, 8), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 16), (1998, 12, 14))

        data_period = w._get_data_period()
        self.assertEqual(datetime.date(1986, 11, 17), data_period.get_start_date())
        self.assertEqual(datetime.date(1998, 12, 14), data_period.get_end_date())

    def test_run_l2p_usecase(self):
        usecase = 'l2p'
        version = 'v2.1.8'
        archive_root = '/group_workspaces/cems2/esacci_sst/output'

        w = SvrWorkflow(usecase, version, archive_root, Period('1991-01-01', '1992-01-01'))
        w.add_sensor('AVHRR10_G', (1986, 11, 17), (1991, 9, 16))
        w.add_sensor('AVHRR11_G', (1988, 11, 8), (1994, 12, 31))
        w.add_sensor('AVHRR12_G', (1991, 9, 16), (1998, 12, 14))
        w.run(log_dir='.', simulation=True)

        with open('l2p.status', 'r') as status:
            self.assertEqual('28 created, 0 running, 0 backlog, 28 processed, 0 failed\n', status.readline())
        with open('l2p.report', 'r') as report:
            self.assertEqual(28, len(report.readlines()))

        os.remove('l2p.status')
        os.remove('l2p.report')


if __name__ == '__main__':
    unittest.main()
