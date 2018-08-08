import os
import tempfile
from configparser import ConfigParser


class TestDataUtils:
    _TEST_DATA_PATH = None

    @staticmethod
    def get_test_data_dir():
        global _TEST_DATA_PATH
        if TestDataUtils._TEST_DATA_PATH is None:
            ini_file = os.path.join(os.path.dirname(__file__), "test_data.ini")
            if not os.path.isfile(ini_file):
                raise IOError("Missing configuration file: " + ini_file)

            parser = ConfigParser()
            parser.read(ini_file)
            _TEST_DATA_PATH = parser.get("TestData", "test_data_path")

        return _TEST_DATA_PATH

    @staticmethod
    def get_output_dir():
        return tempfile.gettempdir()
