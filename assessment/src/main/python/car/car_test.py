__author__ = 'Ralf Quast'

import unittest

from docx import Document

from car import Car


class CarTests(unittest.TestCase):
    def test_create_document(self):
        car = Car()
        document = car.get_document()
        self.assertTrue(isinstance(document, Document))
        document.save("/Users/ralf/Desktop/car.docx")


if __name__ == '__main__':
    unittest.main()
