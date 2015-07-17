__author__ = 'tom'

from flask import Flask, render_template


class AsessmentApp(Flask):


    def __init__(self, import_name):
        super(AsessmentApp, self).__init__(import_name)
        self.name = 'Assessment'

    @app.route('/hello/')
    @app.route('/hello/<name>')
    def hello(name=None):
        return render_template('hello.html', name=name)


app = AsessmentApp(__name__)


if __name__ == '__main__':
    app.run()