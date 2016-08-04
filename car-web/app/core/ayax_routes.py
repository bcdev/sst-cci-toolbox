from app import app
from flask import request
from werkzeug.utils import secure_filename
from flask_login import current_user  # , LoginManager, login_required, login_user, logout_user, UserMixin
from jproperties import Properties
from flask_images import Images, resized_img_src
import json
import os

car_sessions_dir = app.config['SESSIONS_DIR']
car_templates_dir = app.config['TEMPLATES_DIR']
car_default_tables_dir = app.config['DEFAULT_TABLES_DIR']
car_figures_dir = app.config['IMAGES_DIR']
thumb_widht = app.config['THUMBNAIL_IMG_MAX_WIDHT']
thumb_height = app.config['THUMBNAIL_IMG_MAX_HEIGHT']
thumb_back = app.config['THUMBNAIL_BACKGROUND']

images = Images(app)
app.config['IMAGES_PATH'].append(car_figures_dir)
app.config['IMAGES_CACHE'] = 'F:/Development Temp Dirs/SST-CCI_CAR-Tool/Img_cache'


@app.route('/upload/template', methods=['POST'])
def upload_template():
    return _upload(request, '.docx', car_templates_dir, "*.docx")


@app.route('/upload/default_table', methods=['POST'])
def upload_default_table():
    return _upload(request, '.properties', car_default_tables_dir, "*.properties")


@app.route('/get_images', methods=['POST'])
def get_images():
    if request.method == 'POST':
        # check if the post request has 'images_dir' in data
        if 'images_dir' in request.form:
            dir_ = request.form['images_dir']
            images_dir = os.path.join(car_figures_dir, dir_)
            images = os.listdir(images_dir)
            images = filter(lambda f: f.endswith('.png'), images)
            ret = []
            for img in images:
                img_path = os.path.join(dir_, img)
                img_dict = {
                    'name': img,
                    'path': img_path,
                    'url': resized_img_src(img_path, width=thumb_widht, height=thumb_height, mode='fit', background=thumb_back)
                    # 'url': resized_img_src(img_path, width=thumb_widht, height=thumb_height, mode='pad', background=thumb_back)
                }
                ret.append(img_dict)
            return json.dumps(ret)


@app.route('/get_document_keys', methods=['POST'])
def get_document_keys():
    if request.method == 'POST':
        if 'default_table_path' in request.form:
            default_table_path_ = request.form['default_table_path']
            keys = {}
            keys['figure_keys'] = {}
            keys['default_keys'] = {}
            p = Properties()
            with open(os.path.join(car_default_tables_dir, default_table_path_), 'r') as f:
                p.load(f, "utf-8")
            props = p.properties
            for key in props:
                if key.startswith('figure'):
                    if key.endswith('default'):
                        keys['default_keys'][key] = props[key]
                    else:
                        keys['figure_keys'][key] = props[key]
            return json.dumps(keys)


@app.route('/component_update', methods=['POST'])
def component_update():
    if request.method == 'POST':
        # check if the post request has 'component_id' in data
        if 'component_id' in request.form:
            id_ = request.form['component_id']
            if id_ == u'template_drop_down':
                return _create_template_drop_down(id_)
            elif id_ == u'default_table_drop_down':
                return _create_datault_table_drop_down(id_)
            elif id_ == u'car_session_drop_down':
                return _create_car_sessions_drop_down(id_)
            elif id_ == u'figures_dir_drop_down':
                return _create_figures_dir_drop_down(id_)


@app.route('/load_session', methods=['POST'])
def load_session():
    if request.method == 'POST':
        # check if the post request has component_id in data
        if 'session_name' in request.form:
            session_name = request.form['session_name']
            p = Properties()
            with open(os.path.join(car_sessions_dir, session_name), 'r') as f:
                p.load(f, "utf-8")
            session = json.dumps(p.properties)
            return session


