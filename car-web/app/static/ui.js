/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_COMMON_UI = function() {
    var _ = this;

    var _$overlay_div = $('#overlay');
    var _$messageBox_div = $('#message_box');
    var _$msgBoxMsg_pre = $('#msg_box_msg');
    var _$msgContent_div = $('#msg_content');
    var _$figures_thumbs_div = $('#figures_thumbs_div');
    var _$figurePreview_img = $('#figures_preview_img');
    var _$yesNoBox_div = $('#yes_no_box');

    _.show_elem_placed = function(parent, elem) {
        var pos = parent.position();
        var left = pos.left - 5;
        var top = pos.top - 5;
        elem.css({'left': left, 'top': top});
        _.show_elem(elem);
    };

    _.show_elem = function(elem) {
        elem.css({'visibility': 'visible'});
        // elem.show(300);
    };

    _.hide_elem = function(elem) {
        elem.css({'visibility': 'hidden'});
        // elem.hide('fast');
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

    _.select_figure_checkbox_with_value = function(value) {
        var $label = _$figures_thumbs_div.find('label:contains("' + value + '")');
        $label.find('input').prop('checked', true);
        var $thumb_div = $label.parent();
        _.scroll_to_figure_thumb($thumb_div);
    };

    _.scroll_to_figure_thumb = function($thumb_div) {
        var scroll_speed = 500; // milliseconds
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
        _$figures_thumbs_div.animate({scrollTop: new_scroll_top}, 'fast');
    };

    _.removePreviewImage = function() {
        _$figurePreview_img.removeAttr('src');
    };

    _.set_preview_image_for_name = function(image_name) {
        var $label = _$figures_thumbs_div.find('label:contains("' + image_name + '")');
        if ($label.length == 0) {
            _.removePreviewImage();
        } else {
            var url = $label.find('input').val();
            _$figurePreview_img.attr('src', url);
        }
    };
};