/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_Session = function(session_properties) {
    var _ = this;

    var _properties = session_properties;
    if (_properties == null) {
        _properties = {};
    }

    var _changed = false;

    _.isSessionChanged = function() {
        return _changed;
    };

    _.setSessionIsSaved = function() {
        _changed = false;
    };

    _.getProperties = function() {
        return _properties;
    };

    _.setProperty = function(key, value) {
        _properties[key] = value;
        _changed = true;
    };

    _.removeProperty = function(key) {
        delete _properties[key];
        _changed = true;
    };

    _.getProperty = function(key) {
        return _properties[key];
    };

    _.setScale = function(key, scale) {
        key = key + '.scale';
        _.setProperty(key, scale);
    };

    _.getScale = function(key) {
        key = key + '.scale';
        return _properties[key];
    };

    _.setFilename = function(name) {
        _.setProperty('filename', name);
    };

    _.getFilename = function() {
        return _.getProperty('filename');
    };

    _.setTemplateDocPath = function(document) {
        _.setProperty('template_path', document);
    };

    _.getTemplateDocPath = function() {
        return getAtLeastEmptyString(_.getProperty('template_path'));
    };

    _.setDefaultTablePath = function(document) {
        _.setProperty('default_table_path', document);
    };

    _.getDefaultTablePath = function() {
        return getAtLeastEmptyString(_.getProperty('default_table_path'));
    };

    _.setFiguresDirectory = function(dir) {
        _.setProperty('figures.directory', dir);
    };

    _.getFiguresDirectory = function() {
        return getAtLeastEmptyString(_.getProperty('figures.directory'));
    };

    _.hasFileName = function() {
        var filename = _.getFilename();
        return filename != null && filename.trim().length > 0;
    };

    _.addToMultiFigure = function(key, figure_name) {
        var trimmedName = figure_name.trim();
        var figures = _.getMultiFigureNames(key);
        if (figures.length == 0) {
            _.setProperty(key, trimmedName);
            return;
        }
        if (!containsFigure(figures, trimmedName)){
            var value = _.getProperty(key);
            _.setProperty(key, value + ';' + trimmedName)
        }
    };

    _.removeFromMultiFigure = function(key, figure_name) {
        var trimmedName = figure_name.trim();
        var figures = _.getMultiFigureNames(key);
        if (containsFigure(figures, trimmedName)){
            var newVal = '';
            for (var idx in figures) {
                var name = figures[idx];
                if (name != trimmedName) {
                    if(newVal.length > 0) {
                        newVal += ';'
                    }
                    newVal += name;
                }
            }
            if (newVal.length == 0) {
                _.removeProperty(key);
            } else {
                _.setProperty(key, newVal);
            }
        }
    };

    function containsFigure(figures_array, trimmed_figure_name) {
        for (var i in figures_array) {
            var figure_name = figures_array[i];
            if (figure_name == trimmed_figure_name) {
                return true;
            }
        }
        return false;
    }

    _.getMultiFigureNames = function(key) {
        var array = [];
        var value = _.getProperty(key);
        if (value) {
            var values = value.trim().split(';');
            for (var i in values) {
                var img_name = values[i];
                var trimmed = img_name.trim();
                if (trimmed.length > 0) {
                    array.push(trimmed);
                }
            }
        }
        return array;
    };

    function getAtLeastEmptyString(val) {
        if (!val) {
            return '';
        }
        return val;
    }
};
