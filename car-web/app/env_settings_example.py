import os

# *****************************
# Environment specific settings
# *****************************

# The settings below can (and should) be over-ruled by OS environment variable settings

# Flask settings                     # Generated with: import os; os.urandom(24)
SECRET_KEY = '\xb9\x8d\xb5\xc2\xc4Q\xe7\x8ej\xe0\x05\xf3\xa3kp\x99l\xe7\xf2i\x00\xb1-\xcd'
# PLEASE USE A DIFFERENT KEY FOR PRODUCTION ENVIRONMENTS!

# SQLAlchemy settings
SQLALCHEMY_DATABASE_URI = 'mysql://<username>:<password>@127.0.0.1:3306/<db-name>'

USER_ENABLE_EMAIL = False
USER_ENABLE_CONFIRM_EMAIL = False
USER_ENABLE_USERNAME = False
USER_ENABLE_REGISTER = False

# Flask-Mail settings
MAIL_USERNAME = 'email@example.com'
MAIL_PASSWORD = 'password'
MAIL_DEFAULT_SENDER = '"AppName" <noreply@example.com>'
MAIL_SERVER = 'MAIL_SERVER', 'smtp.gmail.com'
MAIL_PORT = 465
MAIL_USE_SSL = True
MAIL_USE_TLS = False

ADMINS = [
    '"Admin One" <admin1@test.domain.com>',
    ]

TEMPLATES_DIR = "/dir/to/car/tool/docx/template/files"
DEFAULT_TABLES_DIR = "/dir/to/car/tool/properties/default/tables"
SESSIONS_DIR = "/dir/to/car/tool/sessions"
IMAGES_DIR = "/dir/to/car/tool/figures"
IMAGES_CACHE = "/dir/to/car/tool/image/request/on/demand/scaled/images/cache"

THUMBNAIL_IMG_MAX_WIDHT = 250
THUMBNAIL_IMG_MAX_HEIGHT = 100
THUMBNAIL_BACKGROUND = '#999999'
