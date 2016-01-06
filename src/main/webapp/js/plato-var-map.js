define(["underscore", "jquery"], function(_, $) {
    'use strict';

    var PlatoVarMap = function(options) {
        this.options = {};

        this.varMapRowTemplate = _.template(
            "<tr id='' class=''>" +
                "<td><%= varName %></td>" +
                "<td><%= varType %></td>" +
                "<td><%= varValue %></td>" +
            "</tr>");

        var self = this;
        $.extend(true, self.options, options);

        this.update = function(varMap) {
            // Remove all existing variables
            $("#" + self.options.tableId).find("tr:gt(0)").remove();

            var newRows = "";

            for (var varName in varMap) {
                if ( !varMap.hasOwnProperty(varName) ) {
                    continue;
                }

                var varType  = varMap[varName]["type"];
                var varValue = varMap[varName]["value"];

                var newRow = self.varMapRowTemplate({
                    "varName"  : varName,
                    "varType"  : varType,
                    "varValue" : varValue
                });

                newRows += newRow;
            }

            $("#" + self.options.headerId).after(newRows);


        };
    };

    return PlatoVarMap;
});