define(["underscore", "jquery", "sprintf", "handwriting-engine-agent", "view-port", "token-display-names", "state-stack",
        "pinch-event",
        "dev-token-helper",
        "jquery.blockUI", "bootstrap"], /* jquery.blockUI is a plugin to jquery */
    function(_, $, sprintf, HandwritingEngineAgent, ViewPort, TokenDisplayNames, StateStack,
             PinchEvent,
             DevTokenHelper) {
        'use strict';

        /* Exported object */
        var TouchManager = function(options) {
            var self = this;

            this.options = {
                markStrokeOnset  : true,
                elementId        : "theCanvas",
                onTouchDown      : null,
                onTouchMove      : null,
                onTouchUp        : null,

                endpoints        : {
//                    tokenRecognizer : "http://127.0.0.1/plato/token-recog" //DEBUG
                    tokenRecognizer : "token-recog"
                },

                drawOptions      : {
                    drawStrokeBoxLabels : false,

                    strokeBoxStyle           : "#303030",
                    strokeStyle              : "#0000FF",
                    tokenBoxStyle            : "#303030",
                    fontStyle                : "#0000FF",

                    cursorSelectBoxStyle     : "#008022",
                    selectedTokenBoxStyle    : "#008022",
                    selectedFontStyle        : "#008022",

                    defaultFontPoints : 20,
                    fontScaleFactor   : null
                },

                stateStackCapacity : 20,     // TODO: Link with the Java state stack capacity

                enableEdgeGuard : true,      // True by default, but for dev, it needs to be set to false;
                edgeGuardWidth  : 5

            };

            this.mouseButton = {
                left   : 0,
                middle : 1,
                right  : 2
            };      // TODO: MS IE

            this.currentMouseButton;
            this.hasBeenMouseDown = false;
            this.hasBeenTouchStart = false;

            this.allTokens = []; /* Token names and display names */

            $.extend(true, self.options, options);

            this.element = $("#" + options.elementId);

            /* Height and width of the canvas */
            this.canvasWidth  = this.element.prop("width");
            this.canvasHeight = this.element.prop("height");

            this.debugLv = 0;

            /* Create instance of handwriting engine agent */
            if (typeof self.options.handwritingEngineAgent === "object" && self.options.handwritingEngineAgent !== null) {
                this.hwEngAgent = self.options.handwritingEngineAgent;
            } else {
                this.hwEngAgent = new HandwritingEngineAgent({
                    getSerializedStateCallback: function () {
                        return self.getSerializedState();
                    }
                });
            }


            /* Initialization, get new backend handwriting engine */
            /* Obtain the handwriting engine UUID */
            self.hwEngAgent.getNewEngine(
                function() {
                    console.log("Creation of handwriting engine succeeded");

                    /* Get all token names */
                    self.hwEngAgent.getAllTokenNames(
                        function(responseJSON) {
                            var allTokenNames = responseJSON.allTokenNames;

                            /* Get the corresponding token display names */
                            self.allTokens = _.map(allTokenNames, function(tokenName) {
                                var displayName;

                                if (TokenDisplayNames.hasOwnProperty(tokenName) &&
                                    typeof TokenDisplayNames[tokenName] === "string") {
                                    displayName = TokenDisplayNames[tokenName];
                                } else {
                                    displayName = tokenName;
                                }

                                return {
                                    name        : tokenName,
                                    displayName : displayName
                                };
                            });

                            /* Sort by token display names */
                            self.allTokens.sort(function(a, b) {
                                if (a.displayName < b.displayName) {
                                    return -1;
                                } else if (a.displayName > b.displayName) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            });

                            /* Create all the token buttons */
                            for (var i = 0; i < self.allTokens.length; ++i) {
                                var tokenName = self.allTokens[i].name;
                                var tokenDisplayName = self.allTokens[i].displayName;

                                var btnId = "tokenName_" + i;
                                var btn = "<button class=\"btn btn-default\" tokenName=\"" + tokenName
                                          + "\" id=\"" + btnId + "\">";
                                btn += tokenDisplayName;
                                btn += "</button>";

                                /* Append button element */
                                $("#allTokens").append(btn);

                                /* Dividers between different categories of tokens */
                                if (tokenName === "/") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "9") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "=") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "Y") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "z") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "]") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "gr_Om") {
                                    $("#allTokens").append("<br />");
                                } else if (tokenName === "gr_omega") {
                                    $("#allTokens").append("<br />");
                                }


                                /* Bind click callback */
                                $("#" + btnId).on("click", function(e) {
                                    e.preventDefault();

                                    var target = $(e.target);
                                    var tokenName = target.attr("tokenName");
                                    console.log("tokenName = " + tokenName);

                                    console.log("Setting token name to " + tokenName + "(" + tokenDisplayName + ")"); //DEBUG

                                    $("#allTokens").modal("hide");

                                    forceSetTokenNameFunc(tokenName);
                                });
                            }

//                            $("#allTokens").dialog();

                            console.log("getAllTokenNames succeeded");
                        },
                        function() {
                            console.error("getAllTokenNames failed");
                        });


                },
                function() {
                    console.error("Creation of handwriting engine failed");
                }
            );

