/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

var CAR_COMMON_UI = function() {
    var _ = this;

    var _$overlay_div = $('#overlay');
    var _$messageBox_div = $('#message_box');
    var _$msgBoxMsg_pre = $('#msg_box_msg');
    var _$msgContent_div = $('#msg_content');
    var _$figures_KeysDropDown = $('#figures_keys');
    var _$figures_thumbs_div = $('#figures_thumbs_div');
    var _$figure_PreviewDiv = $('#figure_preview_div');
    var _$figure_PreviewImg = $('#figure_preview_img');
    var _$figures_PreviewDiv = $('#figures_preview_div');
    var _$figures_ThumbJQModel = $('#figures_thumb_jquery_model');
    var _$yesNoBox_div = $('#yes_no_box');

    _$figures_KeysDropDown.change(function() {
        if (isSingleFigureKey()) {
            _.show_elem(_$figure_PreviewDiv);
            _.hide_elem(_$figures_PreviewDiv);
        } else if (isMultiFigureKey()) {
            _.hide_elem(_$figure_PreviewDiv);
            _.show_elem(_$figures_PreviewDiv);
        } else {
            _.show_elem(_$figure_PreviewDiv);
            _.hide_elem(_$figures_PreviewDiv);
            _.removePreviewImage();
        }
    });

    function isSingleFigureKey() {
        var key = getSelectedFigureKey();
        return isString_and_NotEmpty(key) && key.indexOf('figure.') == 0;
    }

    function isMultiFigureKey() {
        var key = getSelectedFigureKey();
        return isString_and_NotEmpty(key) && key.indexOf('figures.') == 0;
    }

    function getSelectedFigureKey() {
        return _$figures_KeysDropDown.val();
    }

    function isString_and_NotEmpty(value) {
        return value != null && (typeof value == 'string') && value.trim().length > 0;
    }

    _.show_elem_placed = function(parent, elem) {
        var pos = parent.position();
        var left = pos.left - 5;
        var top = pos.top - 5;
        elem.css({'left': left, 'top': top});
        _.show_elem(elem);
    };

    _.show_elem = function($elem) {
        $elem.css({'display': 'block'});
        $elem.css({'visibility': 'visible'});
        // $elem.show(300);
    };

    _.hide_elem = function($elem) {
        $elem.css({'display': 'none'});
        $elem.css({'visibility': 'hidden'});
        // $elem.hide('fast');
    };

    _.show_overlay = function() {
        _.show_elem(_$overlay_div);
    };

    _.hide_overlay = function() {
        _.hide_elem(_$overlay_div);
    };

    _.show_message = function(msg, content, callback) {
        _.show_overlay();
        _.show_elem(_$messageBox_div);
        _$msgBoxMsg_pre.text(msg);
        _$msgContent_div.empty();
        if (content) {
            _$msgContent_div.append(content);
        }
        _.centerAtWindow(_$messageBox_div);
        var button = _$messageBox_div.find('button');
        button.unbind('click');
        button.click(function() {
            _.hide_message();
        });
        button.focus();
        if (callback) {
            button.click(function() {
                callback();
            })
        }
    };

    _.show_yes_no_dialog = function(msg, dict) {
        var _yesButton = _$yesNoBox_div.find("button:contains('yes')");
        var _noButton = _$yesNoBox_div.find("button:contains('no')");

        function close() {
            _.hide_overlay();
            _.hide_elem(_$yesNoBox_div);
        }

        _yesButton.click(function() {
            if (dict != null && dict['yesAction'] != null) {
                dict['yesAction']();
            }
            close();
        });
        _noButton.click(function() {
            if (dict != null && dict['noAction'] != null) {
                dict['noAction']();
            }
            close();
        });
        _.show_overlay();
        _$yesNoBox_div.find('pre').text(msg);
        _.show_elem(_$yesNoBox_div);
        _.centerAtWindow(_$yesNoBox_div);
        _noButton.focus();
    };

    _.hide_message = function() {
        _.close_dialog(_$messageBox_div);
    };

    _.close_dialog = function($component) {
        _.hide_overlay();
        _.hide_elem($component);
    };

    _.centerAtWindow = function(elem) {
        viewport = $(window);
        var vh = viewport.height();
        var vw = viewport.width();
        var eh = elem.height();
        var ew = elem.width();
        var top = (vh - eh) / 3;
        var left = (vw - ew) / 2;
        elem.css({'left': left, 'top': top});
    };

    _.getAttributesFromHtmlElement = function(element, names) {
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

    _.uncheck_all_image_checkboxes = function() {
        _$figures_thumbs_div.find('input').prop('checked', false);
    };

    _.select_figure_checkbox_with_name = function(value) {
        var $label = _$figures_thumbs_div.find('label:contains("' + value + '")');
        $label.find('input').prop('checked', true);
        var $thumb_div = $label.parent();
        _.scroll_to_figure_thumb($thumb_div);
    };

    _.scroll_to_figure_thumb = function($thumb_div) {
        var scroll_speed = 150; // milliseconds
        if ($thumb_div == null || $thumb_div.length == 0) {
            _$figures_thumbs_div.animate({scrollTop: 0}, scroll_speed);
            return;
        }
        var thumbs_div_top = _$figures_thumbs_div.position().top;
        // b_width = _$figures_thumbs_div.css('border-width'); // does not work at firefox ... instead use [0].clientTop
        var b_width = _$figures_thumbs_div[0].clientTop;
        var e_margin = $thumb_div.css('margin-top');
        var inner_offset = parseInt(b_width) + parseInt(e_margin);
        var viewport_top = thumbs_div_top + inner_offset;

        var current_top = _$figures_thumbs_div.scrollTop();
        var elem_top = $thumb_div.offset().top;
        var corrected_elem_top = elem_top + current_top;
        var new_scroll_top = corrected_elem_top - viewport_top;
        _$figures_thumbs_div.animate({scrollTop: new_scroll_top}, scroll_speed);
    };

    _.removePreviewImage = function() {
        _$figure_PreviewImg.removeAttr('src');
    };

    _.set_preview_image_for_name = function(image_name) {
        var $label = _$figures_thumbs_div.find('label:contains("' + image_name + '")');
        if ($label.length == 0) {
            _.removePreviewImage();
        } else {
            var url = $label.find('input').val();
            url += '&t=' + new Date().getTime();
            // _$figure_PreviewImg.attr('src', url);
            _$figure_PreviewImg.attr('src', './static/img/Chasing arrows.gif');
            // _$figure_PreviewImg.attr('src', './static/img/Snake.gif');
            // _$figure_PreviewImg.attr('src', './static/img/Funnel.gif');
            // _$figure_PreviewImg.attr('src', './static/img/Radar.gif');
            var image = new Image;
            image.src = url;
            image.onload = function() {
                _$figure_PreviewImg.attr('src', url);
            }
        }
    };

    _.set_multi_figure_view = function(imageInfos, removeCallback) {
        _$figures_PreviewDiv.empty();
        for (var idx in imageInfos) {
            //noinspection JSUnfilteredForInLoop
            var image_info = imageInfos[idx];
            var url = image_info['url'];
            var name = image_info['name'];
            var width = image_info['width'];
            var height = image_info['height'];
            var $clone = _$figures_ThumbJQModel.clone();
            $clone.removeAttr('id');
            var $img = $clone.find('img');
            $img.attr('src', url);
            $img.on('mouseover', null, name, function(event) {
                _.select_figure_checkbox_with_name(event.data);
            });
            var $input = $clone.find('input');
            $input.val(name);
            $input.prop('checked', true);
            $input.change(function() {
                var $checkbox = $(this);
                var imgName = $checkbox.val();
                var key = _$figures_KeysDropDown.val();
                removeCallback(key, imgName);
            });
            $clone.find('label').append(name);
            $clone.appendTo(_$figures_PreviewDiv);
            $clone.append('width: ' + width + ' &nbsp; height: ' + height);
            // $clone.append('#' + idx + ' &nbsp; width: ' + width + ' &nbsp; height: ' + height);
        }
    };

    _.disable_elem = function($elem, bool) {
        $elem.prop('disabled', bool);
    };
};