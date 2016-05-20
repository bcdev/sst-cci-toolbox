from flask import Flask, render_template
from flask_mail import Mail
from flask_sqlalchemy import SQLAlchemy
from flask_user import login_required, UserManager, UserMixin, SQLAlchemyAdapter

app = Flask(__name__)

@app.route('/')
@app.route('/<name>')
@app.route('/root/')
@app.route('/hello')
@app.route('/hello/')
@app.route('/hello/<name>')
@login_required
def hello(name=None):
    # valid_login

    return render_template('hello.html', name=name)

if __name__ == '__main__':
    app.debug = True
    app.run()