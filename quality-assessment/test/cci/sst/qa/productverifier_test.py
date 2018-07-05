__author__ = 'Ralf Quast'

import os
import unittest

from cci.sst.qa.productverifier import L2P
from cci.sst.qa.productverifier import ProductVerifier
from cci.sst.qa.productverifier import VerificationError


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
        verifier = ProductVerifier('test/cci/sst/qa/testdata/20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc')
        verifier.verify()

        expected_report = ProductVerifier.load_report('test/cci/sst/qa/testdata/report.json')
        self.assertEquals(expected_report, verifier.get_report())

    @staticmethod
    def create_new_file(pathname):
        source_file = open(pathname, 'w')
        source_file.close()


if __name__ == '__main__':
    unittest.main()
