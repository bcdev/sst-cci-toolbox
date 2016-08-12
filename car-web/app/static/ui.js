/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_COMMON_UI = function() {
    var _ = this;

    var _$overlay_div = $('#overlay');
    var _$messageBox_div = $('#message_box');
    var _$msgBoxMsg_pre = $('#msg_box_msg');
    var _$images_div = $('#figures_thumbs_div');
    var _$figurePreview_img = $('#figures_preview_img');

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

    _.show_message = function(msg) {
        _.show_overlay();
        _.show_elem(_$messageBox_div);
        _$msgBoxMsg_pre.text(msg);
        _.centerAtWindow(_$messageBox_div);
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
        _$images_div.find('input').prop('checked', false);
    };

    _.removePreviewImage = function() {
        _$figurePreview_img.removeAttr('src');
    };

    function bindEvents() {
        _$messageBox_div.find('button').click(function() {
            _.hide_message();
        });
    }
    bindEvents();
};