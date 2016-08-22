from flask.helpers import url_for
from app import app
from flask import request, send_from_directory, send_file
from werkzeug.utils import secure_filename
from flask_login import current_user  # , LoginManager, login_required, login_user, logout_user, UserMixin
from jproperties import Properties
from flask_images import Images, resized_img_src
from py4j.java_gateway import JavaGateway
from PIL import Image
import json
import os

car_sessions_dir = app.config['SESSIONS_DIR']
car_templates_dir = app.config['TEMPLATES_DIR']
car_default_tables_dir = app.config['DEFAULT_TABLES_DIR']
car_figures_dir = app.config['IMAGES_DIR']
car_rendered_documents_dir = app.config['RENDERED_DOCUMENTS']
car_images_cache = app.config['IMAGES_CACHE']

thumb_widht = app.config['THUMBNAIL_IMG_MAX_WIDHT']
thumb_height = app.config['THUMBNAIL_IMG_MAX_HEIGHT']
thumb_back = app.config['THUMBNAIL_BACKGROUND']

images = Images(app)
app.config['IMAGES_PATH'].append(car_figures_dir)


@app.route('/upload/template', methods=['POST'])
def upload_template():
    return _upload(request, '.docx', car_templates_dir, "*.docx")


@app.route('/upload/default_table', methods=['POST'])
def upload_default_table():
    return _upload(request, '.properties', car_default_tables_dir, "*.properties")


@app.route('/get_image')
def get_image():
    filename = request.args.get('name')
    directory = request.args.get('dir')
    dir_path = _create_figures_dir_path(directory)
    _log_info('Send image: ' + dir_path + '/' + filename)
    return send_from_directory(dir_path, filename, mimetype='image/png')


@app.route('/get_images', methods=['POST'])
def get_images():
    if request.method == 'POST':
        # check if the post request has 'images_dir' in data
        if 'images_dir' in request.form:
            dir_ = request.form['images_dir']
            images_dir = _create_figures_dir_path(dir_)
            _log_info("Get images list from " + images_dir)
            images = os.listdir(images_dir)
            images = filter(lambda f: f.endswith('.png'), images)
            ret = []
            for img in images:
                img_path = os.path.join(dir_, img)
                img_full_path = os.path.join(car_figures_dir, img_path)
                im = Image.open(img_full_path)
                width, height = im.size
                img_dict = {
                    'width': width,
                    'height': height,
                    'name': img,
                    'path': img_path,
                    'thumb_url': resized_img_src(img_path, width=thumb_widht, height=thumb_height, mode='fit', background=thumb_back),
                    'url': request.host_url + 'get_image?dir=' + dir_ + '&name=' + img
                }
                ret.append(img_dict)
            return json.dumps(ret)


@app.route('/get_document_keys', methods=['POST'])
def get_document_keys():
    if request.method == 'POST':
        if 'default_table_path' in request.form:
            keys = {}
            default_table_path_ = request.form['default_table_path']
            if len(default_table_path_.strip(' /\\')) == 0:
                _log_info("No default table selected: Return empty key set.")
                return json.dumps(keys)

            _log_info("Get document keys from " + default_table_path_)
            keys['figure_keys'] = {}
            keys['default_keys'] = {}
            p = Properties()
            abs_default_table_path = os.path.join(car_default_tables_dir, default_table_path_)
            if os.path.isfile(abs_default_table_path):
                with open(abs_default_table_path, 'r') as f:
                    p.load(f, "utf-8")
            else:
                _log_warning('The default table file ' + abs_default_table_path + ' does not exist.')
            props = p.properties
            return json.dumps(props)


@app.route('/update_component_options', methods=['POST'])
def update_component_options():
    if request.method == 'POST':
        # check if the post request has 'component_id' in data
        if 'component_id' in request.form:
            id_ = request.form['component_id']
            _log_info("Create options for: " + id_)
            if id_ == u'template_file_drop_down':
                return _create_template_file_drop_down_options()
            elif id_ == u'default_table_drop_down':
                return _create_datault_table_drop_down_options()
            elif id_ == u'load_session_form_drop_down':
                return _create_load_session_form_drop_down_options()
            elif id_ == u'figures_dir_drop_down':
                return _create_figures_dir_drop_down_options()


@app.route('/load_session', methods=['POST'])
def load_session():
    if request.method == 'POST':
        # check if the post request has component_id in data
        if 'session_name' in request.form:
            session_name = request.form['session_name']
            p = Properties()
            session_path = os.path.join(car_sessions_dir, session_name)
            _log_info("load session: " + session_path)
            with open(session_path, 'r') as f:
                p.load(f, "utf-8")
            session = p.properties

            fig_dir_ = session['figures.directory']
            if fig_dir_:
                session['figures.directory'] = os.path.split(fig_dir_)[1]

            session = json.dumps(session)
            return session


