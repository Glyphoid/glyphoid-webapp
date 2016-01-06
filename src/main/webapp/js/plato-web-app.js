require.config({
    paths : {
        "jquery"                   : "libs/jquery-1.11.3.min",
        "bootstrap"                : "libs/bootstrap-3.3.5.min",
        "jquery-ui"                : "libs/jquery-ui/jquery-ui.min",
        "jquery-mobile"            : "libs/jquery.mobile-1.4.5",
        "underscore"               : "libs/underscore-1.8.2",
        "sprintf"                  : "libs/sprintf",
        "jquery.blockUI"           : "libs/jquery.blockUI",
        "aws-sdk"                  : "libs/aws/aws-sdk.min",
        "view-port"                : "view-port",
        "plato-gesture-manager"    : "plato-gesture-manager",
        "plato-grammar-manager"    : "plato-grammar-manager",
        "plato-var-map"            : "plato-var-map",
        "handwriting-engine-agent" : "handwriting-engine-agent",
        "mathjax-client"           : "mathjax-client",
        "token-display-names"      : "token-display-names",
        "limited-stack"            : "limited-stack",
        "state-stack"              : "state-stack",
        "vis"                      : "libs/vis/vis.min",
        "aws-helper"               : "aws-helper",
        "main-dev"                 : "main-dev",
        "dev-token-helper"         : "dev-tools/dev-token-helper"
    },
    shim : {
        "jquery.mobile" : {
            deps : ["jquery"]
        },
        "jquery.blockUI" : {
            deps : ["jquery"]
        }
    }
});

