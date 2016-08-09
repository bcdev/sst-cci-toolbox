/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_Keys = function(key_properties) {
    var _properties = key_properties;
    if (_properties == null) {
        _properties = {};
    }

    this.get_figure_keys = function() {
        var fig_keys = [];
        for (var key in _properties) {
            if (key.match('^figure') && !key.match('.default$') && !key.match('.scale$')) {
                fig_keys.push(key);
            }
        }
        return fig_keys;
    };

    this.get_figure_defaults = function() {
        var figure_defaults = {};
        for (var key in _properties) {
            if (key.match('^figure') && key.match('.default$')) {
                var value = _properties[key];
                figure_defaults[key] = value;
            }
        }
        return figure_defaults;
    };
};