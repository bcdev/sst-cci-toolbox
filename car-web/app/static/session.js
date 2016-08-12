/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_Session = function(session_properties) {
    var _ = this;

    var _properties = session_properties;
    if (_properties == null) {
        _properties = {};
    }

    _.getProperties = function() {
        return _properties;
    };

    _.setProperty = function(key, value) {
        _properties[key] = value;
    };

    _.removeProperty = function(key) {
        delete _properties[key];
    };

    _.getProperty = function(key) {
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

    function getAtLeastEmptyString(val) {
        if (!val) {
            return '';
        }
        return val;
    }
};
