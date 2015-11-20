define(["jquery", "jquery.blockUI"], function($) {
    'use strict';

    var HandwritingEngineAgent = function(options) {
        var self = this;

        this.writtenTokenSet = {};
        this.constituentStrokes = [];

        this.lastStrokeCuratorUserAction = null;
        this.canUndoStrokeCuratorUserAction = false;
        this.canRedoStrokeCuratorUserAction = false;

        this.debugLv = 1;

        /* Parameters */
        self.options = {
//            endpointUrl     : "http://127.0.0.1/plato/handwriting", //DEBUG TODO: Revert
            endpointUrl     : "handwriting",
            getSerializedStateCallback : null
        };

        /* Members */
        this.engineUuid = null;

        $.extend(true, self.options, options);

        this.getBrowserVersion = function(browserName) {
            var ver = "Unknown";

            var userAgentItems = window.navigator.userAgent.split(" ");
            for (var k = 0; k < userAgentItems.length; ++k) {
                if (userAgentItems[k].indexOf(browserName + "/") === 0) {
                    ver = userAgentItems[k].replace(browserName + "/", "");
                    break;
                }
            }

            return ver;
        };

        this.getCurrentBrowserNameVer = function() {
            var nameVer = {
                name : "Unknown",
                ver  : "Unknown"
            };

            if (window.navigator.appName === "Netscape") {
                if (window.navigator.vendor === "Google Inc.") {
                    nameVer.name = "Chrome";
                    nameVer.ver = this.getBrowserVersion(nameVer.name);
                } else if (window.navigator.userAgent.indexOf("Safari") !== -1 && window.navigator.userAgent.indexOf("AppleWebKit") !== -1) {
                    nameVer.name = "Safari";
                    nameVer.ver = this.getBrowserVersion(nameVer.name);
                } else {
                    nameVer.name = "Firefox";
                    nameVer.ver = this.getBrowserVersion(nameVer.name);
                }
            } else if (window.navigator.appName === "Opera") {
                nameVer.name = "Opera";
                nameVer.ver = this.getBrowserVersion("Presto");
            } else if (window.navigator.appName === "Microsoft Internet Explorer") { // TODO: New versions of MS IE
                nameVer.name = "IE";
            } else {

            }

            return nameVer;
        };

        this.getNewEngine = function(successCallback, errorCallback) {
            // Block UI
            $.blockUI();

            var browserNameVer = this.getCurrentBrowserNameVer();

            /* Determine the client type based on whether "devPanel" is available */
            var clientTypeMajor = "DesktopBrowser"; // TODO: How to determine if this is desktop or mobile?
            var clientTypeMinor = "DesktopBrowser_" + browserNameVer.name;
            if ($("#devPanel").length > 0) {
                clientTypeMajor = "API";
                clientTypeMinor = "API";
            }

            var reqData = JSON.stringify({
                "action"          : "create-engine",
                "ClientTypeMajor" : clientTypeMajor,
                "ClientTypeMinor" : clientTypeMinor,
                "ClientPlatformVersion" : browserNameVer.ver, // TODO
                "ClientAppVersion": "0.1", // TODO
                "CustomClientData" : {
                    "ScreenWidth"  : screen.width,
                    "ScreenHeight" : screen.height,
                    "UserAgent"    : window.navigator.userAgent
                }
            });

            $.ajax({
                url      : self.options.endpointUrl,
                method   : "POST",
                data     : reqData,
                complete : function(resp) {
                    if (resp.status === 200 && typeof resp.responseJSON.engineUuid === "string") {
                        self.engineUuid = resp.responseJSON.engineUuid;
                        if (self.debugLv > 0) {
                            console.log("New engine UUID = \"" + self.engineUuid + "\"");
                        }

                        if (typeof successCallback === "function") {
                            successCallback();

                        }
                    } else {
                        if (typeof errorCallback === "function") {
                            errorCallback();
                        }
                    }
                    $.unblockUI();
                }
            });
        };

        this.removeEngine = function(successCallback, errorCallback) {
            // Block UI
            $.blockUI();

            if (typeof this.engineUuid === "undefined" || this.engineUuid === null) {
//                throw "engineUuid is undefined or null";
                return;
            }

            var reqData = JSON.stringify({
                "action"          : "remove-engine",
                "engineUuid"      : this.engineUuid
            });

            $.ajax({
                url      : self.options.endpointUrl,
                method   : "POST",
                data     : reqData,
                complete : function(resp) {
                    if (resp.status === 200) {
                        self.engineUuid = null;

                        successCallback(resp.responseJSON);
                    }
                    else {
                        errorCallback(resp.responseJSON);
                    }

                    $.unblockUI();
                }
            });
        };

        var engineCall = function(actionStr, additionalData, successCallback, errorCallback) {
            // Block UI
            $.blockUI();

            var t0 = new Date();

            if ( !self.engineUuid ) {
                errorCallback("Null engine UUID");
            }

            var reqDataObj = {
                "action"     : actionStr,
                "engineUuid" : self.engineUuid
            };

            if (additionalData) {
                for (var fld in additionalData) {
                    if ( !additionalData.hasOwnProperty(fld) ) {
                        continue;
                    }
                    reqDataObj[fld] = additionalData[fld];
                }
            }

            var reqData = JSON.stringify(reqDataObj);

            $.ajax({
                url      : self.options.endpointUrl,
                method   : "POST",
                data     : reqData,
                complete : function(resp) {
                    var t1 = new Date();
                    var elapsedTime = t1.getTime() - t0.getTime();

                    /* Check if engine has expired */
                    if (typeof resp.responseJSON === "object" && Array.isArray(resp.responseJSON.errors) &&
                        resp.responseJSON.errors.length === 1 &&
                        resp.responseJSON.errors[0].indexOf("Engine UUID is invalid:") !== -1) {

                        console.log("Engine expired"); //DEBUG

                        /* Get serialized state */
                        var serializedState = self.options.getSerializedStateCallback();
                        self.getNewEngine(
                            function() {
                                console.log("Created new engine");

                                /* Inject state */
                                self.injectState(serializedState,
                                    function() {
                                        /* Now proceeded to re-attempt of the original action */
                                        /* Recursive call */
                                        engineCall(actionStr, additionalData, successCallback, errorCallback);
                                        //TODO: Make sure stack overflow will never happen.
                                    },
                                    function() {
                                        console.error("Attempt to inject state after creation of new engine failed");
                                    });
                            },
                            function() {
                                console.error("Attempt to create new engine failed");
                            }
                        );


                    } else {

                        if (typeof resp === "object" &&
                            typeof resp.responseJSON === "object" &&
                            typeof resp.responseJSON.writtenTokenSet === "object") {
                            /* Update written token set state */
                            self.writtenTokenSet = resp.responseJSON.writtenTokenSet;

                            if (Array.isArray(resp.responseJSON.constituentStrokes)) {
                                self.constituentStrokes = resp.responseJSON.constituentStrokes;
                            }

                            /* Update undo/redo state */
                            self.lastStrokeCuratorUserAction    = resp.responseJSON.lastStrokeCuratorUserAction;
                            self.canUndoStrokeCuratorUserAction = resp.responseJSON.canUndoStrokeCuratorUserAction;
                            self.canRedoStrokeCuratorUserAction = resp.responseJSON.canRedoStrokeCuratorUserAction;
                        }

                        if (resp.status === 200 && typeof resp.responseJSON === "object") {
                            successCallback(resp.responseJSON, elapsedTime);
                        }
                        else {
                            errorCallback("Failure occurred during \"" + actionStr + "\" request to handwriting servlet");
                        }

                        $.unblockUI(); // The callback has ended. Now it is safe to unblock the UI?
                    }
                }
            });
        };

        /* Get number of tokens */
        this.getNumTokens = function() {
            if (typeof self.writtenTokenSet === "object" &&
                Array.isArray(self.writtenTokenSet.tokens)) {
                return self.writtenTokenSet.tokens.length;
            } else {
                return null;
            }
        };
        /* Determine if the last token contains merged. */
        this.isLastTokenMerged = function() {
            if (Array.isArray(self.constituentStrokes) &&
                self.constituentStrokes.length > 0) {
                var len = self.constituentStrokes.length;
                return self.constituentStrokes[len - 1].length > 1;
            } else {
                return false;
            }
        };

        /* Get the constituent stroke indices of the last token */
        this.getLastTokenConstituentStrokeIndices = function() {
            if (Array.isArray(self.constituentStrokes) &&
                self.constituentStrokes.length > 0) {
                var len = self.constituentStrokes.length;
                return self.constituentStrokes[len - 1];
            } else {
                return null;
            }
        };

        /* Action: Get all token names (not display names) */
        this.getAllTokenNames = function(successCallback, errorCallback) {
            var f = engineCall.bind(self, "get-all-token-names", null);

            f(successCallback, errorCallback);
        };

        /* Action: Add stroke */
        this.addStroke = function(lastStroke, successCallback, errorCallback) {
//            console.log("lastStroke = \"" + JSON.stringify(lastStroke) + "\""); //DEBUG

            var additionalData = { "stroke" : lastStroke };
            var f = engineCall.bind(self, "add-stroke", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: Inject state */
        this.injectState = function(state, successCallback, errorCallback) {
            var additionalData = { "stateData" : state };

            var f = engineCall.bind(self, "inject-state", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: merge strokes of specified indices */
        this.mergeStrokesAsToken = function(strokeIndices, successCallback, errorCallback) {
            var additionalData = {
                "strokeIndices" : strokeIndices
            };

            var f = engineCall.bind(self, "merge-strokes-as-token", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: Force setting recognition winner of a token */
        this.forceSetTokenRecogWinner = function(tokenIdx, tokenName, successCallback, errorCallback) {
            var additionalData = {
                "tokenIdx": tokenIdx,
                "tokenRecogWinner": tokenName
            };

            var f = engineCall.bind(self, "force-set-token-name", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: Clear */
        this.clear = function(successCallback, errorCallback) {
            var f = engineCall.bind(self, "clear", null);

            f(successCallback, errorCallback);
        };

        /* Action: Remove i-th token */
        this.removeToken = function(idxToken, successCallback, errorCallback) {
            /* Input sanity check */
            if (typeof idxToken !== "number") {
                throw "Invalid type in idxToken";
            }

            var additionalData = {
                "idxToken" : idxToken
            };

            var f = engineCall.bind(self, "remove-token", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: Remove last token */
        this.removeLastToken = function(successCallback, errorCallback) {
            var f = engineCall.bind(self, "remove-last-token", null);

            f(successCallback, errorCallback);
        };


        /**
         * Move a single token
         * @param   tokenIdx: token index
         * @param   newBounds: new bounds of tkoen
         * */
        this.moveToken = function(tokenIdx, newBounds, successCallback, errorCallback) {
            /* Input sanity check */
            if (typeof tokenIdx !== "number") {
                throw new Error("Invalid input token index");
            }

            if (!Array.isArray(newBounds) || newBounds.length !== 4) {
                throw new Error("Invalid input new bounds");
            }

            var additionalData = {
                "tokenIdx"  : tokenIdx,
                "newBounds" : newBounds
            };

            var f = engineCall.bind(self, "move-token", additionalData);

            f(successCallback, errorCallback);
        };

        /**
         * Move multiple tokens
         * @param tokenIndices
         * @param newBoundsArray
         * @param successCallback
         * @param errorCallback
         */
        this.moveMultipleTokens = function(tokenIndices, newBoundsArray, successCallback, errorCallback) {
            /* Input sanity check */
            if (!Array.isArray(tokenIndices)) {
                throw new Error("Invalid input token index array");
            }

            if (!Array.isArray(newBoundsArray)) {
                throw new Error("Invalid input new bounds array");
            }

            if (tokenIndices.length != newBoundsArray.length) {
                throw new Error("Length mismatch between input tokenIndices and newBoundsArray");
            }

            var additionalData = {
                "tokenIndices"  : tokenIndices,
                "newBoundsArray" : newBoundsArray
            };

            var f = engineCall.bind(self, "move-multiple-tokens", additionalData);

            f(successCallback, errorCallback);
        };

        /* Action: Parse token set */
        this.parseTokenSet = function(successCallback, errorCallback) {
            var f = engineCall.bind(self, "parse-token-set", null);

            f(successCallback, errorCallback);
        };

        /**
         *  Action: Undo stroke curator user action
         * */
        this.undoStrokeCuratorUserAction = function(successCallback, errorCallback) {
            if (self.canUndoStrokeCuratorUserAction) {
                var f = engineCall.bind(self, "undo-stroke-curator-user-action", null);

                f(successCallback, errorCallback);
            } else {
                throw "The current state of the stroke curator does not permit undo";
            }
        };

        /**
         *  Action: Redo stroke curator user action
         * */
        this.redoStrokeCuratorUserAction = function(successCallback, errorCallback) {
            if (self.canRedoStrokeCuratorUserAction) {
                var f = engineCall.bind(self, "redo-stroke-curator-user-action", null);

                f(successCallback, errorCallback);
            } else {
                throw "The current state of the stroke curator does not permit redo";
            }
        };
    };

    return HandwritingEngineAgent;
});