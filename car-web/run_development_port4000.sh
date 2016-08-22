#!/bin/bash

export ENV_SETTINGS_FILE=/usr/local/cartool/config/cartool-config.py
export PYTHONUNBUFFERED=1

# Run a Flask development server
# ==============================
# --debug         # Run it in DEBUG mode.
# --reload        # Restart when a python file changed
# --host=0.0.0.0  # make flask web application public ... default 127.0.0.1 = localhost
# --port=4000     # make port listening at port 4000 ... default port = 5000
# -----------------------------------------------
python manage.py runserver --debug --reload --host=0.0.0.0 --port=4000
