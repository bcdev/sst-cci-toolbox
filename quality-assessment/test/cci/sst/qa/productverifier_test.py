__author__ = 'Ralf Quast'

import os
import unittest

from cci.sst.qa.productverifier import ProductVerifier
from test.cci.sst.qa.test_data_utils import TestDataUtils


class ProductVerifierTests(unittest.TestCase):

    def test_verify_l2p_v1_2(self):
        self.maxDiff = None  # allows checking strings beyond the limit tb 2018-07-06
        data_dir = TestDataUtils.get_test_data_dir()

        test_product_path = os.path.join(data_dir, "20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc")
        verifier = ProductVerifier(test_product_path)
        verifier.verify()

        expected_report_path = os.path.join(data_dir, "report_l2p_v01_2.json")
        expected_report = ProductVerifier.load_report(expected_report_path)
        self.assertEqual(expected_report, verifier.get_report())

    def test_verify_l2p_v2_0(self):
        self.maxDiff = None  # allows checking strings beyond the limit tb 2018-08-10
        data_dir = TestDataUtils.get_test_data_dir()

        test_product_path = os.path.join(data_dir, "20050606101532-ESACCI-L2P_GHRSST-SSTskin-AATSR-CDR2.0-v02.0-fv01.0.nc")
        verifier = ProductVerifier(test_product_path)
        verifier.verify()

        expected_report_path = os.path.join(data_dir, "report_l2p_v02_0.json")
        expected_report = ProductVerifier.load_report(expected_report_path)
        self.assertEqual(expected_report, verifier.get_report())

    def test_verify_l3u_v2_0(self):
        self.maxDiff = None  # allows checking strings beyond the limit tb 2018-08-10
        data_dir = TestDataUtils.get_test_data_dir()

        test_product_path = os.path.join(data_dir, "20011114100024-ESACCI-L3U_GHRSST-SSTskin-ATSR2-CDR2.0-v02.0-fv01.0.nc")
        verifier = ProductVerifier(test_product_path)
        verifier.verify()

        expected_report_path = os.path.join(data_dir, "report_l3u_v02_0.json")
        expected_report = ProductVerifier.load_report(expected_report_path)
        self.assertEqual(expected_report, verifier.get_report())

    @staticmethod
    def create_new_file(pathname):
        source_file = open(pathname, 'w')
        source_file.close()


if __name__ == '__main__':
    unittest.main()
