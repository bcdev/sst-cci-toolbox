/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_Session = function(session_properties) {
    var _properties = session_properties;
    if (_properties == null) {
        _properties = {};
    }

    this.getProperties = function() {
        return _properties;
    };

    this.setProperty = function(key, value) {
        _properties[key] = value;
    };

    this.getProperty = function(key) {
        return _properties[key];
    };

    this.setFilename = function(name) {
        this.setProperty('filename', name);
    };

    this.getFilename = function() {
        return this.getProperty('filename');
    };

    this.setTemplateDocPath = function(document) {
        this.setProperty('template_path', document);
    };

    this.getTemplateDocPath = function() {
        return getAtLeastEmptyString(this.getProperty('template_path'));
    };

    this.setDefaultTablePath = function(document) {
        this.setProperty('default_table_path', document);
    };

    this.getDefaultTablePath = function() {
        return getAtLeastEmptyString(this.getProperty('default_table_path'));
    };

    this.setFiguresDirectory = function(dir) {
        this.setProperty('figures.directory', dir);
    };

    this.getFiguresDirectory = function() {
        return getAtLeastEmptyString(this.getProperty('figures.directory'));
    };

    this.hasFileName = function() {
        var filename = this.getFilename();
        return filename != null && filename.trim().length > 0;
    };

    function getAtLeastEmptyString(val) {
        if (!val) {
            return '';
        }
        return val;
    }
};
