from svr import Svr

import unittest

class SvrTests(unittest.TestCase):

    def test_assemble_call(self):
        svr = Svr()
        call = svr.assemble_call("python_path", "/input/dir", "/result/dir", "wheater_measure")
        self.assertEquals("python_path svr_workflow.py /input/dir /result/dir wheater_measure", call)

if __name__ == '__main__':
    unittest.main()
