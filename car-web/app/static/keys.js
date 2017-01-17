/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

var CAR_Keys = function(key_properties) {
    var _ = this;
    var _properties = key_properties;
    if (_properties == null) {
        _properties = {};
    }

    _.get_figure_keys = function() {
        var fig_keys = [];
        for (var key in _properties) {
            if (key.match('^figure') && !key.match('.default$') && !key.match('.scale$')) {
                fig_keys.push(key);
            }
        }
        return fig_keys;
    };

    _.get_comment_keys = function() {
        var comment_keys = [];
        for (var key in _properties) {
            if (key.match('^comment.')) {
                comment_keys.push(key);
            }
        }
        return comment_keys;
    };

    _.get_text_keys = function() {
        var text_keys = _.get_comment_keys();
        for (var key in _properties) {
            if ((key.match('^paragraph.') || key.match('^word.')) && !key.match('.default$')) {
                text_keys.push(key);
            }
        }
        return text_keys;
    };

    _.get_text_defaults = function() {
        var text_defaults = {};
        for (var key in _properties) {
            if ((key.match('^paragraph.') || key.match('^word.')) && key.match('.default$')) {
                text_defaults[key] = _properties[key];
            }
        }
        return text_defaults;
    };

    _.get_figure_defaults = function() {
        var figure_defaults = {};
        for (var key in _properties) {
            if (key.match('^figure') && key.match('.default$')) {
                figure_defaults[key] = _properties[key];
            }
        }
        return figure_defaults;
    };

    _.get_figure_scalings = function() {
        var scaling = {};
        for (var key in _properties) {
            if (key.match('^figure') && key.match('.scale$')) {
                scaling[key] = _properties[key];
            }
        }
        return scaling;
    };

    _.getCommentKeyFor = function(name_part){
        var commentKeys = _.get_comment_keys();
        var expr = name_part + '$';
        for (var i in commentKeys) {
            var key = commentKeys[i];
            if (key.match(expr)) {
                return key;
            }
        }
        return '';
    };
};