@app.route('/save_session', methods=['POST'])
def save_session():
    if request.method == 'POST':
        # check if the post request has component_id in data
        if 'session' in request.form:
            session_ = request.form['session']
            session_ = json.loads(session_)
            filename_ = session_['filename']
            _log_info("Try safe session: filename=" + filename_)
            if not filename_:
                msg = 'Missing a filename. Please use "Save Session As" button to set a filename'
                _log_info(msg)
                return msg
            filename_ = secure_filename(filename_)
            filename_ = filename_.strip()
            filename_lowered = filename_.lower()

            file_extension = '.properties'
            if filename_lowered.endswith(file_extension):
                filename_ = filename_[:-len(file_extension)].strip()

            if len(filename_) == 0:
                msg = 'Empty filename is not allowed. Please use "Save Session As" button to set a valid filename.'
                _log_info(msg)
                return msg

            if not os.path.exists(car_sessions_dir):
                msg = 'Sessions directory does not exist.'
                _log_error(msg)
                return msg

            first_name = current_user.first_name
            user_sessions_path = os.path.join(car_sessions_dir, first_name)

            if not os.path.exists(user_sessions_path):
                os.mkdir(user_sessions_path)

            if os.path.isfile(user_sessions_path):
                msg = 'Illegal file system situation. Please inform the administrator of the web application.'
                _log_error(msg)
                return msg

            if not os.path.exists(user_sessions_path):
                msg = 'Unable to crate a user directory for containing sessions.'
                _log_error(msg)
                return msg

            return _save_session_to(user_sessions_path, session_, filename_, file_extension)


@app.route('/render_document', methods=['POST'])
def render_document():
    if request.method == 'POST':
        # check if the post request has component_id in data
        if 'session' in request.form:
            session_ = request.form['session']
            session_ = json.loads(session_)
            if session_:
                session_file_name_ = os.path.split(os.tmpnam())[1]
                document_file_name_ = os.path.split(os.tmpnam())[1]
                extension = '.properties'
                _save_session_to(car_rendered_documents_dir, session_, session_file_name_, extension)

                template_path_ = session_['template_path']

                template_file_path = os.path.join(car_templates_dir, template_path_)
                properties_file_path = os.path.join(car_rendered_documents_dir, session_file_name_ + extension)
                document_file_path = os.path.join(car_rendered_documents_dir, document_file_name_)

                g = JavaGateway()
                args = _create_args(document_file_path, properties_file_path, template_file_path, g)

                fassade = g.entry_point.getFassade()
                message = fassade.renderDocument(args)

                if os.path.isfile(document_file_path):
                    download_url = url_for('download_document', filename=document_file_name_)
                    return '<a href="' + download_url + '">download</a>'
                else:
                    return message
            else:
                return 'Session Invalid!'


@app.route('/dd/<filename>', methods=['GET'])
def download_document(filename):
    if request.method == 'GET':
        return send_from_directory(car_rendered_documents_dir, filename, as_attachment=True ,attachment_filename='rendered_report.docx')


def _create_args(out_file_path, car_properties_file, car_template_file, javaGateway):
    args = ['-o', out_file_path, '-p', car_properties_file, '-t', car_template_file]
    string_class = javaGateway.jvm.String
    string_array = javaGateway.new_array(string_class, len(args))
    idx = 0
    for arg in args:
        string_array[idx] = arg
        idx += 1
    return string_array


def _log_info(message):
    app.logger.info(_add_username(message))


def _log_warning(message):
    app.logger.warning(_add_username(message))


def _log_error(message):
    app.logger.error(_add_username(message))


def _add_username(message):
    email = current_user.email
    message = '[' + email + '] ' + message
    return message


def _save_session_to(dir_path, session, filename, file_extension):
    fig_dir_ = session['figures.directory']
    if fig_dir_:
        session['figures.directory'] = os.path.join(car_figures_dir, fig_dir_)

    p = Properties()
    for key in session:
        p[key] = session[key]
    session_file_path = os.path.join(dir_path, filename + file_extension)
    with open(session_file_path, "w") as f:
        p.store(f, encoding="utf-8")
    msg = 'Session successfully stored as "' + filename + '"'
    _log_info(msg)
    return msg


def _upload(request, extension, targetDir, allowed):
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' in request.files:
            file = request.files['file']
            # if user does not select file, browser also
            # submit a empty part without filename
            filename = file.filename
            _log_info("Try uploading file '" + filename + "' to " + targetDir)
            if filename.endswith(extension):
                filename = secure_filename(file.filename)

                first_name = current_user.first_name
                user_target_path = os.path.join(targetDir, first_name)

                if not os.path.exists(user_target_path):
                    os.mkdir(user_target_path)

                if os.path.isfile(user_target_path):
                    message = 'Illegal file system situation. Please inform the administrator of the web application.'
                    _log_error(message)
                    return message

                if not os.path.exists(user_target_path):
                    message = 'Unable to crate a user directory for containing sessions.'
                    _log_error(message)
                    return message

                file.save(os.path.join(user_target_path, filename))
                message = "The file '%s' is successfully uploaded!" % filename
                _log_info(message)
                return message
            else:
                message = "Only %s files are allowed for upload." % allowed
                _log_info(message)
                return message


def _create_template_file_drop_down_options():
    return _create_files_drop_down_for_context('Templates',
                                               car_templates_dir,
                                               '.docx')


def _create_datault_table_drop_down_options():
    return _create_files_drop_down_for_context('Default tables',
                                               car_default_tables_dir,
                                               '.properties')


def _create_load_session_form_drop_down_options():
    return _create_files_drop_down_for_context('Sessions',
                                               car_sessions_dir,
                                               '.properties')


def _create_files_drop_down_for_context(context_name, root_dir, allowed_ending):
    _log_info("Create drop down options for context '" + context_name + "'")
    user_name = current_user.first_name
    ret = ''
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
    return ret


def _create_figures_dir_drop_down_options():
    _log_info("Create figures drop down options.")
    ret = ''
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
    return ret


def _create_figures_dir_path(selected_dir):
    return os.path.join(car_figures_dir, selected_dir)
