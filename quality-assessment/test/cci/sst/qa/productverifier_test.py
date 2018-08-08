__author__ = 'Ralf Quast'

import os
import unittest

from cci.sst.qa.productverifier import L2P
from cci.sst.qa.productverifier import ProductVerifier
from cci.sst.qa.productverifier import VerificationError
from test.cci.sst.qa.test_data_utils import TestDataUtils


class ProductVerifierTests(unittest.TestCase):
    def test_check_source_pathname(self):
        verifier = ProductVerifier('20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc')
        try:
            verifier._check_source_pathname()
            self.fail()
        except VerificationError:
            report = verifier.get_report()
            self.assertEquals(1, report['source_pathname_check'])

        ProductVerifierTests.create_new_file(verifier.get_source_pathname())
        try:
            verifier._check_source_pathname()
            report = verifier.get_report()
            self.assertEquals(0, report['source_pathname_check'])
        finally:
            os.remove(verifier.get_source_pathname())

    def test_check_source_filename(self):
        verifier = ProductVerifier('20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc')
        product_type = verifier._check_source_filename()
        self.assertIsInstance(product_type, L2P)

        report = verifier.get_report()
        self.assertEquals(0, report['source_filename_check'])

    def test_verify(self):
        self.maxDiff = None  # allows checking strings beyond the limit tb 2018-07-06
        data_dir = TestDataUtils.get_test_data_dir()

        test_product_path = os.path.join(data_dir, "20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc")
        verifier = ProductVerifier(test_product_path)
        verifier.verify()

        expected_report_path = os.path.join(data_dir, "report.json")
        expected_report = ProductVerifier.load_report(expected_report_path)
        self.assertEquals(expected_report, verifier.get_report())

    @staticmethod
    def create_new_file(pathname):
        source_file = open(pathname, 'w')
        source_file.close()


if __name__ == '__main__':
    unittest.main()
