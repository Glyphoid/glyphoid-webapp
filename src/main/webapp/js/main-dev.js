define(["underscore", "jquery", "dev-token-helper", "aws-helper"], function(_, $, DevTokenHelper, AwsHelper) {
    'use strict';

    var MainDev = function(tGestureManager) {
        var self = this;

        this.options = {
            devEndpointUrl    : "dev",
            dataFileRootDir   : ""
        };

        this.devTokenHelper = new DevTokenHelper();

        this.lastAddStrokeIndex = NaN;
        this.sessionID = "";

        if (typeof tGestureManager != "object") {
            throw new Error("Invalid GestureManager object in input argument(s)");
        }

        self.gestureManager = tGestureManager;

        /**
         * Get the file names for the currently selected token
         * @returns {{im: string, wt: string}}
         */
        this.getTokenFileNames = function() {
            if (self.sessionID.length === 0) {
                throw new Error("No session selected");
            }

            var fileNameMain = "L_" + self.sessionID + "_" + self.lastAddStrokeIndex + "_";
            var tokenIdx = self.gestureManager.cursorSelectedTokenIndices[self.gestureManager.cursorSelectedTokenIndices.length - 1];
            var tokenName = self.gestureManager.tokenNames[tokenIdx];
            fileNameMain += tokenName;

            return {
                "im": fileNameMain + ".im",
                "wt": fileNameMain + ".wt"
            }

        };

        /**
         * Save the currently selected token (last one) to wt and im file
         */
        this.saveSelectedTokenToFiles = function() {
            var fileContents = self.gestureManager.getSelectedTokenDevFileString();

            var fileNames = self.getTokenFileNames();

            var imPostData = {
                filePath    : self.options.dataFileRootDir + fileNames.im,
                fileContent : fileContents.im
            };

            var wtPostData = {
                filePath    : self.options.dataFileRootDir + fileNames.wt,
                fileContent : fileContents.wt
            };

            $.ajax({
                url:      self.options.devEndpointUrl,
                method:   "POST",
                data:     JSON.stringify(imPostData),
                complete: function (resp1) {
                    if (resp1.status === 200 && resp1.responseJSON.isError === false) {
                        $.ajax({
                            url:      self.options.devEndpointUrl,
                            method:   "POST",
                            data:     JSON.stringify(wtPostData),
                            complete: function (resp2) {
                                if (resp1.status === 200 && resp1.responseJSON.isError === false) {
                                    console.log("Successfully saved data to files: " + fileNames.wt + " & " + fileNames.im);
                                } else {
                                    throw new Error("Dev request to save wt file failed");
                                }
                            }
                        });
                    } else {
                        var errMsg = "Dev request to save im file failed";
                        if (resp1.responseJSON.result.indexOf("already exist")) {
                            errMsg += ": File already exists";
                        }

                        alert(errMsg);
//                        throw new Error(errMsg);
                    }

                }
            });
        };

        $(function () {
            if ( $("#getSessions").length === 0 ) {
                return; // Not dev environment
            }


            var sessionListItemTemp =
                _.template("<div id='selectSession_<%= sessionID %>'>-&nbsp;<%= sessionPrefix %>(<%= clientIPAddress %>)</div>");

            var actionListItemTemp =
                _.template("<%= actionOrderNum %>&nbsp;-&nbsp;<button class='devButton actionButton' id='selectAction_<%= actionID %>'>Select action </button> <%= action %><br/>");

            var awsHelper = {
                instance: null
            };

            // getSessionData callback
            var getSessionDataCallback = function (sessionData) {
                $("#actionList").empty();

                for (var i = 0; i < sessionData.length; ++i) {
                    var sessionDataAug = sessionData[i];
                    sessionDataAug.actionOrderNum = i;
                    var actionListItem = actionListItemTemp(sessionDataAug);

                    $("#actionList").append(actionListItem);

                    //Bind click callback to the button
                    var selectActionButtonID = "selectAction_" + sessionData[i].actionID;
                    $("#" + selectActionButtonID).off("click").on("click", function (e) {
                        e.preventDefault();

                        var target = e.target;
                        $(e.target).css("background-color", "#888");
                        $(e.target).css("color", "#fff");

                        var actionID = target.id.replace("selectAction_", "");
                        var actionObjectKey = awsHelper.instance.actionID2ObjectKey[actionID];

                        var actionIdx = actionID.split("_")[0];
                        var action = actionID.split("_")[1];
                        if (action === "add-stroke") {
                            self.lastAddStrokeIndex = actionIdx;
                        }

                        awsHelper.instance.getActionData(actionObjectKey, performAction);

                    });
                }
            };

            var performAction = function (actionData) {
                self.gestureManager.options.enableEdgeGuard = false; // Disable edge guards

                var eventData;
                if (actionData.action === "add-stroke") {
                    for (var i = 0; i < actionData.stroke.numPoints; ++i) {
                        var event;

                        // The coordinates in the actionData are in world coordinats.
                        var worldX = actionData.stroke.x[i];
                        var worldY = actionData.stroke.y[i];

                        // Make sure that the view port is large enough, zoom in if necessary
                        var isInFov = function(x, y, fov) {
                            return fov[0] < x && fov[2] > x &&
                                   fov[1] < y && fov[3] > y;
                        };

                        var zoomStep = 0.05;
                        var nZoom = 0;
                        while ( !isInFov(worldX, worldY, self.gestureManager.viewPort.fov) ) {
                            self.gestureManager.viewPort.fovZoom(zoomStep);
                            ++nZoom;
                        }

                        if (nZoom !== 0) {
                            self.gestureManager.redraw();
                            console.log("Zoomed FOV out to accommodate data");
                        }

                        // Get canvas position
                        var canvasXY = self.gestureManager.viewPort.worldPos2CanvasPos([worldX, worldY]);
                        var canvasX = canvasXY[0];
                        var canvasY = canvasXY[1];

                        eventData = {
                            button: 0, //TODO: Deduplicate with plato-gesture-manager
                            offsetX: canvasX,
                            offsetY: canvasY
                        };

                        // Get event name
                        var eventName = "";
                        if (i == 0) {
                            eventName = "mousedown";
                        } else if (i == actionData.stroke.numPoints - 1) {
                            eventName = "mouseup";
                        } else {
                            eventName = "mousemove";
                        }

                        event = $.Event(eventName, eventData);

                        self.gestureManager.el.trigger(event);
                    }

                    if (actionData.stroke.numPoints === 1) { // A single dot
                        // Trigger the mouse up event, which will otherwise not be triggered
                        event = $.Event("mouseup", eventData);
                        self.gestureManager.el.trigger(event);
                    }

                    var wtStr = self.devTokenHelper.actionDataAddStrokeToFileStrings([actionData], "x"); //DEBUG

                } else if (actionData.action === "remove-token") {
                    self.gestureManager.removeStrokesOfToken(actionData.idxToken);
                    self.gestureManager.removeToken(actionData.idxToken);
                } else if (actionData.action === "force-set-token-name") {
                    self.gestureManager.forceSetTokenRecogWinner(actionData.tokenIdx, actionData.tokenRecogWinner);
                } else if (actionData.action === "clear") {
                    self.gestureManager.clear();
                } else if (actionData.action === "parse-token-set") {
                    $("#parseTokenSet").trigger($.Event("click"));
                } else if (actionData.action === "merge-strokes-as-token") {
                    $("#mergeLast2Strokes").trigger($.Event("click"));
                    self.gestureManager.mergeStrokesAsToken(actionData.strokeIndices);
                } else if (actionData.action === "remove-last-token") {
                    self.gestureManager.removeLastToken();
                } else {
                    throw new Error("Action not supported by MainDev: " + actionData.action)
                }

            };


            $("#getSessions").on("click", function (e) {
                var awsAccessKey = $("#awsAccessKey").val().trim();
                var awsSecretKey = $("#awsSecretKey").val().trim();

                if (awsAccessKey.length === 0) {
                    throw new Error("ERROR: AWS access key not specified");
                }
                if (awsSecretKey.length === 0) {
                    throw new Error("ERROR: AWS secret key not specified");
                }

                var bucketName = "plato-dev-1";

                e.preventDefault();

                if (awsHelper.instance === null) {
                    awsHelper.instance = new AwsHelper({
                        bucketName: bucketName,
                        region: $("#awsRegion").val(),
                        accessKey: awsAccessKey,
                        secretKey: awsSecretKey
                    });
                }

                var datePrefix = awsHelper.instance.formatDateISO($("#datePrefix").val());
                var clientType = $("#clientType").val();
                // TODO: Add validations

                awsHelper.instance.getSessionPrefixes(datePrefix, clientType, function (sessions) {
                    $("#sessionList").empty();

                    var handleSession = function(i) {
                        awsHelper.instance.getAdditionalClientData(sessions[i].objectKey, function(additionalClientData) {
                            if (typeof additionalClientData.ClientIPAddress === "undefined") {
                                throw new Error("Undefined clientIPAddress in create-engine object: " + sessions[i].objectKey);
                            }

                            sessions[i].clientIPAddress = additionalClientData.ClientIPAddress;
                            var sessionListItem = sessionListItemTemp(sessions[i]);
                            $("#sessionList").append(sessionListItem);


                        });
                    };

                    for (var i = 0; i < sessions.length; ++i) {
                        var handleSessionBound = handleSession.bind(self, i);

                        handleSessionBound();
                    }
                });


            });

            $("#sortSessions").on("click", function(e) {
                e.preventDefault();

                $("#sessionList").children().sort(function(a, b) {
                    if ($(a).html() > $(b).html()) {
                        return 1;
                    } else if (($(a).html() < $(b).html())) {
                        return -1;
                    } else {
                        return 0;
                    }
                }).each(function () {
                    var elem = $(this);
                    elem.remove();

                    $("#sessionList").append($(elem));

                    //Bind click callback to the button
                    $(elem).off("click").on("click", function (e) {
                        e.preventDefault();

                        var target = e.target;
                        $(e.target).css("color", "#888");

                        self.sessionID = e.target.id.split("_")[1];

                        var sessionPrefix = awsHelper.instance.sessionID2Prefix[target.id.replace("selectSession_", "")];

                        awsHelper.instance.getSessionData(sessionPrefix, getSessionDataCallback);

                    });
                });
            });

            $(".actionButton").on("click", function(e) {
                $("#" + e.target.id).attr("disabled", "disabled");
            });

            // Dev
            $("#saveTokenToFile").on("click", function() {
                var saveDirectory = $("#saveDirectory").val().trim();
                if (saveDirectory.length === 0) {
                    throw new Error("ERROR: Save target directory not specified.");
                }

                self.options.dataFileRootDir = saveDirectory;

                self.saveSelectedTokenToFiles();
            });
        });

    };

    return MainDev;
});