//          this.elementId = options.elementId;
            this.el = $("#" + self.options.elementId);
            this.elLeft = this.el.offset().left;
            this.elTop  = this.el.offset().top;

            this.canvasEl = document.getElementById(self.options.elementId);

            /* Cursor select state */
            this.cursorSelectMode = false;
            this.cursorSelectOriginWorld = [];  // Length-2 array for the cursor selection origin point (where the mouse first clicked), in world coordinates: [x, y]
            this.cursorSelectCurrentWorld =[];  // Length-2 array for the cursor selection current point (where the mouse is), in world coordinates: [x, y]
            this.cursorSelectBoxWorld = [];     // Length-4 array for the cursor selection box, in world coordinates: [xmin, ymin, xmax, ymax]

            this.cursorSelectedTokenIndices = [];

            /* Touch states */
            this.touchDown = false;
            this.pts = [];

            /* Multi-touch states */
            this.pinchMode = false;
            this.hasBeenPinch = false;
            this.pinchPrevPos = null;
            this.pinchEvent = new PinchEvent();

            this.rightButtonStatus = null;  // Possible values: null (not engaged), "fovPan", "tokenMove"
            this.rightButtonTokenMoveIdx = null;
            // An array of indices (with a regions selection active, the length can be > 1)

            this.origRightButtonPos = new Array(2);
            this.origMovedTokenBoundsArray; // An array of array. Each element is the original bounds of a token being moved.
            this.origMovedStrokes;
            // This is a "hashmap" of stroke positions. The key is the string representation of the token index. The value is the original stroke data.

            this.lastRightButtonPos = new Array(2);

            /* Stroke data */
            this.strokes = [];      /* Stroke paths */
            this.strokeBounds = []; /* Stroke bounds */
            this.strokeTokenOwners = []; /* Ownership of the strokes (by tokens) */
            this.constStrokes = []; /* Constituent strokes of tokens */

            /* State stack for local-only states such as strokes */
            this.stateStack = new StateStack(this.options.stateStackCapacity);

            /* Token data, obtained from a back-end stroke curator */
            this.tokenNames = [];
            this.tokenBounds = [];

            this.viewPort = new ViewPort({
                width     : this.canvasWidth,
                height    : this.canvasHeight
            });

            this.getConstituentStrokeIndices = function(tokenIdx) {
                /* Input sanity check */
                if (typeof tokenIdx !== "number") {
                    throw "Invalid input";
                }

                if (tokenIdx >= self.tokenBounds.length) {
                    throw "Token index out of bounds";
                }

                var strokeIndices = new Array();
                for (var i = 0; i < self.strokeTokenOwners.length; ++i) {
                    if (self.strokeTokenOwners[i] === tokenIdx) {
                        strokeIndices.push(i);
                    }
                }

                return strokeIndices;
            };

            /* Methods */
            this.toggleCursorSelectMode = function() {
                self.cursorSelectMode = !self.cursorSelectMode;

                self.cursorSelectOriginWorld  = [];
                self.cursorSelectCurrentWorld = [];
                self.cursorSelectBoxWorld     = [];

                self.redraw();
            };

            /**
             *  Move a token, together with its constituting strokes
             *  @param          tokenIdx: token index
             *  @param          origBounds: original bounds
             *  @param          worldDx: amount of shift along x-axis, in world coordinates
             *  @param          worldDy: amount of shift along y-axis, in world coordinates
             *  */
            this.moveToken = function(tokenIdx, origBounds, worldDx, worldDy) {
                /* Input sanity check */
                if (typeof tokenIdx !== "number" || typeof worldDx !== "number" || typeof worldDy !== "number") {
                    throw "Invalid input";
                }

                /* Check that tokenIdx is within bound */
                if (tokenIdx >= self.tokenBounds.length) {
                    throw "Token index out of bound";
                }

                /* Move token bounds */
                self.tokenBounds[tokenIdx][0] = origBounds[0] + worldDx;
                self.tokenBounds[tokenIdx][1] = origBounds[1] + worldDy;
                self.tokenBounds[tokenIdx][2] = origBounds[2] + worldDx;
                self.tokenBounds[tokenIdx][3] = origBounds[3] + worldDy;

                /* Determine the constituting stroke indices */
                var strokeIndices = self.getConstituentStrokeIndices(tokenIdx);

                /* Move the strokes */
                var tokenIdxStr = "s" + String(tokenIdx);
                var origMovedStrokes = self.origMovedStrokes[tokenIdxStr];

                for (var i = 0; i < strokeIndices.length; ++i) {
                    var strokeIdx = strokeIndices[i];

                    for (var j = 0; j < self.strokes[strokeIdx].length; ++j) {
                        self.strokes[strokeIdx][j][0] = origMovedStrokes[i][j][0] + worldDx;
                        self.strokes[strokeIdx][j][1] = origMovedStrokes[i][j][1] + worldDy;
                    }
                }
            };

            /**
             * Move token and redraw canvas
             * @param tokenIndices
             * @param origBoundsArray
             * @param worldDx
             * @param worldDy
             */
            this.moveTokenRedraw = function(tokenIndices, origBoundsArray, worldDx, worldDy) {
                // Input sanity checks
                if (!Array.isArray(tokenIndices) || !Array.isArray(origBoundsArray)) {
                    throw new Error("First two input arguments are not arrays");
                }

                if (tokenIndices.length !== origBoundsArray.length) {
                    throw new Error("Mismatch in length between tokenIndices and origBoundsArray");
                }

                for (var i = 0; i < tokenIndices.length; ++i) { // Iterate through all the tokens that need to be moved
                    self.moveToken(tokenIndices[i], origBoundsArray[i], worldDx, worldDy);
                }

                /* Canvas redraw */
                self.redraw();
            };

            /* Zoom- and pan-related methods */
            this.fovPan = function(panX, panY) {
                self.viewPort.fovPan(panX, panY);
            };

            /* Pan with amounts specified relative to the current fov size. Calls redraw() */
            this.relativeFovPanRedraw = function(relPanX, relPanY) {
                self.viewPort.relativeFovPan(relPanX, relPanY);

                self.redraw();
            };

            this.fovPanRedraw = function(panX, panY) {
                self.viewPort.fovPan(panX, panY);

                self.redraw();
            };

            /* FOV Zoom */
            this.fovZoom = function(zoomRatio) {
                self.viewPort.fovPan(zoomRatio);

                return self.viewPort.fov;
            };

            this.fovZoomRedraw = function(zoomRatio) {
                self.viewPort.fovZoom(zoomRatio);

                self.redraw();
            };

            /* Resest FOV to default */
            this.fovReset = function() {
                self.viewPort.fovRest();
            };

            this.path2Bounds = function(path) {
                var minX = Number.POSITIVE_INFINITY;
                var maxX = Number.NEGATIVE_INFINITY;
                var minY = Number.POSITIVE_INFINITY;
                var maxY = Number.NEGATIVE_INFINITY;

                for (var i = 0; i < path.length; ++i) {
                    if (path[i][0] < minX) {
                        minX = path[i][0];
                    }

                    if (path[i][0] > maxX) {
                        maxX = path[i][0];
                    }

                    if (path[i][1] < minY) {
                        minY = path[i][1];
                    }

                    if (path[i][1] > maxY) {
                        maxY = path[i][1];
                    }
                }

                return [minX, minY, maxX, maxY];
            };

            /* Process the writtenTokenSet output from the back-end stroke curator and
             * marshall the data into members.
             * Input argument: tokens         : an array
             *                 constStokes    : indices of constituent strokes (an array of arrays)
             *                 pushStack      : whether strokes and other local states should be pushed on to the local state stack
             *                 causedByAction : the action that causes this update (optional)
             *                 */
            this.procWrittenTokenSet = function(tokens, constStrokes, pushLocalStateStack, causedByAction) {
                var N = tokens.length;

                self.tokenNames = new Array(N);
                self.tokenBounds = new Array(N);

                for (var i = 0; i < N; ++i) {
                    self.tokenNames[i] = tokens[i].recogWinner;
                    self.tokenBounds[i] = tokens[i].bounds;
                }

                /* Process the stroke ownership */
                if (tokens.length !== constStrokes.length) {
                    console.error("The lengths of tokens and constStrokes do not match");
                    return;
                }

                self.strokeTokenOwners = new Array(self.strokes.length);
                for (var i = 0; i < constStrokes.length; ++i) {
                    for (var j = 0; j < constStrokes[i].length; ++j) {
                        self.strokeTokenOwners[constStrokes[i][j]] = i;
                    }
                }

                self.constStrokes = constStrokes;

                if (typeof pushLocalStateStack === "boolean" && pushLocalStateStack) {
                    self.localStateStackPush();
                }

                // Automatically select the last token
                var updateCursorSelectedTokenIndices = true;

                // Actions that do not require resetting of the selected token indices
                var actionsNoUpdateCursorSelection = ["forceSetTokenRecogWinner", "undoStrokeCuratorUserAction",
                    "redoStrokeCuratorUserAction", "moveMultipleTokens"];

                if (typeof causedByAction === "string" && actionsNoUpdateCursorSelection.indexOf(causedByAction) !== -1) {
                    updateCursorSelectedTokenIndices = false;
                }

                if (updateCursorSelectedTokenIndices) {
                    self.cursorSelectedTokenIndices = [self.tokenNames.length - 1];
                }
            };

            this.getNumTokens = function() {
                return self.hwEngAgent.getNumTokens();
            };

            this.getLastStrokeCuratorUserAction = function() {
                return self.hwEngAgent.lastStrokeCuratorUserAction;
            };

            this.canUndoStrokeCuratorUserAction = function() {
                return self.hwEngAgent.canUndoStrokeCuratorUserAction;
            };

            this.canRedoStrokeCuratorUserAction = function() {
                return self.hwEngAgent.canRedoStrokeCuratorUserAction;
            };

            this.getCanvasCoordinates = function(e) {
                var x;
                var y;

                if (typeof e.touches === "object") {
                    // This occurs on certain mobile browsers during touchstart event
                    var touch = e.touches[0];

                    if (typeof touch.offsetX === "undefined") {
                        x = touch.pageX - self.elLeft;
                        y = touch.pageY - self.elTop;
                    } else {
                        x = touch.offsetX;
                        y = touch.offsetY;
                    }
                } else if (typeof e.originalEvent === "object" && typeof e.originalEvent.touches === "object") {
                    // This occurs on certain mobile browsers during touchstart and touchmove events
                    var touch = e.originalEvent.touches[0];

                    if (self.debugLv > 0) {
                        var str = "";
                        for (var fld in touch) {
                            if ( !touch.hasOwnProperty(fld) ) {
                                continue;
                            }
                            str += "" + fld + ", ";
                        }

//                        str += ", " + typeof touch.offsetX + ": " + touch.offsetX + ", " +
//                            typeof touch.offsetY + ": " + touch.offsetY;

//                        $("#eventData").val(str);
                    }

                    if (typeof e.offsetX === "undefined") {
                        x = touch.pageX - self.elLeft;
                        y = touch.pageY - self.elTop;
                    } else {
                        x = touch.offsetX;
                        y = touch.offsetY;
                    }
                } else if (typeof e.offsetX === "undefined") { /* Firefox */
                    x = e.pageX - self.elLeft;
                    y = e.pageY - self.elTop;
                } else { /* Other browsers */
                    x = e.offsetX;
                    y = e.offsetY;
                }

//                return { "x" : x , "y" : y };
                return [x, y];
            };

            this.getCanvasSize = function() {
                var w = Number(this.el.attr("width"));
                var h = Number(this.el.attr("height"));

                return [w, h];
            };

            /* Handle mouse left button / touch down event */
            var handleLeftButtonDown = function(e) {
                self.touchDown = true;

                var canvasCoord = self.getCanvasCoordinates(e);
                $("#misc").html($("#misc").html() + "canvasCoord=" + canvasCoord + "<br/>");

                var worldCoord = self.viewPort.canvasPos2WorldPos(canvasCoord);

                var ctx = self.canvasEl.getContext("2d");

                if (self.cursorSelectMode) {        // Cursor select mode
                    self.cursorSelectOriginWorld = worldCoord;
                } else {                            // Writing mode
                    self.pts.push(worldCoord);

                    if (self.options.markStrokeOnset) {
//                    ctx.fillRect(self.pts[0][0], self.pts[0][1], 3, 3);
                        ctx.fillRect(canvasCoord[0], canvasCoord[1], 3, 3);
                    }

                    if (self.options.onTouchDown) {
                        self.options.onTouchDown.call(this, worldCoord);
                    }
                }
            };

            /* Handle mouse middle button down event */
            var handleMiddleButtonDown = function(e) {
                e.preventDefault();

                var canvasCoord = self.getCanvasCoordinates(e);
                var tokenIdx = self.getEnclosingTokenIndex(canvasCoord[0], canvasCoord[1]);

                if (tokenIdx >= 0) {
                    console.log("Middle-button deletion: " + tokenIdx); //DEBUG

                    /* Remove the front-end strokes that belong to the selected token */
                    self.removeStrokesOfToken(tokenIdx);

                    // INSERT
                    self.removeToken(tokenIdx);
                }
            };

            /* Handle mouse right button down event */
            var handleRightButtonDown = function(e) {
                var canvasCoord = self.getCanvasCoordinates(e);

                var enclosingTokenIdx = self.getEnclosingTokenIndex(canvasCoord[0], canvasCoord[1]);

                if (enclosingTokenIdx === -1) {  // Not within any tokens, start FOV panning
                    self.rightButtonStatus = "fovPan";

                    self.lastRightButtonPos[0] = canvasCoord[0];
                    self.lastRightButtonPos[1] = canvasCoord[1];
                } else {
                    self.rightButtonStatus = "tokenMove";

                    if (self.cursorSelectedTokenIndices.length > 1 &&
                        self.cursorSelectedTokenIndices.indexOf(enclosingTokenIdx) !== -1) {

                        self.rightButtonTokenMoveIdx = self.cursorSelectedTokenIndices;

                    } else {
                        self.rightButtonTokenMoveIdx = [enclosingTokenIdx];
                        self.cursorSelectedTokenIndices = [self.rightButtonTokenMoveIdx];
                    }

                    var nMovedTokens = self.rightButtonTokenMoveIdx.length;
                    self.origMovedTokenBoundsArray = new Array(self.rightButtonTokenMoveIdx.length);
                    for (var k = 0; k < nMovedTokens; ++k) {
                        var tokenIdx = self.rightButtonTokenMoveIdx[k];

                        self.origMovedTokenBoundsArray[k] = self.tokenBounds[tokenIdx].slice();
                    }



                    self.origMovedStrokes = {}; // "hashmap"
                    for (var j = 0; j < nMovedTokens; ++j) {
                        /* Store the original strokes */
                        var strokeIndices = self.getConstituentStrokeIndices(self.rightButtonTokenMoveIdx[j]);

                        var tokenIdxStr = "s" + String(self.rightButtonTokenMoveIdx[j]);

                        self.origMovedStrokes[tokenIdxStr] = [];

                        for (var i = 0; i < strokeIndices.length; ++i) {
                            var strokeIdx = strokeIndices[i];

                            self.origMovedStrokes[tokenIdxStr].push(_.map(self.strokes[strokeIdx], _.clone)); // Deep copy
                        }
                    }


                }

                // Store coordinates
                self.origRightButtonPos[0] = canvasCoord[0];
                self.origRightButtonPos[1] = canvasCoord[1];


            };

            /* Mouse down / touch down event */
            this.el.on("mousedown", function(event) {
                event.preventDefault();

                self.hasBeenMouseDown = true;

                self.currentMouseButton = event.button;
                if (event.button === self.mouseButton.left) {
                    handleLeftButtonDown(event);
                } else if (event.button === self.mouseButton.middle) {
                    handleMiddleButtonDown(event);
                } else if (event.button === self.mouseButton.right) {
                    handleRightButtonDown(event);
                }

                return false; // Disable context menu
            });

            this.el.on("touchstart", function(event) {
                self.hasBeenTouchStart = true;

                var e = event;
                if (typeof e.originalEvent === "object") {
                    e = e.originalEvent;
                }

                if ( !self.pinchMode ) {
                    if (e.touches.length === 2 && e.changedTouches.length === 1) {
                        /* The second touch just commenced */
                        self.pinchMode = true;
                        self.hasBeenPinch = true;
//                        $("#glyphoidTitle").html($("#glyphoidTitle").html() + "Pinch start. "); //DEBUG

                        if (typeof e.touches[0].offsetX === "undefined") {
                            self.pinchPrevPos = [
                                [e.touches[0].pageX - self.elLeft, e.touches[0].pageY - self.elTop],
                                [e.touches[1].pageX - self.elLeft, e.touches[1].pageY - self.elTop]
                            ];
                        } else {
                            self.pinchPrevPos = [
                                [e.touches[0].offsetX, e.touches[0].offsetY],
                                [e.touches[1].offsetX, e.touches[1].offsetY]
                            ];
                        }

                        // Eliminate the pts accumulated prior to the pinch onset
                        self.pts = [];
                    }
                }

//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + "TS: touches.length=" + e.touches.length + ", " +
//                                         "changedTouches.length=" + e.changedTouches.length + "; ");
//                for (var key in e.touches[0]) {
//                    if (e.touches[0].hasOwnProperty(key)) {
//                        $("#glyphoidTitle").html($("#glyphoidTitle").html() + "[" + key + "]");
//                    }
//                }
//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + ". ");

                if (e.touches.length === 1) {
                    self.hasBeenPinch = false;
                    handleLeftButtonDown(event);
                }
            }); /* For touch devices */

            /* Mouse wheel event */
            this.el.on("mousewheel", function(event) {
                event.preventDefault();

                var mouseWheelZoomStep = 0.0625;
                var wheelDelta = event.originalEvent.wheelDelta;

                if (wheelDelta > 0) {
                    self.fovZoomRedraw(-mouseWheelZoomStep);
                } else if (wheelDelta < 0) {
                    self.fovZoomRedraw(mouseWheelZoomStep);
                }
            });

            $(window).on("keydown", function(event) {

                if (!event.altKey && !event.ctrlKey) {
                    if (event.keyCode >= 48 && event.keyCode < 91 ||  // Alphanumeric
                        event.keyCode === 187 || // "=/+"
                        event.keyCode === 189 || // "-"
                        event.keyCode === 191 || // "/"
                        event.keyCode === 219 || // "["
                        event.keyCode === 221) { // "]"
                        // Force set token name
                        /* TODO: Arrow keys for panning and pageUp/pageDown keys for zooming */

                        var key;
                        if (event.keyCode === 187) {
                            key = "=";
                        } else if (event.keyCode === 189) {
                            key = "-";
                        } else if (event.keyCode === 191) {
                            key = "/";
                        } else if (event.keyCode === 219) {
                            key = "[";
                        } else if (event.keyCode === 221) {
                            key = "]";
                        } else {
                            key = String.fromCharCode(event.keyCode).toLowerCase();
                        }

                        if (event.shiftKey) {
                            if (key === "8") {
                                key = "*";
                            } else if (key === "9") {
                                key = "(";
                            } else if (key === "0") {
                                key = ")";
                            } else if (key === "=") {
                                key = "+";
                            } else {
                                key = key.toUpperCase();
                            }
                        }

                        self.forceSetTokenRecogWinnerByContext(key);
                    } else if (event.keyCode === 46) { // Delete key
                        if (event.shiftKey) { // Shift+Delete: Remove all tokens
                            self.clear();
                        } else { // Only Delete key: Remove selected token(s)
                            self.removeTokens(self.cursorSelectedTokenIndices);
                        }
                    } else if (event.keyCode === 13) { // Enter key: Parse. // TODO: Shift key: Incremental parsing using NodeTokens
                        if ( !$("#parseTokenSet").prop("disabled") ) {
                            $("#parseTokenSet").trigger("click"); //TODO: Get rid of reverse dependency on element ID.d
                        }
                    }

                } else if (event.ctrlKey && !event.altKey) {
                    if (event.keyCode === 90) { // Ctrl + z
                        // Undo
                        if (self.canUndoStrokeCuratorUserAction()) {
                            self.undoStrokeCuratorUserAction();
                        }
                    } else if (event.keyCode === 89) { // Ctrl + y
                        // Redo
                        if (self.canRedoStrokeCuratorUserAction()) {
                            self.redoStrokeCuratorUserAction();
                        }
                    }
                }


            });

            /* Handle mouse left button / touch move event */
            var handleLeftButtonMove = function(e) {
                if (!self.touchDown) {
                    return; // Unexpected state
                }

                var canvasCoord = self.getCanvasCoordinates(e);
                var worldCoord = self.viewPort.canvasPos2WorldPos(canvasCoord);

                var ctx = self.canvasEl.getContext("2d");

                if (self.cursorSelectMode) {        // Cursor select mode
                    self.cursorSelectCurrentWorld = worldCoord;
                    self.processCursorSelect(); // Calculate the select box coordinates and determine the selected tokens
                    self.redraw();

                } else {        // Writing mode
                    var oldCanvasPos = self.viewPort.worldPos2CanvasPos(self.pts[self.pts.length - 1]);

                    var oldX = oldCanvasPos[0];
                    var oldY = oldCanvasPos[1];


                    self.pts.push(worldCoord);

//                var newX = self.pts[self.pts.length - 1][0];
//                var newY = self.pts[self.pts.length - 1][1];
                    var newX = canvasCoord[0];
                    var newY = canvasCoord[1];

                    /* Edge guard: If the cursor gets too close to the edge, trigger a left-button up event */
                    var edgeGuardWidth = self.options.edgeGuardWidth;
                    if (self.options.enableEdgeGuard &&
                        (newX < edgeGuardWidth ||
                         newY < edgeGuardWidth ||
                         newX > self.canvasWidth - edgeGuardWidth ||
                         newY > self.canvasHeight - edgeGuardWidth)) {
                        console.log("Edge guard triggered");

                        self.hasBeenMouseDown = false;
                        self.hasBeenTouchStart = false;
                        handleLeftButtonUp(e);
                    }

                    ctx.strokeStyle = self.options.drawOptions.strokeStyle;
                    ctx.beginPath();
                    ctx.moveTo(oldX, oldY);
                    ctx.lineTo(newX, newY);
                    ctx.stroke();

                    if (self.options.markStrokeOnset) {
                        ctx.fillRect(canvasCoord[0], canvasCoord[1], 3, 3);
                    }

                    if (self.options.onTouchMove) {
                        self.options.onTouchMove.call(this, worldCoord);
                    }
                }
            };

            this.processCursorSelect = function() {
                if (self.cursorSelectBoxWorld.length === 0) {
                    self.cursorSelectBoxWorld = new Array(4);
                }

                // xMin
                self.cursorSelectBoxWorld[0] = self.cursorSelectOriginWorld[0] < self.cursorSelectCurrentWorld[0] ?
                                               self.cursorSelectOriginWorld[0] : self.cursorSelectCurrentWorld[0];
                // yMin
                self.cursorSelectBoxWorld[1] = self.cursorSelectOriginWorld[1] < self.cursorSelectCurrentWorld[1] ?
                                               self.cursorSelectOriginWorld[1] : self.cursorSelectCurrentWorld[1];
                // xMax
                self.cursorSelectBoxWorld[2] = self.cursorSelectOriginWorld[0] < self.cursorSelectCurrentWorld[0] ?
                                               self.cursorSelectCurrentWorld[0] : self.cursorSelectOriginWorld[0];
                // yMin
                self.cursorSelectBoxWorld[3] = self.cursorSelectOriginWorld[1] < self.cursorSelectCurrentWorld[1] ?
                                               self.cursorSelectCurrentWorld[1] : self.cursorSelectOriginWorld[1];

                self.cursorSelectedTokenIndices = [];
                for (var i = 0; i < self.tokenBounds.length; ++i) {
                    var bounds = self.tokenBounds[i];
                    var ctrX = (bounds[0] + bounds[2]) / 2;
                    var ctrY = (bounds[1] + bounds[3]) / 2;

                    if (ctrX > self.cursorSelectBoxWorld[0] && ctrX < self.cursorSelectBoxWorld[2] &&
                        ctrY > self.cursorSelectBoxWorld[1] && ctrY < self.cursorSelectBoxWorld[3]) {
                        self.cursorSelectedTokenIndices.push(i);
                    }
                }
            };

            /* Handle mouse right button move event */
            var handleRightButtonMove = function(e) {
                if (self.rightButtonStatus === "fovPan") {
                    var canvasCoord = self.getCanvasCoordinates(e);

                    var panX = canvasCoord[0] - self.lastRightButtonPos[0];
                    var panY = canvasCoord[1] - self.lastRightButtonPos[1];

//                    self.fovPan(panX, panY);
                    self.relativeFovPanRedraw(- panX / self.canvasWidth, - panY / self.canvasHeight);
                    console.log("Right-button panning"); //DEBUG

                    self.lastRightButtonPos[0] = canvasCoord[0];
                    self.lastRightButtonPos[1] = canvasCoord[1];
                } else if (self.rightButtonStatus === "tokenMove") {
                    var canvasCoord = self.getCanvasCoordinates(e);

                    // dx and dy in canvas coordinates
                    var canvasDx = canvasCoord[0] - self.origRightButtonPos[0];
                    var canvasDy = canvasCoord[1] - self.origRightButtonPos[1];

                    // Translated to world coordinates
                    var worldDxy = self.viewPort.canvasDisplacementVector2WorldDisplacementVector([canvasDx, canvasDy]);

                    // Move the token and redraw
                    self.moveTokenRedraw(self.rightButtonTokenMoveIdx, self.origMovedTokenBoundsArray, worldDxy[0], worldDxy[1]);
                }
            };

            this.el.on("mousemove", function(event) {
                event.preventDefault();

                if (self.currentMouseButton === self.mouseButton.left) {
                    handleLeftButtonMove(event);
                } else if (self.currentMouseButton === self.mouseButton.right) {
                    handleRightButtonMove(event);
                }

                return false;
            });
            this.el.on("touchmove", function(event) {
                var e = event;
                if (typeof e.originalEvent === "object") {
                    e = e.originalEvent;
                }

//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + "TM: touches.length=" + e.touches.length + ", " +
//                    "changedTouches.length=" + e.changedTouches.length + "; ");
//                for (var key in e.touches[0]) {
//                    if (e.touches[0].hasOwnProperty(key)) {
//                        $("#glyphoidTitle").html($("#glyphoidTitle").html() + "[" + key + "]");
//                    }
//                }
//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + ". ");

                if (self.pinchMode) {
                    var pinchCurrPos;
                    if (typeof e.touches[0].offsetX === "undefined") {
                        pinchCurrPos = [
                            [e.touches[0].pageX - self.el.offset().left, e.touches[0].pageY - self.el.offset().top],
                            [e.touches[1].pageX - self.el.offset().left, e.touches[1].pageY - self.el.offset().top]
                        ];
                    } else {
                        pinchCurrPos = [
                            [e.touches[0].offsetX, e.touches[0].offsetY],
                            [e.touches[1].offsetX, e.touches[1].offsetY]
                        ];
                    }

                    var pinchInfo = self.pinchEvent.getMovementInfo(self.pinchPrevPos, pinchCurrPos);
                    //$("#glyphoidTitle").html($("#glyphoidTitle").html() + "Pinch=" + JSON.stringify(pinchInfo) + ". "); //DEBUG
                    if (pinchInfo.type === "PinchPan") {
                        self.fovPanRedraw(-pinchInfo.panX, -pinchInfo.panY);
                    } else if (pinchInfo.type === "PinchZoom") {
                        self.fovZoomRedraw(-pinchInfo.zoomRatio);
                    }

                    self.pinchPrevPos = pinchCurrPos;
                } else {
                    handleLeftButtonMove(event);
                }
            }); /* For touch devices */

            /* Recognize a set of strokes as a token, through an AJAX call to the HTTP endpoint */
            this.recognizeToken = function(strokeIndices, successCallback) {
                var tokenJson = self.getWrittenTokenJSON(strokeIndices);

                var t0 = new Date();
                $.ajax({
                    url      : self.options.endpoints.tokenRecognizer,
                    method   : "POST",
                    data     : tokenJson,
                    complete : function(resp) {
                        if (self.debugLv > 0) {
                            console.log("Token recognizer response =", resp);
                        }

                        var t1 = new Date();
                        var elapsedMillis = t1.getTime() - t0.getTime();
                        if (resp.status === 200) {
                            if (typeof resp.responseJSON.winnerTokenName === "string") {
                                var cands = resp.responseJSON.recogPVals;
                                successCallback(resp.responseJSON.winnerTokenName, cands, elapsedMillis);
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
            };

            var forceSetTokenNameFunc = function(tokenName) {
                console.log("Force setting token to: " + tokenName);

                var nTokens = self.getNumTokens();

                if (typeof nTokens === "number" && nTokens > 0) {
                    self.forceSetTokenRecogWinnerByContext(tokenName);
                }

            };


            /**
             * Get the display name (UTF-8) given a token name (ASCII)
             * @param    tokenName: tokenName in ASCII
             * @returns  token display name (UTF-8)
             */
            this.getTokenDisplayName = function(tokenName) {
                /* Input sanity check */
                if (typeof tokenName !== "string") {
                    throw "Invalid input type";
                }

                var tokenDisplayName;
                if ( TokenDisplayNames.hasOwnProperty(tokenName) &&
                    typeof TokenDisplayNames[tokenName] === "string" ) {
                    tokenDisplayName = TokenDisplayNames[tokenName];
                } else {
                    tokenDisplayName = tokenName;
                }

                return tokenDisplayName;
            };

            /* Input arguments: cp: candidates and their p-values as an array */
            this.createTokenCandidateButtons = function(cp) {
                $("#tokenRecogCands").empty();

                for (var i = 0; i < cp.length; ++i) {
                    var btnId = "tokenCand_" + i;

                    var tokenName = cp[i][0];
                    var tokenDisplayName = self.getTokenDisplayName(tokenName);

                    var btn = "<button class=\"btn btn-default TokenCandBtn\" tokenName=\"" + tokenName
                              + "\" id=\"" + btnId + "\">";
//                    btn += cp[i][0] + "<br />(" + sprintf("%.3e", cp[i][1]) + ")";
                    btn += tokenDisplayName;
                    btn += "</button>";

                    /* Append button element */
                    $("#tokenRecogCands").append(btn);

                    /* Bind click callback */
                    $("#" + btnId).on("click", function(e) {
                        e.preventDefault();

//                        forceSetTokenNameFuncBound();
                        var target = $(e.target);
                        var tokenName = target.attr("tokenName");
                        console.log("tokenName = " + tokenName);

                        console.log("Setting token name to " + tokenName + "(" + tokenDisplayName + ")"); //DEBUG

                        forceSetTokenNameFunc(tokenName);
                    });
                }
            };

            /* Handle mouse left button / touch up event */
            var handleLeftButtonUp = function(e) {
                self.touchDown = false;

//                var lastStroke = self.getStrokeJsonObj(lastStrokeIdx);
                if (self.cursorSelectMode) {      // Cursor selection mode
                    self.toggleCursorSelectMode();
                    $.unblockUI();
                } else {                          // Writing mode
                    var newStroke = self.getStrokeJsonObject(self.pts);

                    self.hwEngAgent.addStroke(
                        newStroke,
                        function (responseJSON, elapsedMillis) { /* Success function */
//                        console.log("Add-stroke success: responseJSON =", responseJSON); //DEBUG

                            /* In this call, we set pushLocalStateStack to false, because the state-stack pushing action
                             * will be performed after the next two lines of code.
                             */
                            self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens,
                                responseJSON.constituentStrokes, false);

                            /* Update the stroke states. This is done after the ajax call to the engine, so that in case
                             * a state injection is required, the serialized state to inject will not going to include the
                             * new stroke. If the new stroke were included in the injected stated, the engine will end up
                             * having a redundant stroke.
                             */
                            self.strokes.push(self.pts);
                            self.strokeBounds.push(self.path2Bounds(self.pts));

                            self.localStateStackPush();

                            self.pts = [];
                            /* Empty the points */

                            /* Redraw the canvas */
                            self.redraw();

                            /* Now we can perform token recognition, now we have the constituent stroke indices of the last token */
                            var strokeIndices = responseJSON.constituentStrokes[responseJSON.constituentStrokes.length - 1];
                            self.recognizeToken(strokeIndices, function (recogWinner, cands, elapsedMillis) {
                                $("#tokenRecogWinner").val(recogWinner + " (" + elapsedMillis + " ms)");

                                /* Sort the candidates */
                                cands.sort(function (x, y) {
                                    return y[1] - x[1];
                                });

                                var candsCount = 8; // TODO: Do not hard-code
                                var len = (cands.length > candsCount) ? candsCount : cands.length;

                                cands = cands.slice(0, len);

                                self.createTokenCandidateButtons(cands);

                                //                    var ctx = self.canvasEl.getContext("2d");
                                //                    var bnds = self.strokeBounds[self.strokeBounds.length - 1];
                            });


                        },
                        function () { /* Failure function */
                            console.log("Add-stroke failure");
                        }
                    );

                    if (self.options.onTouchUp) {
                        self.options.onTouchUp.call(self, self.viewPort.canvasPos2WorldPos(self.getCanvasCoordinates(e)));
                    }
                }
            };

            /* Handle mouse right button up event */
            var handleRightButtonUp = function(e) {
                e.preventDefault();

                if (self.rightButtonStatus === "tokenMove") {
                    /* Send move-token request to backend */

                    var nMovedTokens = self.rightButtonTokenMoveIdx.length;
                    var newBoundsArray = new Array(nMovedTokens);
                    for (var i = 0; i < nMovedTokens; ++i) {
                        newBoundsArray[i] = self.tokenBounds[self.rightButtonTokenMoveIdx[i]];
                    }

                    self.hwEngAgent.moveMultipleTokens(self.rightButtonTokenMoveIdx, newBoundsArray,
                        function(responseJSON, elapsedMillis) {    /* Clear action success */
                            self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens, responseJSON.constituentStrokes,
                                true, "moveMultipleTokens");

                            self.redraw();
                        },
                        function() {    /* Clear action failure */
                            console.log("move-multiple-tokens failed.");
                        });
                } else {
                    $.unblockUI();
                }

                self.rightButtonStatus = null;

            };

            this.el.on("mouseup", function(event) {
                if ( !self.hasBeenMouseDown ) {
                    return;
                }

                self.hasBeenMouseDown = false;

                $.blockUI(); // Prevent too-fast actions from user

                event.preventDefault();

                if (self.currentMouseButton === self.mouseButton.left) {
                    handleLeftButtonUp(event);
                } else if (self.currentMouseButton === self.mouseButton.right) {
                    handleRightButtonUp(event);
                } else {
                    $.unblockUI(); // Middle button
                }

                return false;
            });

            this.el.on("touchend", function(event) {
                if ( !self.hasBeenTouchStart ) {
                    return;
                }

                var e = event;
                if (typeof e.originalEvent === "object") {
                    e = e.originalEvent;
                }

                if ( self.pinchMode ) {
                    if (e.touches.length === 1 && e.changedTouches.length === 1) {
                        /* The second touch just commenced */
                        self.pinchMode = false;
                        self.pinchPrevPos = null;
//                        $("#glyphoidTitle").html($("#glyphoidTitle").html() + "Pinch end. "); //DEBUG
                    }
                }

//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + "TE: touches.length=" + e.touches.length + ", " +
//                    "changedTouches.length=" + e.changedTouches.length + "; ");
//                for (var key in e.touches[0]) {
//                    if (e.touches[0].hasOwnProperty(key)) {
//                        $("#glyphoidTitle").html($("#glyphoidTitle").html() + "[" + key + "]");
//                    }
//                }
//                $("#glyphoidTitle").html($("#glyphoidTitle").html() + ". ");

                if ( !self.hasBeenPinch ) {
                    self.hasBeenTouchStart = false;

                    $.blockUI();
                    handleLeftButtonUp(event);
                }
            }); /* For touch devices */

            this.el.on("contextmenu", function(e) {
                return false;
            });

            this.getPaths = function() {
                return self.strokes;
            };

            this.clearCanvas = function() {
                var canvasWH = self.getCanvasSize();
                //        var ctx = document.getElementById(self.options.elementId).getContext("2d");
                var ctx = self.canvasEl.getContext("2d");
                ctx.clearRect(0, 0, canvasWH[0], canvasWH[1]);
            };

            this.clear = function() {
                /* Clear back-end stroke-curator */
                self.hwEngAgent.clear(
                    function(responseJSON, elapsedMillis) {    /* Clear action success */
//                        console.log("Clear action succeeded. responseJSON =", responseJSON);

                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens, responseJSON.constituentStrokes, true);

                        self.redraw();
                    },
                    function() {    /* Clear action failure */
                        console.log("Clear action failed.");
                    }
                );

                self.strokes = [];
                self.strokeBounds = [];
                self.strokeTokenOwners = [];

                self.tokenNames = [];
                self.tokenBounds = [];

                self.clearCanvas();

                if (self.debugLv > 0) {
                    $("#strokesDisplay").val(JSON.stringify(self.strokes));
                    $("#writtenTokenJsonDisplay").val(getWrittenTokenJSON());
                }

            };

            /* Merge a subset of the strokes, specified with the indices, as a token */
            this.mergeStrokesAsToken = function(strokeIndices, successCallback, failureCallback) {
                self.hwEngAgent.mergeStrokesAsToken(
                    strokeIndices,
                    function(responseJSON, elapsedMillis) {    /* Merge action success */
//                        console.log("Merge action succeeded. responseJSON =", responseJSON);

                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens,
                            responseJSON.constituentStrokes, true);

                        self.redraw();

                        if (typeof successCallback === "function") {
                            successCallback(responseJSON, elapsedMillis);
                        }
                    },
                    function() {    /* Clear action failure */
                        console.log("Merge action failed.");

                        if (typeof failureCallback === "function") {
                            failureCallback();
                        }
                    }
                );
            };

            /* Unmerge the last stroke */
            this.unmergeLastStroke = function() {
                if (self.strokes.length > 1) {
                    var lastStrokeIndex = [self.strokes.length - 1];

                    self.mergeStrokesAsToken(lastStrokeIndex);
                }
            };

            /* Unmege the last token */
            this.unmergeLastToken = function() {
                if (self.hwEngAgent.isLastTokenMerged()) {
                    var strokeIndices = self.hwEngAgent.getLastTokenConstituentStrokeIndices();

                    if (strokeIndices.length === 2) {
                        self.mergeStrokesAsToken(strokeIndices[1], function(responseJSON, elapsedMillis) {
                            self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens, responseJSON.constituentStrokes, true);
                            self.redraw();
                        });
                    } else if (strokeIndices.length === 3) {
                        self.mergeStrokesAsToken(strokeIndices[1], function(responseJSON1, elapsedMillis1) {
                            self.mergeStrokesAsToken(strokeIndices[2], function(responseJSON2, elapsedMillis2) {
                                self.procWrittenTokenSet(responseJSON2.writtenTokenSet.tokens, responseJSON2.constituentStrokes, true);
                                self.redraw();
                            });
                        });
                    } else {
                        throw "Not implemented: unmerging 4 strokes";
                    }
                }
            };

            /* Force set token name (recog winner) */
            this.forceSetTokenRecogWinner = function(tokenIdx, tokenName) {
                self.hwEngAgent.forceSetTokenRecogWinner(
                    tokenIdx, tokenName,
                    function(responseJSON, elapsedMillis) { /* Success callback */

                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens,
                            responseJSON.constituentStrokes, true, "forceSetTokenRecogWinner");
                        self.redraw();
                    },
                    function() {    /* Failure callback */
                        console.log("Force-set-token-name action failed.");
                    }
                );
            };


            /**
             * Set token name by context (whether there is non-default selection of tokens).
             * Wrapper around forceSetTokenRecogWinner
             */
            this.forceSetTokenRecogWinnerByContext = function(tokenName) {
                if (Array.isArray(self.cursorSelectedTokenIndices) && self.cursorSelectedTokenIndices.length > 0) {
                    var setTokenIdx = self.cursorSelectedTokenIndices[0];
                    self.forceSetTokenRecogWinner(setTokenIdx, tokenName);
                } else {
                    if (self.getNumTokens() > 0) {
                        self.forceSetTokenRecogWinner(self.getNumTokens() - 1, tokenName);
                    }
                }
            };

            this.getStrokeIndices = function(scenario, num) {
                if (scenario === "last") {
                    if (num > self.strokes.length) {
                        return null;
                    }

                    var indices = new Array(num);
                    for (var i = 0; i < num; ++i) {
                        indices[i] = self.strokes.length - num + i;
                    }

                    return indices;
                }
                else {
                    console.error("Unrecognized scenario for genStrokeIndices(): \"" + scenario + "\"");
                    return null;
                }
            };

            this.getNumStrokes = function() {
                return self.strokes.length;
            };

            this.removePath = function(n) {
                if (n >= self.strokes.length || n < 0) {
                    console.error("n exceeds the length of strokes");
                    return;
                }

                self.strokes.splice(n, 1);
                self.strokeBounds.splice(n, 1);
            };

            this.removeLastPath = function() {
                if (self.strokes.length === 0) {
                    return;
                }

                self.removePath(self.strokes.length - 1);

                self.redraw();

                if (self.debugLv > 0) {
                    $("#strokesDisplay").val(JSON.stringify(self.strokes));
                }
            };

            /* Remove the front-end strokes that belong a specific token */
            this.removeStrokesOfToken = function(tokenIdx) {
                var preserveIdx = [];
                for (var i = 0; i < self.strokeTokenOwners.length; ++i) {
                    if (self.strokeTokenOwners[i] !== tokenIdx) {
                        preserveIdx.push(i);
                    }
                }

                var N = preserveIdx.length;
                var newStrokes = new Array(N);
                var newStrokeBounds = new Array(N);
                for (var j = 0; j < N; ++j) {
                    newStrokes[j] = self.strokes[preserveIdx[j]];
                    newStrokeBounds[j] = self.strokeBounds[preserveIdx[j]];
                }

                self.strokes = newStrokes;
                self.newStrokeBounds = newStrokeBounds;
            };

            /* Remove the last token in the token set.
             *   This involves a call to the back-end engine */
            this.removeLastToken = function() {
                if (self.tokenNames.length === 0) {
                    return;
                }

                self.cursorSelectedTokenIndices = [self.tokenNames.length - 1];    // Select the last token
                self.removeSelectedTokens();
            };

           this.removeSelectedTokens = function() {
                if (self.tokenNames.length === 0) {
                    return;
                }

               if (self.cursorSelectedTokenIndices.length > 0) {

                   self.removeTokens(self.cursorSelectedTokenIndices);

                   self.cursorSelectedTokenIndices = [];
               }
            };

            /**
             * Remove specified token
             */
            this.removeToken = function(tokenIdx) {
                self.hwEngAgent.removeToken(tokenIdx,
                    function(responseJSON, elapsedMillis) { /* Success in removing last token */
//                            console.log("Removal of token (idxToken = " + tokenIdx +
//                                        ") was successful: responseJSON =", responseJSON);

                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens,
                            responseJSON.constituentStrokes, true);

                        self.redraw();
                    },
                    function() {             /* Failure in removing last token */
                        console.log("Removal of token (idxToken = " + tokenIdx +
                            ") failed.");
                    }
                );
            };

            /** Remove specified multiple tokens */
            this.removeTokens = function(tokenIndices) {
                // Assume: tokenIndices is sorted in ascending order
                if (!Array.isArray(tokenIndices)) {
                    throw new Error("Input to removeTokens() is not an array");
                }

                if (tokenIndices.length === 0) {
                    return;
                }
                var removeIdx = tokenIndices.length - 1;

                var removeOneToken = function() {
                    self.hwEngAgent.removeToken(tokenIndices[removeIdx],
                        function(responseJSON, elapsedMillis) { /* Success in removing last token */
                            self.removeStrokesOfToken(tokenIndices[removeIdx]);
                            removeIdx--;

                            self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens,
                                                     responseJSON.constituentStrokes, true);

                            if (removeIdx >= 0) {
                                removeOneToken();
                            } else {
                                self.redraw();
                            }
                        },
                        function() {             /* Failure in removing last token */
                            throw new Error("removeTokens failed");
                        }
                    );
                };

                removeOneToken();
            };


            /**
             * Undo stroke curator last user action
             */
            this.undoStrokeCuratorUserAction = function() {
                self.hwEngAgent.undoStrokeCuratorUserAction(
                    function(responseJSON, elapsedMillis) {
                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens, responseJSON.constituentStrokes,
                                                 false, "undoStrokeCuratorUserAction");
                        self.cursorSelectedTokenIndices = [self.tokenNames.length - 1];

                        self.localStateStackUndo();

                        self.redraw();
                    },
                    function() {
                        console.error("Attempt to undo stroke curator user action failed");
                    }
                );
            };

            /**
             * Redo stroke curator last user action
             */
            this.redoStrokeCuratorUserAction = function() {
                self.hwEngAgent.redoStrokeCuratorUserAction(
                    function(responseJSON, elapsedMillis) {
                        self.procWrittenTokenSet(responseJSON.writtenTokenSet.tokens, responseJSON.constituentStrokes,
                                                 false, "redoStrokeCuratorUserAction");
                        self.cursorSelectedTokenIndices = [self.tokenNames.length - 1];

                        self.localStateStackRedo();
                        self.redraw();
                    },
                    function() {
                        console.error("Attempt to undo stroke curator user action failed");
                    }
                );
            };

            /* Parse token set */
            this.parseTokenSet = function(successFunc, errorFunc) {
                /* Call to the back-end to parse the token set */
                self.hwEngAgent.parseTokenSet(
                    function(responseJSON, elapsedMillis) { /* Success in removing last token */
//                        console.log("Parsing of token set succeeded: responseJSON =", responseJSON);

                        self.redraw();

                        if (responseJSON.errors.length === 0) {
                            if (typeof successFunc === "function") {
                                successFunc(responseJSON.parseResult, elapsedMillis);

                            }
                        } else {
                            var allErrorMsgs = "[";
                            for (var j = 0; j < responseJSON.errors.length; ++j) {
                                allErrorMsgs += responseJSON.errors[j];
                                if (j < responseJSON.errors.length - 1) {
                                    allErrorMsgs += " | ";
                                }
                            }
                            allErrorMsgs += "]";

                            errorFunc(allErrorMsgs);
                        }
                    },
                    function() {             /* Failure in removing last token */
                        console.log("Parsing of token set failed.");
                        errorFunc("[Parsing operation failed]");
                    }
                )
            };

            /* Get graphical productions */
            this.getGraphicalProductions = function(successFunc, errorFunc) {
                /* Call to the back-end to parse the token set */
                self.hwEngAgent.getGraphicalProductions(
                    function(responseJSON, elapsedMillis) { /* Success in removing last token */

                        if (responseJSON.errors.length === 0) {
                            if (typeof successFunc === "function") {
                                successFunc(responseJSON["graphicalProductions"], elapsedMillis);

                            }
                        } else {
                            // TODO: Refactor out
                            var allErrorMsgs = "[";
                            for (var j = 0; j < responseJSON.errors.length; ++j) {
                                allErrorMsgs += responseJSON.errors[j];
                                if (j < responseJSON.errors.length - 1) {
                                    allErrorMsgs += " | ";
                                }
                            }
                            allErrorMsgs += "]";

                            errorFunc(allErrorMsgs);
                        }
                    },
                    function() {             /* Failure in removing last token */
                        console.log("Getting graphical productions failed.");
                        errorFunc("[Getting graphical productions failed]");
                    }
                );
            };

            /* Redraw on the display, including the canvas and the UI controls */
            this.redraw = function() {
                var ctx = self.canvasEl.getContext("2d");

                self.clearCanvas();

                /* Draw the strokes */
                for (var i0 = 0; i0 < self.strokes.length; ++i0) {
                    /* Draw stroke */
                    var path = self.strokes[i0];
                    if (path.length === 0) {
                        continue;
                    }

                    ctx.strokeStyle = self.options.drawOptions.strokeStyle;
                    if (self.options.markStrokeOnset) {
                        var canvasPos = self.viewPort.worldPos2CanvasPos(path[0]);
                        ctx.fillRect(canvasPos[0], canvasPos[1], 3, 3);
                    }
                    for (var j = 0; j < path.length - 1; ++j) {
                        var canvasPos0 = self.viewPort.worldPos2CanvasPos(path[j]);
                        var canvasPos1 = self.viewPort.worldPos2CanvasPos(path[j + 1]);

                        ctx.beginPath();
                        ctx.moveTo(canvasPos0[0], canvasPos0[1]);
                        ctx.lineTo(canvasPos1[0], canvasPos1[1]);
                        ctx.stroke();
                    }

                    /* Draw bounding boxes for the strokes (optional) */
                    if (self.options.drawOptions.drawStrokeBoxLabels) {
                        var bnds = self.strokeBounds[i0];
                        var canvasBnds = self.viewPort.worldBnds2CanvasBnds(bnds);

                        ctx.beginPath();
                        ctx.strokeStyle = self.options.drawOptions.strokeBoxStyle;
                        ctx.rect(canvasBnds[0], canvasBnds[1], canvasBnds[2] - canvasBnds[0], canvasBnds[3] - canvasBnds[1]);
                        ctx.stroke();
                    }
                }

                /* Draw tokens: names and bounds */
                for (var i = 0; i < self.tokenNames.length; ++i) {
                    /* Draw bounding boxes for the tokens */
                    var bnds = self.tokenBounds[i];
                    var canvasBnds = self.viewPort.worldBnds2CanvasBnds(bnds);

                    var selected = self.cursorSelectedTokenIndices.indexOf(i) !== -1;
                    if (selected) {
                        ctx.strokeStyle = self.options.drawOptions.selectedTokenBoxStyle;
                    } else {
                        ctx.strokeStyle = self.options.drawOptions.tokenBoxStyle;
                    }
                    ctx.beginPath();
                    ctx.rect(canvasBnds[0], canvasBnds[1], canvasBnds[2] - canvasBnds[0], canvasBnds[3] - canvasBnds[1]);
                    ctx.stroke();


                    /* Draw token name */
                    var fontPoints = self.options.drawOptions.defaultFontPoints;
                    if (self.options.drawOptions.fontScaleFactor) {
                        fontPoints = ((canvasBnds[2] - canvasBnds[0]) + (canvasBnds[3] - canvasBnds[1])) * 0.5 *
                            self.options.drawOptions.fontScaleFactor;
                    }

                    fontPoints = fontPoints - fontPoints % 1.0;
                    ctx.font = fontPoints.toString() + "px Verdana";
                    if (selected) {
                        ctx.fillStyle = self.options.drawOptions.selectedFontStyle;
                    } else {
                        ctx.fillStyle = self.options.drawOptions.fontStyle;
                    }

                    var tokenName = self.tokenNames[i];
                    var tokenDisplayName = self.getTokenDisplayName(tokenName);

                    if (self.options.drawOptions.fontScaleFactor) {
                        ctx.fillText(tokenDisplayName, canvasBnds[0], canvasBnds[3]);
                    } else {
                        ctx.fillText(tokenDisplayName, canvasBnds[0], canvasBnds[1]);
                    }
                }

                /* Draw the selection box, if any */
                if (self.cursorSelectMode && self.cursorSelectBoxWorld.length === 4) {
                    // Get the canvas coordinates of the box to draw;
                    var cursorSelectBoxCanvas = self.viewPort.worldBnds2CanvasBnds(self.cursorSelectBoxWorld);

                    ctx.strokeStyle = self.options.drawOptions.cursorSelectBoxStyle;
                    ctx.beginPath();
                    ctx.rect(cursorSelectBoxCanvas[0], cursorSelectBoxCanvas[1],
                             cursorSelectBoxCanvas[2] - cursorSelectBoxCanvas[0], cursorSelectBoxCanvas[3] - cursorSelectBoxCanvas[1]);
                    ctx.stroke();
                }

                /* Update UI control status */
                self.updateUIControlState();

            };

            /**
             * Determine if a position is within the bounds of any tokens and if so, the index of that token
             * @param       canvasX, x coordinate, in canvas coordinates
             * @param       canvasY, y coordinate, in canvas coordintes
             * @return      index to the enclosing token, -1 if there is no enclosing token
             */
            this.getEnclosingTokenIndex = function(canvasX, canvasY) {
                /* Input sanity check */
                if (typeof canvasX !== "number" || typeof canvasY !== "number") {
                    throw "Invalid input";
                }

                var worldXY = self.viewPort.canvasPos2WorldPos([canvasX, canvasY]);
                var worldX = worldXY[0];
                var worldY = worldXY[1];

                var idx = -1;
                for (var i = 0; i < self.tokenBounds.length; ++i) {
                    var worldBnds = self.tokenBounds[i]; // Bounds in world coordinates
                    if (worldBnds[0] <= worldX && worldBnds[2] >= worldX &&
                        worldBnds[1] <= worldY && worldBnds[3] >= worldY) {
                        idx = i;
                        break;
                    }
                }

                return idx;
            };

            this.getStrokeJsonObject = function(pts) {
                var len = pts.length;
                var strokeJson = {
                    numPoints : len,
                    x         : new Array(len),
                    y         : new Array(len)
                };

                for (var j = 0; j < len; ++j) {
                    strokeJson.x[j] = pts[j][0];
                    strokeJson.y[j] = pts[j][1];
                }

                return strokeJson;
            };

            this.getSelectedTokenDevFileString = function() {
                if (self.cursorSelectedTokenIndices.length !== 1) {
                    throw new Error("Cannot handle zero or more than one selected tokens at a time");
                }

                var devTokenHelper = new DevTokenHelper();
                var selIdx = self.cursorSelectedTokenIndices[0];

                var strokes = [];
                for (var i = 0; i < self.strokeTokenOwners.length; ++i) {
                    if (self.strokeTokenOwners[i] === selIdx) {
                        strokes.push(self.strokes[i]);
                    }
                }

                return devTokenHelper.getDevFileString(strokes, self.tokenNames[selIdx]);
            };

