#!/bin/bash

export ENV_SETTINGS_FILE=/usr/local/cartool/config/cartool-config.py
export PYTHONUNBUFFERED=1

# Run a Flask development server
# ==============================
# --debug         # Run it in DEBUG mode.
# --reload        # Restart when a python file changed
# --host=0.0.0.0  # make flask web application public ... default 127.0.0.1 = localhost
# -----------------------------------------------
# python manage.py runserver --debug --reload --host=0.0.0.0
python manage.py runserver --reload --host=0.0.0.0

# init database and init/add users
# ================================
# to add users edit create_users.py before starting the script
# ------------------------------------------------------------
# python manage.py init_db
