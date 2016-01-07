define(["underscore", "jquery", "token-display-names"], function(_, $, displayNameMap) {
    'use strict';

    var PlatoVarMap = function(options) {
        this.options = {};

        this.varMapRowTemplate = _.template(
            "<tr id='' class='<%= rowClass %>'>" +
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

                var isNew    = varMap[varName]["isNew"];
                var rowClass = "platoVarMapTableRowOld";
                if (typeof isNew === "boolean" && isNew) {
                    rowClass = "platoVarMapTableRowNew";
                }


                var varDisplayName;
                if (typeof displayNameMap[varName] === "string") {
                    varDisplayName = displayNameMap[varName];
                } else {
                    varDisplayName = varName;
                }

                var newRow = self.varMapRowTemplate({
                    "varName"  : varDisplayName,
                    "varType"  : varType,
                    "varValue" : varValue,
                    "rowClass" : rowClass
                });

                newRows += newRow;
            }

            $("#" + self.options.headerId).after(newRows);


        };
    };

    return PlatoVarMap;
});