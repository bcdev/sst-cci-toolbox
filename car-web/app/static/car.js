/**
 * Created by sabine.embacher@brockmann-consult.de on 21.07.2016.
 */

CAR_Tool = function() {
    var _session = new CAR_Session();

    this.getSession = function() {
        return _session;
    };

    this.setSession = function(car_session) {
        _session = car_session;
    };

    this.resetSession = function() {
        var old = _session;
        _session = new CAR_Session();
        _session.setFiguresDirectory(old.getFiguresDirectory());
        _session.setDefaultTablePath(old.getDefaultTablePath());
        _session.setTemplateDocPath(old.getTemplateDocPath());
        _session.setFilename(old.getFilename());
        this.set_figure_defaults_to_session()
    };

    // ############################################################
    // ############################################################
    // ############################################################

    var _keys = new CAR_Keys();

    this.getKeys = function() {
        return _keys;
    };

    this.setKeys = function(keys) {
        _keys = keys;
    };

    // ############################################################
    // ############################################################
    // ############################################################

    //@todo needed? should it be moved to session?
    function remove_keys_from_session(key_array) {
        var i;
        for (i in key_array) {
            var key = key_array[i];
            delete _session[key];
        }
    }

    // ############################################################
    // ############################################################
    // ############################################################

    var ui = new CAR_COMMON_UI();

    this.getUI = function() {
        return ui;
    };

    // ############################################################
    // ############################################################
    // ############################################################

    this.update_ui = function() {
        var session = this.getSession();
        var selected_template = session.getTemplateDocPath();
        $('#template_drop_down').val(selected_template);

        var $defaultTableDropDown = $('#default_table_drop_down');
        var old_default_table_path = $defaultTableDropDown.val();
        var new_default_table_path = session.getDefaultTablePath();
        if (new_default_table_path != old_default_table_path) {
            $defaultTableDropDown.val(new_default_table_path);
            if (new_default_table_path.trim().length > 0) {
                $C.ajax_get_document_keys(new_default_table_path, function(keys) {
                    $C.setKeys(new CAR_Keys(keys));
                    $C.update_ui();
                });
            }
        }

        var $figures_keys = $('#figures_keys');
        var $firstOption = $figures_keys.find('option').first();
        $figures_keys.empty();
        $figures_keys.append($firstOption);
        var figure_keys = this.getKeys().get_figure_keys();
        for (var key in figure_keys) {
            key = figure_keys[key];
            $figures_keys.append($('<option value="' + key + '">' + key + '</option>'));
        }
    };

    function on_session_loaded() {
        // session Object setzen
        // Buttons updaten
        // Session name display updaten
        // Template drop down updaten
        // Default Table drop down updaten
        //    Keys updaten
        //        Key HTML Elemente ... aktuell dann keinen Key auswählen
        //              Nice to have:
        //              - Key list, damit man per cursor tasten einfach durchsteppen kann
        //              - Autoscroll zu selektierter Checkbox
        //              - show only selected Images Mode
        //        Keys an Session validieren ... ungültige entfernen
        // Images Dir setzen
    }

    this.set_figure_defaults_to_session = function() {
        var default_keys = this.getKeys().get_figure_defaults();
        for (var key in default_keys) {
            var val = default_keys[key];
            this.getSession().setProperty(key, val);
        }
    };

    this.remove_image_dir_dependent_keys_from_session = function() {
        // @todo
        // remove_keys_from_session(get_figure_keys());
    };

    this.save_session = function(ajax_callback) {
        var formData = new FormData($('#general_form')[0]);
        formData.append('session', JSON.stringify(this.getSession().getProperties()));
        $.ajax({
            type:        'POST',
            url:         '/save_session',
            contentType: false,
            data:        formData,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                if (ajax_callback) {
                    ajax_callback();
                }
                var responseText = data.responseText;
                $C.getUI().show_message(responseText);
                $C.update_ui();
                $C.validate_ui();
            }
        });
    };

    this.save_session_as = function(filename, ajax_callback) {
        this.getSession().setFilename(filename);
        this.save_session(ajax_callback);
    };

    this.load_session = function(filename, ajax_callback) {
        var formData = new FormData($('#general_form')[0]);
        formData.append('session_name', filename);
        $.ajax({
            type:        'POST',
            url:         '/load_session',
            contentType: false,
            data:        formData,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                if (ajax_callback) {
                    ajax_callback();
                }
                var json_session = data.responseText;
                var session_properties = JSON.parse(json_session);
                $C.setSession(new CAR_Session(session_properties));
                $C.update_ui();
                $C.validate_ui();
            }
        });
    };

    this.validate_ui = function() {
        var defaultTableSelected = $('#default_table_drop_down').prop('selectedIndex') > 0;

        var session = this.getSession();
        var session_name = session.getFilename();
        $('#save_session_button').prop('disabled', !defaultTableSelected || !session.hasFileName());
        $('#save_session_as_button').prop('disabled', !defaultTableSelected);
        $('#session_name_display').text(session_name);
    };

    this.ajax_file_upload = function(upload_path, form, hide_function) {
        var form_data = new FormData(form[0]);
        $.ajax({
            type:        'POST',
            url:         '/upload/' + upload_path,
            data:        form_data,
            contentType: false,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                // success:     function(data) {
                hide_function();
                // alert('<span style="font-size: 22px;">' + data + '</span>');
                $C.getUI().show_message(data.responseText);
            }
        });
    };

    this.on_template_selected = function(component) {
        this.getSession().setTemplateDocPath(component.value);
        this.validate_ui();
    };

    this.on_default_table_selected = function(component) {
        var default_table_path = component.value;
        this.ajax_get_document_keys(default_table_path, function(keys) {
            //@todo create new Session and transform valid properties from the old one?
            $C.setKeys(new CAR_Keys(keys));
            $C.resetSession();
            $C.getSession().setDefaultTablePath(default_table_path);
            $C.set_figure_defaults_to_session();
            $C.getUI().uncheck_all_image_checkboxes();
            $C.update_ui();
            $C.validate_ui();
        });
        this.validate_ui();
    };

    this.ajax_get_document_keys = function(default_table_path, call_back) {
        var formData = new FormData($('#general_form')[0]);
        formData.append('default_table_path', default_table_path);
        $.ajax({
            type:        'POST',
            url:         '/get_document_keys',
            contentType: false,
            data:        formData,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                var json_keys = data.responseText;
                json_keys = json_keys.trim();
                if (json_keys.indexOf('{') == 0) {
                    var keys = JSON.parse(json_keys);
                    call_back(keys);
                } else {
                    $C.getUI().show_message("Error while requesting key's.");
                }
            }
        });
    };

    this.ajax_get_images = function(dir, ajax_callback) {
        var formData = new FormData($('#general_form')[0]);
        formData.append('images_dir', dir);
        $.ajax({
            type:        'POST',
            url:         '/get_images',
            contentType: false,
            data:        formData,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                var json_images = data.responseText;
                var images = JSON.parse(json_images);
                ajax_callback(images);
            }
        });
    };

    this.ajax_component_update = function($element, ajax_callback) {
        var events = $C.getUI().getAttributesFromHtmlElement($element, ['onchange']);
        var component_id = $element[0].id;
        var formData = new FormData($('#general_form')[0]);
        formData.append('component_id', component_id);
        $.ajax({
            type:        'POST',
            url:         '/component_update',
            contentType: false,
            data:        formData,
            cache:       false,
            processData: false,
            async:       true,
            complete:    function(data) {
                var responseText = data.responseText;
                var $new_elem = $(responseText.trim());
                for (var event_name in events) {
                    var event_content = events[event_name];
                    $new_elem.attr(event_name, event_content);
                }
                $('#' + component_id).replaceWith($new_elem);
                if (ajax_callback) {
                    ajax_callback();
                }
            }
        });
    };

    this.create_images = function(images) {
        var $images = $('#images');
        $images.empty();
        for (var idx in images) {
            var info = images[idx];
            var thumb_url = info['thumb_url'];
            var url = info['url'];
            var name = info['name'];
            var $clone = $('#img_thumb_jquery_model').clone();
            $clone.removeAttr('id');
            $clone.find('img').attr('src', thumb_url);
            var $input = $clone.find('input');
            $input.val(url);
            $clone.find('label').append(name);
            $clone.appendTo($images);
            info.thumb_div = $clone;
        }
        $C.images = images;
    };

    this.getSelctedFigureKey = function() {
        return $('#figures_keys').val().trim();
    };

    this.isFigureKeySelected = function(key_name) {
        if (key_name) {
            return $C.getSelctedFigureKey() == key_name
        } else {
            return $C.getSelctedFigureKey().length != 0;
        }
    };
};

$C = new CAR_Tool();