@app.route('/save_session', methods=['POST'])
def safe_session():
    if request.method == 'POST':
        # check if the post request has component_id in data
        if 'session' in request.form:
            session_ = request.form['session']
            session_ = json.loads(session_)
            filename_ = session_['filename']
            if not filename_:
                return 'Missing a filename. Please use "Save Session As" button to set a filename'
            filename_ = secure_filename(filename_)
            filename_ = filename_.strip()
            filename_lowered = filename_.lower()

            file_extension = '.properties'
            if filename_lowered.endswith(file_extension):
                filename_ = filename_[:-len(file_extension)].strip()

            if len(filename_) == 0:
                return 'Empty filename is not allowed. Please use "Save Session As" button to set a valid filename.'

            if not os.path.exists(car_sessions_dir):
                return 'Sessions directory does not exist.'

            first_name = current_user.first_name
            user_sessions_path = os.path.join(car_sessions_dir, first_name)

            if not os.path.exists(user_sessions_path):
                os.mkdir(user_sessions_path)

            if os.path.isfile(user_sessions_path):
                return 'Illegal file system situation. Please inform the administrator of the web application.'

            if not os.path.exists(user_sessions_path):
                return 'Unable to crate a user directory for containing sessions.'

            p = Properties()
            for key in session_:
                p[key] = session_[key]

            session_file_path = os.path.join(user_sessions_path, filename_ + file_extension)
            with open(session_file_path, "w") as f:
                p.store(f, encoding="utf-8")

            return 'Session successfully stored as "' + filename_ + '"'


def _upload(request, extension, targetDir, allowed):
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' in request.files:
            file = request.files['file']
            # if user does not select file, browser also
            # submit a empty part without filename
            filename = file.filename
            if filename.endswith(extension):
                filename = secure_filename(file.filename)

                first_name = current_user.first_name
                user_target_path = os.path.join(targetDir, first_name)

                if not os.path.exists(user_target_path):
                    os.mkdir(user_target_path)

                if os.path.isfile(user_target_path):
                    return 'Illegal file system situation. Please inform the administrator of the web application.'

                if not os.path.exists(user_target_path):
                    return 'Unable to crate a user directory for containing sessions.'

                file.save(os.path.join(user_target_path, filename))
                return "The file '%s' is successfully uploaded!" % filename
            else:
                return "Only %s files are allowed for upload." % allowed


def _create_template_drop_down(component_id):
    return _create_files_drop_down_for_context('Templates',
                                               car_templates_dir,
                                               component_id,
                                               'select template ...',
                                               '.docx')


def _create_datault_table_drop_down(component_id):
    return _create_files_drop_down_for_context('Default tables',
                                               car_default_tables_dir,
                                               component_id,
                                               'select default table ...',
                                               '.properties')


def _create_car_sessions_drop_down(component_id):
    return _create_files_drop_down_for_context('Sessions',
                                               car_sessions_dir,
                                               component_id,
                                               'select session ...',
                                               '.properties')


def _create_files_drop_down_for_context(context_name, root_dir, component_id, first_option_text, allowed_ending):
    user_name = current_user.first_name
    ret = '<select id="%s">' % component_id
    ret += '<option value="">%s</option>' % first_option_text
    user_path = os.path.join(root_dir, user_name)
    if os.path.isdir(user_path):
        user_files = os.listdir(user_path)
        for f_name in user_files:
            if f_name.endswith(allowed_ending):
                f_path = os.path.join(user_path, f_name)
                relpath = os.path.relpath(f_path, root_dir)
                ret += '<option value="' + relpath + '" >' + f_name[:-len(allowed_ending)] + '</option>'

    for dir, dirs, files in os.walk(root_dir):
        is_root = dir == root_dir
        is_user_path = dir == user_path
        files = filter(lambda f: f.endswith(allowed_ending), files)
        if len(files) > 0:
            if is_root:
                ret += '<optgroup label="General">'
                for file in files:
                    ret += '<option value="' + file + '">' + file[:-len(allowed_ending)] + '</option>'
                ret += '</optgroup>'
            elif not is_user_path:
                u_name = os.path.split(dir)[1]
                ret += '<optgroup label="' + context_name + ' from user: ' + u_name + '">'
                for file in files:
                    file_path = os.path.join(dir, file)
                    relpath = os.path.relpath(file_path, root_dir)
                    ret += '<option value="' + relpath + '" >' + file[:-len(allowed_ending)] + '</option>'
                ret += '</optgroup>'
    ret += '</select>'
    return ret


def _create_figures_dir_drop_down(id_):
    ret = '<select id="%s">' % id_
    ret += '<option values="">select figures dir ...</option>'
    for dir, dirs, files in os.walk(car_figures_dir):
        is_figures_root_dir = dir == car_figures_dir
        relpath = os.path.relpath(dir, car_figures_dir)
        if len(files) > 0:
            ret += '<option value="' + relpath + '" >'
            if is_figures_root_dir:
                ret += 'FIGURES_ROOT'
            else:
                ret += relpath
            ret += '</option>'
    ret += '</select>'
    return ret
