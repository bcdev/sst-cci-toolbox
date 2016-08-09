/**
 * Created by sabine.embacher@brockmann-consult.de on 09.08.2016.
 */

CAR_COMMON_UI = function() {
    this.show_elem_placed = function(parent, elem) {
        var pos = parent.position();
        var left = pos.left - 5;
        var top = pos.top - 5;
        elem.css({'left': left, 'top': top});
        this.show_elem(elem);
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
        this.show_elem($('#overlay'));
    };

    this.hide_overlay = function() {
        this.hide_elem($('#overlay'));
    };

    this.show_message = function(msg) {
        this.show_overlay();
        var box = $('#message_box');
        this.show_elem(box);
        $('#msg_box_msg').text(msg);
        this.centerAtWindow(box);
    };

    this.hide_message = function() {
        this.close_dialog($('#message_box'));
    };

    this.close_dialog = function($component) {
        this.hide_overlay();
        this.hide_elem($component);
    };

    this.centerAtWindow = function(elem) {
        viewport = $(window);
        var vh = viewport.height();
        var vw = viewport.width();
        var eh = elem.height();
        var ew = elem.width();
        var top = (vh - eh) / 3;
        var left = (vw - ew) / 2;
        elem.css({'left': left, 'top': top});
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

    this.uncheck_all_image_checkboxes = function() {
        $('#images').find('input').prop('checked', false);
    };
};