__author__ = 'ralf'

import os
import unittest

from productverifier import L2P
from productverifier import ProductVerifier

from productverifier import VerificationException


class ProductVerifierTests(unittest.TestCase):
    def test_check_source_pathname(self):
        verifier = ProductVerifier('20100701003842-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-X1-v02.0-fvX1.0.nc')
        try:
            verifier._check_source_pathname()
            self.fail()
        except VerificationException:
            pass

        report = verifier.get_report()
        self.assertEquals(1, report['source_pathname_check'])

        f = open(verifier.get_source_pathname(), 'w')
        try:
            verifier._check_source_pathname()
        finally:
            f.close()
            os.remove(verifier.get_source_pathname())

        report = verifier.get_report()
        self.assertEquals(0, report['source_pathname_check'])

    def test_check_source_filename(self):
        verifier = ProductVerifier('20100701003842-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-X1-v02.0-fvX1.0.nc')
        product_type = verifier._check_source_filename()
        self.assertIsNotNone(product_type)
        self.assertIsInstance(product_type, L2P)

        report = verifier.get_report()
        self.assertEquals(0, report['source_filename_check'])

    def test_check_dataset(self):
        verifier = ProductVerifier('testdata/20100701003842-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-X1-v02.0-fvX1.0.nc')
        verifier._check_dataset(L2P())

        report = verifier.get_report()

        for key in sorted(report.keys()):
            print key, report[key]


if __name__ == '__main__':
    unittest.main()