require(["jquery", "sprintf", "plato-gesture-manager", "plato-grammar-manager", "plato-var-map",
         "mathjax-client", "main-dev",
         "bootstrap", "jquery.blockUI"], /* jquery-mobile, jquery-ui, jquery.blockUI are plugins to jquery */
    function($, sprintf, PlatoGestureManager, PlatoGrammarManager, PlatoVarMap,
             MathJaxClient, MainDev) {
        'use strict';

        var debugLv = 0;

        // History of parsing results
        var parseResults = [];

        var showSpinner = function() {
            $("#ajaxLoading").css("visibility", "visible");
        };

        var hideSpinner = function() {
            $("#ajaxLoading").css("visibility", "hidden");
        };

        var mathJaxClient = new MathJaxClient();

        // Parsing history
        var parsingHistory = {
            history : []
        };

//        mathJaxClient.tex2mml("\\frac{1}{3}",
//        );

        // Plato variable map (table) instance
        var varMap = new PlatoVarMap({
            tableId        : "varMapTable",
            headerId       : "varMapTableHeader"
        });

        // Gesture manager instance
        var gestureManager = new PlatoGestureManager({
            elementId       : "gestureCanvas",
            varMap          : varMap,
            markStrokeOnset : false,
            onTouchDown  : function(coord) {
                if (debugLv > 0) {
                    console.log("touchDown event: (" + coord.x + ", " + coord.y + ")");
                }
            },
            onTouchMove  : function(coord) {
                if (debugLv > 0) {
                    console.log("touchMove event: (" + coord.x + ", " + coord.y + ")");
                }
            },
            onTouchUp    : function(coord) {
                if (debugLv > 0) {
                    console.log("touchUp event: (" + coord.x + ", " + coord.y + ")");
                }
            }
        });

        var gProdRowTemplate = _.template(
                "<tr class='graphicalProductionRow'>" +
                "<td>P<%= prodIdxOneBased %></td><td><span id=\"gProd_<%= prodIdx %>\"><%= sumString %></span><span><button style='float:right'>X</button></span></td>" +
                "</tr>");

        var grammarManager = new PlatoGrammarManager({
            graphicalProductionRowTemplate: gProdRowTemplate
        }, gestureManager);

        $("#printPaths").on("click", function(e) {
            e.preventDefault();

            var strokesJson = JSON.stringify(gestureManager.getPaths());
            if (debugLv > 0) {
                console.log("Paths = ", strokesJson);
            }
            $("#strokesDisplay").val(strokesJson);
            $("#writtenTokenJsonDisplay").val(gestureManager.getWrittenTokenJSON());
        });

        $("#clearPaths").on("click", function(e) {
            e.preventDefault();

            gestureManager.clear();
        });

        $("#removeLastPath").on("click", function(e) {
            e.preventDefault();

            gestureManager.removeLastPath();
        });

        $("#removeLastToken").on("click", function(e) {
            e.preventDefault();

            gestureManager.removeSelectedTokens();
        });

        $("#cursorSelect").on("click", function(e) {
            e.preventDefault();

            gestureManager.toggleCursorSelectMode();
        });

        var mergeLastNStrokes = function(N) {
            var strokeIndices = gestureManager.getStrokeIndices("last", N);
            if (strokeIndices) {
                gestureManager.mergeStrokesAsToken(strokeIndices);
            }
        };

        var unmergeLastStroke = function() {
            gestureManager.unmergeLastToken();
        };

        /**
         * Merge tokens. Used by cursor selection merge
         * @param tokenIndices Indices to the abstract tokens (i.e., tokens that may be node token)
         */
        var mergeStrokesByTokenIndices = function(tokenIndices) {
            // Translate abstract token indices to written token indices
            var wtIndices = gestureManager.abstract2WrittenTokenIndices(tokenIndices);

            var strokeIndices = [];
//            for (var i = 0; i < tokenIndices.length; ++i) {
//                var tokenIndex = tokenIndices[i];
//
//                var strokes = gestureManager.getConstituentStrokeIndices(tokenIndex);
//
//                for (var j = 0; j < strokes.length; ++j) {
//                    strokeIndices.push(strokes[j]);
//                }
//            }
            for (var i = 0; i < wtIndices.length; ++i) {
                var wtIndex = wtIndices[i];
                var strokes = gestureManager.getConstituentStrokeIndices(wtIndex);

                for (var j = 0; j < strokes.length; ++j) {
                    strokeIndices.push(strokes[j]);
                }
            }

            gestureManager.mergeStrokesAsToken(strokeIndices);
        };

        var undoStrokeCuratorUserAction = function() {
            gestureManager.undoStrokeCuratorUserAction();
        };

        var redoStrokeCuratorUserAction = function() {
            gestureManager.redoStrokeCuratorUserAction();
        };


        $("#mergeLast2Strokes").on("click", function(e) {
            e.preventDefault();

            if (gestureManager.cursorSelectedTokenIndices.length > 1) {
                // When there is a non-default selection of tokens, merge those selected tokens.
                mergeStrokesByTokenIndices(gestureManager.cursorSelectedTokenIndices);
            } else {
                mergeLastNStrokes(2);
            }
        });

        $("#mergeLast2StrokesAlt").on("click", function(e) {
            e.preventDefault();
            mergeLastNStrokes(2);
        });

        $("#mergeLast3Strokes").on("click", function(e) {
            e.preventDefault();
            mergeLastNStrokes(3);
        });

        $("#mergeLast4Strokes").on("click", function(e) {
            e.preventDefault();
            mergeLastNStrokes(4);
        });

        $("#unmergeLastToken").on("click", function(e) {
            e.preventDefault();
            unmergeLastStroke();
        });

        $("#undoStrokeCuratorUserAction").on("click", function(e) {
            e.preventDefault();
            undoStrokeCuratorUserAction();
        });

        $("#redoStrokeCuratorUserAction").on("click", function(e) {
            e.preventDefault();
            redoStrokeCuratorUserAction();
        });

        var parserEvaluatorOutputTemplate = _.template(
            "<tr class='platoTableMostRecentRow'>" +
                "<td><%= index %></td>" +
                "<td><%= stringizerOutput %></td>" +
                "<td><%= mathTex %></td>" +
                "<td id='mathML_id'><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img id='generatedImage_id' src='res/images/ajax-loader.gif'/></td>" +
                "<td><%= evaluatorOutput %></td>" +
                "<td><%= elapsedTimeMillis %></td>" +
            "</tr>");

        var parserEvaluatorOutputPendingTemplate = _.template(
            "<tr id='parserEvaluatorOutputPendingRow' class='platoTableMostRecentRow'>" +
                "<td></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
                "<td><img src='res/images/ajax-loader.gif'/></td>" +
            "</tr>");

        /**
         *
         * @param success           boolean: whether this is a success
         * @param successData       Object with fields: parseResult and elapsedMillis. Used iff success
         * @param errMsg            String: error message. Used iff !success
         */
        var parseTokenSetCallback = function(success, successData, errMsg) {
            var parseResult;
            var elapsedMillis;

            var timeoutMsg;
            if (success) {
                parseResult = successData["parseResult"];
                elapsedMillis = successData["elapsedMillis"];
            } else {
                timeoutMsg = "Parsing failed: " + errMsg;
                parseResult = {
                    stringizerOutput : timeoutMsg,
                    evaluatorOutput  : timeoutMsg,
                    mathTex          : timeoutMsg
                };

                elapsedMillis = "(Timeout)";
            }

            parseResults.push(parseResult);

            parseResult.index = parsingHistory.history.length + 1; // 1-based
            parseResult.elapsedTimeMillis = elapsedMillis < 1000 ?
                    "" + elapsedMillis + " ms" :
                    "" + elapsedMillis / 1000 + " s";

            // Save the parsing result to the record
            parsingHistory.history.push(parseResult);

            var newResultRow = parserEvaluatorOutputTemplate(parseResult);

            $("#parserEvaluatorOutputPendingRow").remove();       // Removing pending row
            $("#parseEvalResultTable tr").removeClass("platoTableMostRecentRow"); // Remove most-recent-row class from all existing rows
            $("#parseEvalResultTableHeader").after(newResultRow); // Add new result row

            var mathMLElemId = "mathML_" + parseResult.index;
            $("#mathML_id").attr("id", mathMLElemId);
            var mathMLElem = $("#" + mathMLElemId);

                var generatedImageElemId = "generatedImage_" + parseResult.index;
                $("#generatedImage_id").attr("id", generatedImageElemId);
                var generatedImageElem = $("#" + generatedImageElemId);

            if (success) {
                // Submit AJAX call to ML conversion service
                getMathML(function (conversionResult) {
                    mathMLElem.text(conversionResult);
                    console.log("In tex2mml success callback: ", conversionResult);
                });

                // Submit AJAX call to image generation service
                getGeneratedImage(function (conversionResult) {
                    generatedImageElem.attr("src", "data:image/png;base64," + conversionResult);
                });

            } else {
                mathMLElem.html("");
                generatedImageElem.attr("src", "");
            }
        };

        // Callback for parseTokenSet button
        $("#parseTokenSet").on("click", function(e) {
            e.preventDefault();

            $("#parseEvalResultTableHeader").after(parserEvaluatorOutputPendingTemplate);

            gestureManager.parseTokenSet(
                function(parseResult, elapsedMillis) { /* Callback for parsing success */
                    parseTokenSetCallback(true, {
                        "parseResult"   : parseResult,
                        "elapsedMillis" : elapsedMillis
                    }, null);

                },
                function(errMsg) {  /* Callback for parsing failure */
                    parseTokenSetCallback(false, null, errMsg);
                }
            );
        });

        // Callback for parseTokenSet button
        $("#parseSelected").on("click", function(e) {
            e.preventDefault();

            $("#parseEvalResultTableHeader").after(parserEvaluatorOutputPendingTemplate);

            if (gestureManager.cursorSelectedTokenIndices.length === 0) {
                throw new Error("Unable to perform subset parsing because no token is selected");
            }

            gestureManager.parseTokenSubset(
                gestureManager.cursorSelectedTokenIndices,
                function(parseResult, elapsedMillis) { /* Callback for parsing success */
                    parseTokenSetCallback(true, {
                        "parseResult"   : parseResult,
                        "elapsedMillis" : elapsedMillis
                    }, null);
                },
                function(errMsg) {  /* Callback for parsing failure */
                    parseTokenSetCallback(false, null, errMsg);
                }
            );

        });

        /* Math ML tab */
        var isMathMLTabActive = function() {
            return isPillActive("mathMLTab");
        };

        var isGeneratedImageActive = function() {
            return isPillActive("generatedImageTab");
        };

        var isPillActive = function(pillId) {
            var classes = $("#" + pillId).attr("class").split(" ");

            return classes.indexOf("active") !== -1;
        };

        var getMathML = function(successCallback, errorCallback) {
            if (parseResults.length > 0) {
                mathJaxClient.tex2mml(parseResults[parseResults.length - 1].mathTex,
                    function(responseJSON) {
                        successCallback(responseJSON.conversionResult);
                    },
                    function() {
                        errorCallback();
                    });

                hideSpinner(); // Hide the main spinner during MathML conversion
            }
        };

        var getGeneratedImage = function(successCallback, errorCallback) {
            if (parseResults.length > 0) {
                mathJaxClient.tex2png(parseResults[parseResults.length - 1].mathTex,
                    function(responseJSON) {
                        console.log("In tex2mml success callback: ", responseJSON);

                        successCallback(responseJSON.conversionResult);
                    },
                    function() {
                        errorCallback(); //TODO
                    });

                hideSpinner(); // Hide the main spinner during image generation
            }
        };

        $("#mathMLTabIndex").on("click", function(e) {
            getMathML();
        });

        $("#generatedImageTabIndex").on("click", function(e) {
            getGeneratedImage();
        });

        /* Force set token name */
        $("#forceSetLastToken").on("click", function(e) {
            e.preventDefault();

            var forceSetTokenIdx = gestureManager.tokenNames.length - 1;
            var forceSetTokenName = $("#forceSetTokenName").val().trim();

            console.log("Force set last token to \"" + forceSetTokenName +"\""); //DEBUG

            gestureManager.forceSetTokenRecogWinner(forceSetTokenIdx, forceSetTokenName);
        });

        $("#recogToken").on("click", function(e) {
            e.preventDefault();

            if (debugLv > 0) {
                console.log("Issuing AJAX call to token recognizer");
            }

            //TODO: Check if the token is empty

            var t0 = new Date();
            $.ajax({
                url      : gestureManager.endpoints.tokenRecognizer,
                method   : "POST",
                data     : gestureManager.getWrittenTokenJSON(),
                complete : function(resp) {
                    if (debugLv > 0) {
                        console.log("Token recognizer response =", resp);
                    }

                    var t1 = new Date();
                    var elapsedMillis = t1.getTime() - t0.getTime();
                    if (resp.status === 200) {
                        if (typeof resp.responseJSON.winnerTokenName === "string") {
                            $("#tokenRecogWinner").val(resp.responseJSON.winnerTokenName + " (" + elapsedMillis + " ms)");

                            /* Sort the candidates */
                            var cands = resp.responseJSON.recogPVals;
                            cands.sort(function(x, y) {
                                return y[1] - x[1];
                            });

                            var candsCount = 5;
                            var len = (cands.length > candsCount) ? candsCount : cands.length;

                            cands = cands.slice(0, len);

                            createTokenCandidateButtons(cands);
                        }
                        else {
                            $("#tokenRecogWinner").val("[Failed to parse token recognizer response]");
                        }
                    }
                    else {
                        $("#tokenRecogWinner").val("[Error occurred during token recognition]");
                    }
                }
            });
        });

        /* Callback functions of navigation (e.g., zoom/pan) buttons */
        var relativeFovPanStep = 0.0625;
        var fovZoomStep = 0.10;

        $("#zoomIn").on("click", function(e) {
            gestureManager.fovZoomRedraw(-fovZoomStep);
        });

        $("#zoomOut").on("click", function(e) {
            gestureManager.fovZoomRedraw(fovZoomStep);
        });

        $("#moveLeft").on("click", function(e) {
            e.preventDefault();

            gestureManager.relativeFovPanRedraw(relativeFovPanStep, 0); // TODO: Remove magic number
        });

        $("#moveRight").on("click", function(e) {
            e.preventDefault();

            gestureManager.relativeFovPanRedraw(-relativeFovPanStep, 0); // TODO: Remove magic number
        });

        $("#moveDown").on("click", function(e) {
            e.preventDefault();

            gestureManager.relativeFovPanRedraw(0, -relativeFovPanStep); // TODO: Remove magic number
        });

        $("#moveUp").on("click", function(e) {
            e.preventDefault();

            gestureManager.relativeFovPanRedraw(0, relativeFovPanStep); // TODO: Remove magic number
        });



        //$("#gestureCanvas").on("mousemove", function(e) {
        //    var x = e.offsetX;
        //    var y = e.offsetY;
        //    console.log("mousemove event: (" + x + ", " + y + ")");
        //});

        /* Disable right-click context menu for canvas */
        $("#gestureCanvas").bind("contextmenu", function(e){
            e.preventDefault();
            return false;
        });

        $("body").on("contextmenu", "img", function(e){
            e.preventDefault();
            return false;
        });


        $(window).bind("beforeunload", function() {
            gestureManager.removeEngine();
        });

        $(window).bind("unload", function() {
            gestureManager.removeEngine();
        });




        $(function() {
            if ( !window.opera ) {
                $("body").attr("oncontextmenu", "return false;");
            }

            $.unblockUI();

            $(document).on({
                ajaxStart : function() {

                    showSpinner();
                },
                ajaxStop  : function() {
//                    $.unblockUI(); // Don not unblock UI too early.
                    hideSpinner();
                }
            });

            // Button group placement
            $("#parseDropdown").offset({
                left: $("#parseTokenSet").offset().left + $("#parseTokenSet").width() + 26,
                top: $("#parseTokenSet").offset().top
            });

            $("#mergeStrokesDropdown").offset({
                left: $("#mergeLast2Strokes").offset().left + $("#mergeLast2Strokes").width() + 26,
                top: $("#mergeLast2Strokes").offset().top
            });

            $(".textDisplay").attr("disabled", true);
            $("#tokenRecogWinner").attr("disabled", true);

            /* Callback for button "Set token" */
            $("#setToken").on("click", function(e) {
                e.preventDefault();

                $("#allTokens").modal("show");
            });

            /* Help button */
            $("#helpButton").on("click", function(e) {
                e.preventDefault();

                $("#helpInfo").modal("show");
            });

            /* Mobile Downloads button */
            $("#mobileDownloadsButton").on("click", function(e) {
                e.preventDefault();

                $("#mobileDownloads").modal("show");
            });

            /* TODO: Do not us global variables */
            window.blockCanvasScroll = false;
            $(window).on("touchstart", function(e) {
                if ($(e.target).closest('#gestureCanvas').length === 1) {
                    window.blockCanvasScroll = true;
                }
            });
            $(window).on("touchend", function() {
                window.blockCanvasScroll = false;
            });
            $(window).on("touchmove", function(e) {
                if (window.blockCanvasScroll) {
                    e.preventDefault();
                }
            });
            $(window).on("keydown", function(e) {
                if (e.keyCode === 27) {
                    // Escape key: Cancels the all tokens (select token) dialog
                    $("#allTokens").modal("hide");

                    $("#helpInfo").modal("hide");
                    $("#mobileDownloads").modal("hide");
                }
            });

            $("#grammar").on("click", function(e) {
                e.preventDefault();

                grammarManager.init();
            });

            // Modal dialog close callbacks
            $("#allTokensCancel").on("click", function(e) {
                e.preventDefault();
                $("#allTokens").modal("hide");
            });

            $("[id^=helpInfoCancel]").on("click", function(e) {
                e.preventDefault();
                $("#helpInfo").modal("hide");
            });

            $("[id^=mobileDownloadsCancel]").on("click", function(e) {
                e.preventDefault();
                $("#mobileDownloads").modal("hide");
            });


            gestureManager.updateUIControlState();
        });

        var mainDev = new MainDev(gestureManager);
    }
 );