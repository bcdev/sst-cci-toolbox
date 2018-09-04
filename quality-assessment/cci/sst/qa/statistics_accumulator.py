import os
import re

from sst.qa.productverifier import ProductVerifier
from sst.qa.svrrunner import SvrRunner


class StatisticsAccumulator():

    def __init__(self, report_dir_pathname, summary_report_pathname=None, report_filename_pattern=SvrRunner.get_report_filename_pattern()):

        """

        :type report_dir_pathname: str
        :type summary_report_pathname: str
        :type report_filename_pattern: str
        """
        self.report_dir_pathname = report_dir_pathname
        self.summary_report_pathname = summary_report_pathname
        self.report_filename_pattern = report_filename_pattern
        self.summary_report = {'files.count': 0}

    def accumulate(self):
        total_vector = [0, 0, 0, 0, 0]
        try:
            for root, dirnames, filenames in os.walk(self.report_dir_pathname):
                for filename in filenames:
                    if re.match(self.report_filename_pattern, filename):
                        report_pathname = os.path.join(root, filename)
                        year = filename[:4]
                        month = filename[4:6]
                        date_key = year + '-' + month

                        month_vector = [0, 0, 0, 0, 0]
                        if date_key in self.summary_report:
                            month_vector = self.summary_report[date_key]
                        else:
                            self.summary_report[date_key] = month_vector

                        report = ProductVerifier.load_report(report_pathname)
                        total_count = int(report["lon.count.total"])
                        month_vector[0] += total_count
                        total_vector[0] += total_count

                        not_ocean = int(report["not_ocean"])
                        month_vector[1] += not_ocean
                        total_vector[1] += not_ocean

                        failed_retrieval = int(report["sst_failed_retrieval"])
                        month_vector[2] += failed_retrieval
                        total_vector[2] += failed_retrieval

                        invalid_retrieval = int(report["sst_invalid_retrieval"])
                        month_vector[3] += invalid_retrieval
                        total_vector[3] += invalid_retrieval

                        valid_retrieval = int(report["sst_valid_retrieval"])
                        month_vector[4] += valid_retrieval
                        total_vector[4] += valid_retrieval

                        self.summary_report[date_key] = month_vector

                        self.summary_report['files.count'] += 1

        finally:
            self.summary_report["total"] = total_vector
            ProductVerifier.dump_report(self.summary_report, self.summary_report_pathname)


if __name__ == "__main__":
    # Call with up to two arguments:
    #
    # 1 = report dir pathname
    # 2 = summary report pathname (optional)
    import sys

    argument_count = len(sys.argv)
    if argument_count == 2:
        accumulator = StatisticsAccumulator(sys.argv[1])
    elif argument_count == 3:
        accumulator = StatisticsAccumulator(sys.argv[1], sys.argv[2])
    else:
        print('usage:', sys.argv[0], '<report dir pathname> <summary report pathname>')
        sys.exit(1)

    # noinspection PyBroadException
    try:
        accumulator.accumulate()
    except:
        pass  # sys.exit(1)

    sys.exit()
