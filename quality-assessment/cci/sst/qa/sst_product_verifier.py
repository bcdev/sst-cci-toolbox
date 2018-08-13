import os
import re

from netCDF4._netCDF4 import Dataset

from cci.sst.qa.product_type import L2P, L3U, L4
from cci.sst.qa.verification_error import VerificationError


class SstProductVerifier:

    def __init__(self, report):
        self.report = report
        self.filename_patterns = {
            '[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc': L2P(),
            '[0-9]{14}-ESACCI-L3U_GHRSST-.*\\.nc': L3U(),
            '[0-9]{14}-ESACCI-L4_GHRSST-.*\\.nc': L4(),
        }

    def check_source_pathname(self, source_pathname):
        ok = os.path.isfile(source_pathname)
        if ok:
            self.report['source_pathname_check'] = float(0)
        else:
            self.report['source_pathname_check'] = float(1)
            self.report['source_pathname_check_failed_for'] = source_pathname
            raise VerificationError

    def check_source_filename(self, source_pathname):
        """

        :rtype : ProductType
        """
        product_type = None

        filename = os.path.basename(source_pathname)
        for p, t in self.filename_patterns.items():
            if re.match(p, filename):
                product_type = t
                break

        if product_type is not None:
            self.report['source_filename_check'] = float(0)
            return product_type
        else:
            self.report['source_filename_check'] = float(1)
            filename = os.path.basename(source_pathname)
            self.report['source_filename_check_failed_for'] = filename
            raise VerificationError

    def check_product_can_be_opened(self, source_pathname):
        try:
            dataset = Dataset(source_pathname)
            dataset.set_auto_maskandscale(False)
            self.report['product_can_be_opened_check'] = float(0)
            return dataset
        except:
            self.report['product_can_be_opened_check'] = float(1)
            filename = os.path.basename(source_pathname)
            self.report['product_can_be_opened_check_failed_for'] = filename
            raise VerificationError
