from svr import Svr

import unittest

class SvrTests(unittest.TestCase):

    def setUp(self):
        self.svr = Svr()

    def test_assemble_call(self):
        call = self.svr.assemble_call("python_path", "/input/dir", "/result/dir", "wheater_measure")
        self.assertEquals("python_path svr_workflow.py /input/dir /result/dir wheater_measure", call)

    def test_assemble_input_path(self):
        input_path = self.svr.assemble_input_path("/archive/directory/root", "SensoR", "2008", "05")
        self.assertEquals("/archive/directory/root/SensoR/2008/05", input_path)

    def test_extract_sensor_name(self):
        self.assertEquals("AVHRR", self.svr.extract_sensor_name("AVHRRMTA_G"))
        self.assertEquals("AVHRR", self.svr.extract_sensor_name("avhrrmta_g"))

        self.assertEquals("ATSR", self.svr.extract_sensor_name("atsr.3"))
        self.assertEquals("ATSR", self.svr.extract_sensor_name("ATSR.2.3"))

        self.assertEquals("", self.svr.extract_sensor_name("MERIS_FRS"))

if __name__ == '__main__':
    unittest.main()
