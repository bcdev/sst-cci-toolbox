from cci.sst.qa.product_type import L2P, L3U, L4
from cci.sst.qa.sst_product_verifier import SstProductVerifier
from cci.sst.qa.verification_error import VerificationError

__author__ = 'Ralf Quast'

import json
import os
from time import gmtime, strftime

import numpy as np


class ProductVerifier:
    def __init__(self, source_pathname, report_pathname=None):
        """

        :type source_pathname: str
        :type report_pathname: str
        """
        self.source_pathname = source_pathname
        self.report_pathname = report_pathname
        self.report = {}
        self.filename_patterns = {
            '[0-9]{14}-ESACCI-L2P_GHRSST-.*\\.nc': L2P(),
            '[0-9]{14}-ESACCI-L3U_GHRSST-.*\\.nc': L3U(),
            '[0-9]{14}-ESACCI-L4_GHRSST-.*\\.nc': L4(),
        }

    def get_source_pathname(self):
        """

        :rtype : str
        """
        return self.source_pathname

    def get_report_pathname(self):
        """

        :rtype : str
        """
        return self.report_pathname

    def get_report(self):
        return self.report

    def verify(self):
        try:
            verifier = SstProductVerifier(self.report, self.source_pathname)

            print(ProductVerifier.get_current_time(), 'checking source pathname')
            verifier.check_source_pathname()

            print(ProductVerifier.get_current_time(), 'checking source filename')
            product_type = verifier.check_source_filename()

            print(ProductVerifier.get_current_time(), 'checking dataset can be opened')
            dataset = verifier.check_product_can_be_opened()

            version = verifier.check_product_version(dataset)
            product_type.set_version(version)

            print(ProductVerifier.get_current_time(), 'checking dataset')
            verifier.check_dataset(dataset, product_type)
        except VerificationError:
            print('Verification error:', self.source_pathname)
        finally:
            ProductVerifier.ensure_correct_integer_types(self.report)
            ProductVerifier.dump_report(self.report, self.report_pathname)

    @staticmethod
    def ensure_correct_integer_types(report):
        """

        :type report: Dictionary
        """
        for key, value in report.items():
            if isinstance(value, np.int64):
                report[key] = value.astype(int)
            if isinstance(value, np.int32):
                report[key] = value.astype(int)

    @staticmethod
    def load_report(report_pathname):
        """

        :type report_pathname: str
        :rtype : dict
        """
        report_file = open(report_pathname, 'r')
        try:
            return json.load(report_file)
        finally:
            report_file.close()

    @staticmethod
    def dump_report(report, report_pathname=None):
        """

        :type report: dict
        :type report_pathname: str
        """
        if report_pathname is None:
            print(json.dumps(report, indent=1, sort_keys=True))
        else:
            report_dirname = os.path.dirname(report_pathname)
            if not os.path.exists(report_dirname):
                os.makedirs(report_dirname)

            with open(report_pathname, 'w') as f:
                json.dump(report, f, indent=4, sort_keys=True)
            # report_file = open(report_pathname, 'w')
            # try:
            #     json.dump(report, report_file, indent=1, sort_keys=True)
            # finally:
            #     report_file.close()

    @staticmethod
    def get_current_time():
        return strftime("%Y-%m-%dT%H:%M:%S%Z", gmtime())


def ensure_availability_of_imports():
    import sys

    # noinspection PyBroadException
    try:
        import netCDF4

        print(ProductVerifier.get_current_time(), 'The netCDF4 module is available')
    except:
        print(ProductVerifier.get_current_time(), 'The netCDF4 module is not available. Please install netCDF4.')
        sys.exit(1)


if __name__ == "__main__":
    # Call with one or two arguments:
    #
    # 1 = source pathname
    # 2 = report pathname (optional)
    import sys

    argument_count = len(sys.argv)
    if argument_count == 2:
        verifier = ProductVerifier(sys.argv[1])
    elif argument_count == 3:
        verifier = ProductVerifier(sys.argv[1], sys.argv[2])
    else:
        print('usage:', sys.argv[0], '<source pathname> <report pathname>')
        sys.exit(1)

    ensure_availability_of_imports()

    # noinspection PyBroadException
    try:
        verifier.verify()
        sys.exit()
    except Exception as e:
        print("Error {0}".format(e.args))
        sys.exit(1)
