__author__ = 'tom'

from flask import Flask

class AsessmentApp(Flask):

    def __init__(self, import_name):
        self.name = 'Assessment'

app = AsessmentApp(__name__)

if __name__ == '__main__':
    app.run()