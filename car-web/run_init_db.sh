#!/bin/bash

export ENV_SETTINGS_FILE=/usr/local/cartool/config/cartool-config.py
export PYTHONUNBUFFERED=1

# init database and init/add users
# ================================
# to add users edit create_users.py before starting the script
# ------------------------------------------------------------
# python manage.py init_db