//            /* Obtain the JSON for a stroke.
//             *   Input argument: idx: the index to the stroke in the array of strokes */
//            this.getStrokeJsonObj = function(idx) {
//                if (idx >= self.strokes.length) {
//                    return null;
//                }
//
//                var len = self.strokes[idx].length;
//                var strokeJson = {
//                    numPoints : len,
//                    x         : new Array(len),
//                    y         : new Array(len)
//                };
//
//                for (var j = 0; j < self.strokes[idx].length; ++j) {
//                    strokeJson.x[j] = self.strokes[idx][j][0];
//                    strokeJson.y[j] = self.strokes[idx][j][1];
//                }
//
//                return strokeJson;
//            };

            /**
             * Get serialized state, for injecting state to handwriting engine (e.g., after engine removal due to expiration)
             * @param   How many most recent strokes to omit from the serialized state. default: 0
             * @return  Serialized state as a JSON object
             */
            this.getSerializedState = function(omitLastStrokesCount) {
                if (typeof omitLastStrokesCount === "undefined") {
                    omitLastStrokesCount = 0;
                }

                var state = {};

                /* Strokes */
                var strokes = [];
                for (var i = 0; i < self.strokes.length - omitLastStrokesCount; ++i) {
                    var np = self.strokes[i].length;

                    var stroke = {
                        numPoints: np,
                        x: new Array(np),
                        y: new Array(np)
                    };

                    for (var j = 0; j < np; ++j) {
                        stroke.x[j] = self.strokes[i][j][0];
                        stroke.y[j] = self.strokes[i][j][1];
                    }

                    strokes.push(stroke);
                }

                state["strokes"] = strokes;

                /* Constituent stroke indices */
                /* Make deep copy */
                state["wtConstStrokeIndices"] = _.map(self.constStrokes, _.clone);

                /* Token bounds */
                state["tokenBounds"] = _.map(self.tokenBounds, _.clone);

                /* Token recognition winners (may have been force-set) */
                state["wtRecogWinners"] = _.map(self.tokenNames, _.clone);

                return state;
            };


