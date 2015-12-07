define(["underscore", "jquery", "vis"], function(_, $, vis) {
    'use strict';

    var PlatoGrammarManager = function(options, gestureManager) { // PlatoGestureManager constructor
        var self = this;

        this.options = {
            grammarTable  : $("#grammarTable"),

            colorLHS      : "#AABBFF",
            colorProd     : "#97C2FC",
            colorTerm     : "#00FF00",
            colorLHS2Prod : "#808080",
            widthHead     : 4,
            widthNonHead  : 1
        };

        $.extend(true, self.options, options);

        this.grammarTable = this.options.grammarTable;
        this.gestureManager = gestureManager;

        this.gProds = null;  // Array of graphical productions


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

//                    self.termNames = new Array();     // Terminal names
//                    var lhsNames = new Array();     // LHS names
//                    var prodNames = new Array();     // Productions
//                    var numProds = {};
//
//                    var nodes = new Array();
//                    var edges = new Array();
//
//                    for (var i = 0; i < gProds.length; ++i) {
//                        var lhs = gProds[i].lhs;
//                        var rhs = gProds[i].rhs;
//
//                        if (!Array.isArray(rhs)) {
//                            throw new Error("rhs ought to be a an array, but is not");
//                        }
//
//                        var sumString = gProds[i].sumString;
//                        var prodId;
//
//                        if (lhsNames.indexOf(lhs) === -1) { // The first time the lhs has been encountered
//
//                            lhsNames.push(lhs);
//
//                            // Add the LHS
//                            var lhsId = "lhs_" + lhs;
//                            nodes.push({
//                                id: lhsId,
//                                label: lhs,
//                                color: colorLHS
//                            });
//
//                            prodNames.push(sumString);
//
//                            // Add the production
//                            prodId = "prod_" + prodNames.length;
//                            nodes.push({
//                                id: prodId,
//                                label: "P1",
//                                color: colorProd
//                            });
//
//                            // Edge from the LHS to the prod
//                            edges.push({
//                                from: lhsId,
//                                to: prodId,
//                                physics: false,
//                                length: edgeLenLHS2Prod,
//                                color: colorLHS2Prod,
//                                width: 1,
//                                dashes: true
//                            });
//
//                            numProds[lhs] = 1;
//                        } else {
//                            // Add the production
//                            var lhsId = "lhs_" + lhs;
//
//                            // Add the production
//                            prodNames.push(sumString);
//
//                            numProds[lhs]++;
//
//                            prodId = "prod_" + prodNames.length;
//                            nodes.push({
//                                id: prodId,
//                                label: "P" + numProds[lhs],
//                                color: colorProd
//                            });
//
//                            // Edge from the LHS to the prod
//                            edges.push({
//                                from: lhsId,
//                                to: prodId,
//                                physics: false,
//                                length: edgeLenLHS2Prod,
//                                color: colorLHS2Prod,
//                                width: 1,
//                                dashes: true
//                            });
//
//                        }
//
//                        // Edges for lhs-rhs connections
//                        for (var rhsIdx in rhs) {
//                            var rhsName = rhs[rhsIdx];
//                            var rhsIsTerminal = gProds[i].rhsIsTerminal[rhsIdx];
//
//                            var targId;
//                            if (!rhsIsTerminal) {
//                                targId = "lhs_" + rhsName;
//                            } else {
//                                targId = "term_" + rhsName;
//
//                                if (self.termNames.indexOf(rhsName) === -1) { // Encountering new terminal
//                                    nodes.push({
//                                        id: targId, // Terminal ID
//                                        label: rhsName,
//                                        color: colorTerm
//                                    });
//
//                                    self.termNames.push(rhsName);
//                                }
//                            }
//
//                            edges.push({
//                                from: prodId,
//                                to: targId,
//                                arrows: "to",
//                                width: rhsIdx == 0 ? widthHead : widthNonHead
//                            });
//                        }
//                    }
//
//                    var lhsNodesDataSet = new vis.DataSet(nodes);
//                    var edgesDataSet = new vis.DataSet(edges);
//
//                    // create a network
//                    var container = document.getElementById('grammarGraph');
//                    var grammarGraphData = {
//                        nodes: lhsNodesDataSet,
//                        edges: edgesDataSet
//                    };
//                    var options = {
//                        height: Number($("#gestureCanvas").attr("height")) * 2,
//                        width: "100%"
//                    };
//                    var network = new vis.Network(container, grammarGraphData, options);