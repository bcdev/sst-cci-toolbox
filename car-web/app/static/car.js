/**
 * Created by sabine.embacher@brockmann-consult.de on 21.07.2016.
 */

_CAR_Tool = function() {
    var _session = {};
    var _keys = {};

    function setSession(session) {
        _session = session;
        update_ui();
        $C.validate_ui();
    }

    function setKeys(keys) {
        _keys = keys;
        update_ui();
        $C.validate_ui();
    }

    function get_figure_keys() {
        return _keys['figure_keys'];
    }

    function get_default_keys() {
        return _keys['default_keys'];
    }

    function remove_keys_from_session(key_array) {
        var i;
        for (i in key_array) {
            var key = key_array[i];
            delete _session[key];
        }
    }

    function update_ui() {
        var selected_template = _session['template'];
        $('#template_drop_down').val(selected_template);

        var $defaultTableDropDown = $('#default_table_drop_down');
        var old_default_table_path = $defaultTableDropDown.val();
        var new_default_table_path = _session['default_table_path'];
        if (new_default_table_path != old_default_table_path) {
            $defaultTableDropDown.val(new_default_table_path);
            if (new_default_table_path.trim().length > 0) {
                $C.ajax_get_document_keys(new_default_table_path, function(keys) {
                    setKeys(keys);
                });
            }
        }

        var $figures_keys = $('#figures_keys');
        var $firstOption = $figures_keys.find('option').first();
        $figures_keys.empty();
        $figures_keys.append($firstOption);
        var figure_keys = get_figure_keys();
        for (var key in figure_keys) {
            $figures_keys.append($('<option value="' + key + '">' + key + '</option>'));
        }
    }

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

    this.set_session_property = function(name, value) {
        _session[name] = value;
        $C.validate_ui();
    };

    this.remove_old_keys_from_session = function() {
        for (var i in _keys) {
            var key_array = _keys[i];
            remove_keys_from_session(key_array);
        }
    };

    this.set_default_keys_to_session = function() {
        var default_keys = get_default_keys();
        for (var key in default_keys) {
            var val = default_keys[key];
            $C.set_session_property(key, val);
        }
    };

    this.uncheck_all_image_checkboxes = function() {
        $('#images').find('input').prop('checked', false);
    };

    this.remove_image_dir_dependent_keys_from_session = function() {
        remove_keys_from_session(get_figure_keys());
    };

    this.save_session = function(ajax_callback) {
        var formData = new FormData($('#general_form')[0]);
        formData.append('session', JSON.stringify(_session));
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
                $C.show_message(responseText);
            }
        });
    };

    this.save_session_as = function(filename, ajax_callback) {
        var key = 'filename';
        $C.set_session_property(key, filename);
        $C.save_session(ajax_callback);
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
                var session = JSON.parse(json_session);
                setSession(session);
            }
        });
    };

    this.validate_ui = function() {
        var table_index = $('#default_table_drop_down').prop('selectedIndex');

        var session_name = _session['filename'];

        $('#save_session_button').prop('disabled', table_index == 0 || session_name == null || session_name.trim().length == 0);
        $('#save_session_as_button').prop('disabled', table_index < 1);
        $('#session_name_display').text(session_name);
    };

    this.show_elem_placed = function(parent, elem) {
        var pos = parent.position();
        var left = pos.left - 5;
        var top = pos.top - 5;
        elem.css({'left': left, 'top': top});
        $C.show_elem(elem);
    };

    this.show_elem = function(elem) {
        elem.css({'visibility': 'visible'});
        // elem.show(300);
    };

    this.hide_elem = function(elem) {
        elem.css({'visibility': 'hidden'});
        // elem.hide('fast');
    };

    this.show_overlay = function() {
        $C.show_elem($('#overlay'));
    };

    this.hide_overlay = function() {
        $C.hide_elem($('#overlay'));
    };

    this.show_message = function(msg) {
        $C.show_overlay();
        var box = $('#message_box');
        $C.show_elem(box);
        $('#msg_box_msg').text(msg);
        $C.center($(window), box);
    };

    this.hide_message = function() {
        $C.close_dialog($('#message_box'));
    };

    this.close_dialog = function($component) {
        $C.hide_overlay();
        $C.hide_elem($component);
    };

    this.center = function(viewport, elem) {
        var vh = viewport.height();
        var vw = viewport.width();
        var eh = elem.height();
        var ew = elem.width();
        var top = (vh - eh) / 3;
        var left = (vw - ew) / 2;
        elem.css({'left': left, 'top': top});
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
                $C.show_message(data.responseText);
            }
        });
    };

    this.on_template_selected = function(component) {
        $C.set_session_property('template_path', component.value);
        $C.validate_ui();
    };

    this.on_default_table_selected = function(component) {
        var default_table_path = component.value;
        $C.set_session_property('default_table_path', default_table_path);
        $C.ajax_get_document_keys(default_table_path, function(keys) {
            $C.remove_old_keys_from_session();
            setKeys(keys);
            $C.set_default_keys_to_session();
            $C.uncheck_all_image_checkboxes();
        });
        $C.validate_ui();
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
                    $C.show_message("Error while requesting key's.");
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
        var events = $C.getAttributesFromHtmlElement($element, ['onchange']);
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

    this.getAttributesFromHtmlElement = function(element, names) {
        var ret = {};
        for (var i in names) {
            var name = names[i];
            var value = element.attr(name);
            if (value) {
                ret[name] = value;
            }
        }
        return ret;
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

$C = new _CAR_Tool();