//            /**
//             * Initialize local state stack
//             *  */
//            this.localStateStackInit = function() {
//                var strokeState = {
//                    strokes: [],
//                    strokeBounds: []
//                };
//
//                var strokeStateStr = JSON.stringify(strokeState);
//
//                self.stateStack.push(strokeStateStr);
//            };

            /**
             * Push strokes to stack
             *  */
            this.localStateStackPush = function() {
                var strokesState = {
                    strokes           : self.strokes,
                    strokeBounds      : self.strokeBounds,
                    strokeTokenOwners : self.strokeTokenOwners
                };

                var strokeStateStr = JSON.stringify(strokesState);

                self.stateStack.push(strokeStateStr);
            };

            /**
             * Local-stack undo
             */
            this.localStateStackUndo = function() {
                self.stateStack.undo();

                self.localStateStackExtractState();
            };

            /**
             * Local-stack redo
             */
            this.localStateStackRedo = function() {
                self.stateStack.redo();

                self.localStateStackExtractState();
            };

            this.localStateStackExtractState = function() {
                var lastState = self.stateStack.getLastState();

                var strokeState = {
                    strokes           : [],
                    strokeBounds      : [],
                    strokeTokenOwners : []
                };

                if (typeof lastState === "string" && lastState.length > 0) {
                     strokeState = JSON.parse(lastState);
                }

                self.strokes = strokeState.strokes;
                self.strokeBounds = strokeState.strokeBounds;
                self.strokeTokenOwners = strokeState.strokeTokenOwners;
            };

            /* Obtain the JSON for a token */
            this.getWrittenTokenJSON = function(indices) {
                var tIndices = null;
                if (indices) {
                    tIndices = indices;
                }

                var json = {
                    numStrokes : 0,
                    strokes    : {}
                };

                json.numStrokes = indices ? indices.length : self.strokes.length;

                var strokeCnt = 0;
                for (var i = 0; i < self.strokes.length; ++i) {
                    if (tIndices && tIndices.indexOf(i) === -1) {
                        continue;
                    }

                    var fldName = strokeCnt.toString();
                    strokeCnt ++;

//                    json.strokes[fldName] = self.getStrokeJsonObj(i);
                    json.strokes[fldName] = self.getStrokeJsonObject(self.strokes[i]);
                }

                return JSON.stringify(json);
            };

            this.removeEngine = function() {
                self.hwEngAgent.removeEngine(
                    function(resp) {
                        console.log("removeEngine request succeeded: removeEngineUuid = \"" + resp.removedEngineUuid + "\"");
                    },
                    function(resp) {
                        console.error("removeEngine request failed");
                    }
                );
            };

            this.updateUIControlState = function() {
                $("#parseTokenSet").prop("disabled",        self.getNumTokens() === null || self.getNumTokens() === 0);
                $("#removeLastToken").prop("disabled",      self.getNumTokens() === null || self.getNumTokens() === 0);
                $("#clearPaths").prop("disabled",           self.getNumTokens() === null || self.getNumTokens() <= 1);
                $("#mergeLast2Strokes").prop("disabled",    self.getNumTokens() === null || self.getNumTokens() <= 1);
                $("#mergeLast2StrokesAlt").prop("disabled", self.getNumTokens() === null || self.getNumTokens() <= 1);
                $("#mergeLast3Strokes").prop("disabled",    self.getNumTokens() === null || self.getNumTokens() <= 2);
                $("#mergeLast4Strokes").prop("disabled",    self.getNumTokens() === null || self.getNumTokens() <= 3);
                $("#mergeStrokesDropdown").prop("disabled", self.getNumTokens() === null || self.getNumTokens() <= 1);
                $("#unmergeLastToken").prop("disabled",     self.getNumTokens() === null || !self.hwEngAgent.isLastTokenMerged())

                $("#undoStrokeCuratorUserAction").prop("disabled", !self.canUndoStrokeCuratorUserAction());
                $("#redoStrokeCuratorUserAction").prop("disabled", !self.canRedoStrokeCuratorUserAction());

            };

        };

        return TouchManager;
    }
);