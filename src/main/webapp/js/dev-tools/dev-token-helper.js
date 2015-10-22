define(["underscore"], function(_) {
    'use strict';

    var DevTokenHelper = function() {
        var self = this;

        var n_w = 16;
        var n_h = 16;

        self.AddStrokeAction = "add-stroke";

        /**
         * Transform strokes and token name into the content of the im and wt files
         * @param strokes
         * @param tokenName
         * @returns {string}
         */
        this.getDevFileString = function(strokes, tokenName) {
            if (typeof tokenName !== "string" || tokenName.length === 0) {
                throw new Error("Invalid token name");
            }

            var nStrokes = strokes.length;

            /* wt file content string */
            var wtStr = "Token name: " + tokenName + "\n";
            wtStr += "CWrittenToken (nStrokes=" + nStrokes + "):\n";

            /* wt file content string */
            var imStr = "Token name: " + tokenName + "\n";
            imStr += "n_w = " + n_w + "\n";
            imStr += "n_h = " + n_h + "\n";
            imStr += "ns = " + nStrokes + "\n";

            var xMin = Number.POSITIVE_INFINITY;
            var xMax = Number.NEGATIVE_INFINITY;
            var yMin = Number.POSITIVE_INFINITY;
            var yMax = Number.NEGATIVE_INFINITY;

            for (var i = 0; i < nStrokes; ++i) {
                var stroke = strokes[i];

                var np = stroke.length;

                if (np === 0) {
                    throw new Error("Cannot handle stroke with zero points");
                }

                wtStr += "Stroke (np=" + np + "):\n";

                var xsStr = "\txs=[";
                var ysStr = "\tys=[";

                for (var j = 0; j < np; ++j) {
                    var x = stroke[j][0];
                    var y = stroke[j][1];

                    if (x < xMin) { xMin = x; }
                    if (x > xMax) { xMax = x; }
                    if (y < yMin) { yMin = y; }
                    if (y > yMax) { yMax = y; }

                    xsStr += x;
                    ysStr += y;

                    if (j < np - 1) {
                        xsStr += ", ";
                        ysStr += ", ";
                    }
                }

                xsStr += "]\n";
                ysStr += "]\n";

                wtStr += xsStr;
                wtStr += ysStr;
            }

            imStr += "w = " + (xMax - xMin) + "\n";
            imStr += "h = " + (yMax - yMin) + "\n";

            /* Add dummy -1 data to imStr */
            for (var ih = 0; ih < n_h; ++ih) {
                for (var iw = 0; iw < n_w; ++iw) {
                    imStr += "0 ";
                }
                imStr += "\n";
            }

            return {
                "im": imStr,
                "wt": wtStr
            };
        };

        this.actionDataAddStrokeToFileStrings = function(actionDataArray, tokenName) {
            if (typeof tokenName !== "string" || tokenName.length === 0) {
                throw new Error("Invalid token name");
            }

            if ( !Array.isArray(actionDataArray) ) {
                throw new Error("Input data is not an array");
            }

            if (actionDataArray.length === 0) {
                throw new Error("Cannot token with no strokes");
            }

            var nStrokes = actionDataArray.length;
            var wtStr = "Token name: " + tokenName + "\n";
            wtStr += "CWrittenToken (nStrokes=" + nStrokes + "):\n";
            for (var i = 0; i < nStrokes; ++i) {
                var actionData = actionDataArray[i];

                if (actionData.action !== self.AddStrokeAction) {
                    throw new Error("Unexpected action: " + actionData.action);
                }



                var np = actionData.stroke.numPoints;

                if (np === 0) {
                    throw new Error("Cannot handle stroke with zero points");
                }

                wtStr += "Stroke (np=" + np + "):\n";

                var xsStr = "\txs=[";
                var ysStr = "\tys=[";

                for (var j = 0; j < np; ++j) {
                    xsStr += String(actionData.stroke.x[j]);
                    ysStr += String(actionData.stroke.y[j]);

                    if (j < np - 1) {
                        xsStr += ", ";
                        ysStr += ", ";
                    }
                }

                xsStr += "]\n";
                ysStr += "]\n";

                wtStr += xsStr;
                wtStr += ysStr;
            }

            return wtStr;

        };
    };

    return DevTokenHelper;

});