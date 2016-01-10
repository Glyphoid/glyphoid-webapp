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

                var varDisplayName = self.getVarDisplayName(varName);

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

        /**
         * Get the display name of a variable, taking into account the display name map and subscripts
         * @param varName   Original (internal) var name
         * @return          Display name of the variable
         */
        this.getVarDisplayName = function(varName) {
            var displayName;

            for (var key in displayNameMap) {
                if (varName.indexOf(key) != -1) {
                    varName = varName.replace(key, displayNameMap[key]);
                }
            }

            if (varName.indexOf("_") != -1) {
                displayName = varName.substring(0, varName.indexOf("_")) + "<sub>" +
                              varName.substring(varName.indexOf("_") + 1, varName.length) + "</sub>";
            } else {
                displayName =  varName;
            }

            return displayName;
        };
    };

    return PlatoVarMap;
});