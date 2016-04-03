define(["underscore", "jquery", "vis"], function(_, $, vis) {
    'use strict';

    var PlatoGrammarManager = function(options, gestureManager) { // PlatoGestureManager constructor
        var self = this;

        this.options = {
            grammarTable  : $("#grammarTable"),
            grammarSelectTable : $("#grammarSelectTable"),

            colorLHS      : "#AABBFF",
            colorProd     : "#97C2FC",
            colorTerm     : "#00FF00",
            colorLHS2Prod : "#808080",
            widthHead     : 4,
            widthNonHead  : 1
        };

        $.extend(true, self.options, options);

        this.grammarTable = this.options.grammarTable;
        this.grammarSelectTable = this.options.grammarSelectTable;
        this.gestureManager = gestureManager;

        this.gProds = null;  // Array of graphical productions

        this.selectGrammarNodes = [];
        this.selectGrammarEnabled = [];

        this.grammarSelectChanged = function(e) {
            var grammarCheckboxes = self.grammarSelectTable.find("input[id^=grammarSelect_]");

            var grammarNodesToDisable = [];

            for (var i = 0; i < grammarCheckboxes.length; ++i) {
                var checkbox = $(grammarCheckboxes[i]);

                var checkboxId = checkbox.attr("id");
                var grammarNodeName = checkboxId.replace("grammarSelect_", "").toUpperCase();

                self.selectGrammarNodes.push(grammarNodeName.toUpperCase());

                if (!checkbox.prop("checked")) {
                    grammarNodesToDisable.push(grammarNodeName);
                }
            }

            console.log("To disable: ", grammarNodesToDisable);

            self.gestureManager.hwEngAgent.enableAllProductions(
                function (responseJSON) {
                },
                function (responseJSON) {
                    console.log("ERROR: Failed to enable all productions");
                });
            self.gestureManager.hwEngAgent.disableProductionsByGrammarNodeNames(
                grammarNodesToDisable,
                function (responseJSON) {
                    console.log("Successfully disabled " + grammarNodesToDisable.length + " grammar node(s)");
                },
                function (responseJSON) {
                    console.log("ERROR: Failed to disable grammar nodes");
                });
        };

        this.enableAllGrammarNodes = function() {
            self.selectGrammarNodes = [];
            self.selectGrammarEnabled = [];

            var grammarCheckboxes = self.grammarSelectTable.find("input[id^=grammarSelect_]");

            for (var i = 0; i < grammarCheckboxes.length; ++i) {
                var checkbox = $(grammarCheckboxes[i]);

                var checkboxId = checkbox.attr("id");
                var grammarNodeName = checkboxId.replace("grammarSelect_", "").toUpperCase();

                self.selectGrammarNodes.push(grammarNodeName);

                checkbox.prop("checked", true);
                self.selectGrammarEnabled.push(true);

                /* Wire up checkbox callbacks */
                checkbox.on("change", self.grammarSelectChanged);
            }

        };

        this.init = function() {
            // Obtain all the graphical productions
            if (self.gProds == null) {
                gestureManager.getGraphicalProductions( // TODO: Refactor into separate JS file
                    function (gProds, ellapsedMillis) { // gProds: Graphical productions
                        if (!Array.isArray(gProds)) {
                            throw new Error("Unexpected input: gProds is not an array");
                        }

                        self.gProds = gProds;

                        self.renderGraphicalProductionsTable();
                    },
                    function (errorMsg) {
                        // TODO:
                    }
                );
            }

        };

        var getProdNetworkDataSets = function(i) {
            var rhs = self.gProds[i].rhs;
            if (!Array.isArray(rhs)) {
                throw new Error("rhs ought to be a an array, but is not");
            }

            var nodes = [];
            var edges = [];
            var nodeIds = [];

            var lhs = self.gProds[i].lhs;

            // LHS
            var lhsName = "lhs_" + lhs;
            nodes.push({
                id    : lhsName,
                label : lhs,
                color : self.options.colorLHS
            });
            nodeIds.push(lhsName);

            // production
            var prodId = "P" + (i + 1);
            nodes.push({
                id    : prodId,
                label : prodId,
                color : self.options.colorProd
            });
            nodeIds.push(prodId);

            // edge: LHS->production
            edges.push({
                from   : "lhs_" + lhs,
                to     : prodId,
                dashes : true,
                arrows : "to"
            });

            // Edges for lhs-rhs connections
            for (var rhsIdx in rhs) {
                var rhsName = rhs[rhsIdx];
                var rhsIsTerminal = self.gProds[i].rhsIsTerminal[rhsIdx];

                var targId;
                var targColor;
                if (!rhsIsTerminal) {
                    targId = "lhs_" + rhsName + "_" + rhsIdx;
                    targColor = self.options.colorLHS;
                } else {
                    targId = "term_" + rhsName + "_" + rhsIdx;
                    targColor = self.options.colorTerm;
                }

                if (nodeIds.indexOf(targId) === -1) {
                    nodes.push({
                        id: targId, // Terminal ID
                        label: rhsName,
                        color: targColor
                    });
                }

                edges.push({
                    from   : prodId,
                    to     : targId,
                    arrows : "to",
                    width  : rhsIdx == 0 ? self.options.widthHead : self.options.widthNonHead
                });
            }

            return {
                nodes : new vis.DataSet(nodes),
                edges : new vis.DataSet(edges)
            }
        };

        var renderGraphicalProduction = function(i) {
            var prodDataSets = getProdNetworkDataSets(i);

            var options = {
                height : "600px",
                width  : "450px"
            };

            var container = document.getElementById("grammarGraph");
            var network = new vis.Network(container, prodDataSets, options);

        };

        var gProdClickCallback = function(i, e) {
            console.log("Hello from gProd " + i);

            renderGraphicalProduction(i);
        };

        this.renderGraphicalProductionsTable = function() {
            if ( !self.options.graphicalProductionRowTemplate ) {
                throw new Error("No valid graphicalProductionRowTemplate is available");
            }

            for (var i = 0; i < self.gProds.length; ++i) {
                var gProd = self.gProds[i];

                var newProdRow = self.options.graphicalProductionRowTemplate({
                    prodIdx         : i,
                    prodIdxOneBased : i + 1,
                    sumString       : gProd.sumString
                });

                $("#grammarTableFooter").before(newProdRow);

                var gProdElem = $("#gProd_" + i);

                gProdElem.on("click", gProdClickCallback.bind(self, i));

            }
        };


    };

    return PlatoGrammarManager;
});
