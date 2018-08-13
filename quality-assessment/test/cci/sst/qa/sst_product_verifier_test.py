import unittest

import os

from cci.sst.qa.sst_product_verifier import SstProductVerifier
from cci.sst.qa.verification_error import VerificationError
from cci.sst.qa.product_type import L2P


class SstProductVerifierTest(unittest.TestCase):

    test_path = '20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc'

    def test_check_source_pathname_success(self):
        report = {}
        verifier = SstProductVerifier(report)
        try:
            verifier.check_source_pathname(self.test_path)
            self.fail()
        except VerificationError:
            self.assertEqual(1, report['source_pathname_check'])

    def test_check_source_pathname_error(self):
        report = {}
        verifier = SstProductVerifier(report)
        self._create_new_file(self.test_path)
        try:
            verifier.check_source_pathname(self.test_path)
            self.assertEqual(0, report['source_pathname_check'])
        finally:
            os.remove(self.test_path)

    def test_check_source_filename(self):
        report = {}
        verifier = SstProductVerifier(report)
        product_type = verifier.check_source_filename('20100505121116-ESACCI-L2P_GHRSST-SSTskin-AVHRRMTA_G-EXP1.2-v02.0-fv1.0.nc')
        self.assertIsInstance(product_type, L2P)

        self.assertEqual(0, report['source_filename_check'])

    @staticmethod
    def _create_new_file(pathname):
        source_file = open(pathname, 'w')
        source_file.close()