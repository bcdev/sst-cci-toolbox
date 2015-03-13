__author__ = 'ralf'

import os

from svr_product_verifier import ProductVerifier


class ReportAccumulator:
    def __init__(self, report_dir_pathname, summary_report_pathname):
        """

        :type report_dir_pathname: str
        :type summary_report_pathname: str
        """
        self.report_dir_pathname = report_dir_pathname
        self.summary_report_pathname = summary_report_pathname
        self.summary_report = {}

    def accumulate_entry(self, k, v):
        try:
            a = self.summary_report[k]
        except KeyError:
            a = 0
        self.summary_report[k] = a + v

    def accumulate(self):
        try:
            for root, dirnames, filenames in os.walk(self.report_dir_pathname):
                for filename in filenames:
                    if filename.endswith('.json'):
                        report_pathname = os.path.join(root, filename)
                        print 'Accumulating report', report_pathname
                        report = ProductVerifier.load_report(report_pathname)
                        for k, v in report.iteritems():
                            self.accumulate_entry(k, v)
                        self.accumulate_entry('count', 1)
        finally:
            ProductVerifier.dump_report(self.summary_report, self.summary_report_pathname)


if __name__ == "__main__":
    # Call with two arguments:
    #
    # 1 = source pathname
    # 2 = report pathname
    import sys

    accumulator = ReportAccumulator('/Users/ralf/scratch')
    # noinspection PyBroadException
    try:
        accumulator.accumulate()
        sys.exit(0)
    except:
        sys.exit(1)
