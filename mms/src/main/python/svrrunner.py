__author__ = 'Ralf Quast'

from os import path
from os import walk

from productverifier import ProductVerifier


class SvrRunner:
    def __init__(self, source_dirpath, report_dirpath):
        """
        :param source_dirpath: string
        :param report_dirpath: string
        """
        self.source_dirpath = source_dirpath
        self.report_dirpath = report_dirpath

    def run(self):
        source_pathnames = self.glob_source_pathnames()

        print ProductVerifier.get_current_time(), \
            "found " + str(len(source_pathnames)) + " source files in '" + self.source_dirpath + "'"

        for source_pathname in source_pathnames:
            print ProductVerifier.get_current_time(), "source pathname:", source_pathname
            report_filename = path.basename(source_pathname) + ".json"
            report_pathname = path.join(self.report_dirpath, report_filename)
            print ProductVerifier.get_current_time(), "report pathname:", report_pathname
            verifier = ProductVerifier(source_pathname, report_pathname)
            verifier.verify()

    def glob_source_pathnames(self):
        """

        :rtype : list
        """
        source_pathnames = list()
        for dirpath, dirnames, filenames in walk(self.source_dirpath):
            for filename in filenames:
                if filename.endswith(".nc"):
                    pathname = path.join(dirpath, filename)
                    source_pathnames.append(path.abspath(pathname))
        return source_pathnames

    @staticmethod
    def get_report_filename_pattern():
        """

        :rtype : str
        """
        return '.*\\.nc\\.json'

    @staticmethod
    def get_source_dirpath(archive_root, version, usecase, sensor, year, month, day):
        """

        :type archive_root: str
        :type version: str
        :type usecase: str
        :type sensor: str
        :type year: str
        :type month: str
        """
        # /<archive-root>/<version>/<usecase>/<sensor>/<yyyy>/<mm>/<dd>
        return path.join(archive_root, version, usecase, sensor, year, month, day)

    @staticmethod
    def get_report_dirpath(archive_root, version, usecase, sensor, year=None, month=None, day=None):
        """

        :type archive_root: str
        :type version: str
        :type usecase: str
        :type sensor: str
        :type year: str
        :type month: str
        :type day: str
        """
        if year is None:
            # /<archive-root>/<version>/<usecase>-svr/<sensor>
            return path.join(archive_root, version, usecase, sensor)
        elif month is None:
            # /<archive-root>/<version>/<usecase>-svr/<sensor>/<yyyy>
            return path.join(archive_root, version, usecase, sensor, year)
        elif day is None:
            # /<archive-root>/<version>/<usecase>-svr/<sensor>/<yyyy>/<mm>
            return path.join(archive_root, version, usecase, sensor, year, month)
        else:
            # /<archive-root>/<version>/<usecase>-svr/<sensor>/<yyyy>/<mm>/<dd>
            return path.join(archive_root, version, usecase, sensor, year, month, day)


if __name__ == "__main__":
    import sys

    _year = sys.argv[1]
    _month = sys.argv[2]
    _day = sys.argv[3]
    _sensor = sys.argv[4]
    _usecase = sys.argv[5]
    _version = sys.argv[6]
    _archive_root = sys.argv[7]
    _report_root = sys.argv[8]

    _source_dirpath = SvrRunner.get_source_dirpath(_archive_root, _version, _usecase, _sensor, _year, _month, _day)
    _report_dirpath = SvrRunner.get_report_dirpath(_report_root, _version, _usecase, _sensor, _year, _month, _day)
    runner = SvrRunner(_source_dirpath, _report_dirpath)

    # noinspection PyBroadException
    try:
        runner.run()
    except:
        sys.exit(1)

    sys.exit()
