define([], function() {
    var PinchEvent = function() {};

    PinchEvent.prototype.fixedPinchZoomRatio = 0.02;

    /**
     * Get the type of the pinch movement
     * @param prevPos: Previous positions of the two touch points
     *        Requires prevPos.length === 2,
     *                 prevPos[0].length === 2: x and y positions of the 1st touch point
     *                 prevPos[1].length === 2: x and y positions of the 2nd touch point
     * @param currPos: Current positions of the two touch points
     *        Requires currPos.length = 2,
     *                 currPos[0].length === 2: x and y positions of the 1st touch point
     *                 currPos[1].length === 2: x and y positions of the 2nd touch point
     * @return
     *     output.type: Pinch movement type: "PinchPan" or "PinchZoom"
     *     output.panX, output.panY: (If applicable) amount of pan in x and y dimensions, null if not applicable
     *     output.zoomRation:        (If applicable) amount of zoom, null if not applicable
     */
    PinchEvent.prototype.getMovementInfo = function(prevPos, currPos) {
        if (prevPos.length !== 2 || prevPos[0].length !== 2 || prevPos[1].length !== 2 ||
            typeof prevPos[0][0] !== "number" || typeof prevPos[0][1] !== "number" ||
            typeof prevPos[1][0] !== "number" || typeof prevPos[1][1] !== "number") {
            throw "Incorrect input argument: prevPos";
        }
        if (currPos.length !== 2 || currPos[0].length !== 2 || currPos[1].length !== 2 ||
            typeof currPos[0][0] !== "number" || typeof currPos[0][1] !== "number" ||
            typeof currPos[1][0] !== "number" || typeof currPos[1][1] !== "number") {
            throw "Incorrect input currPos: currPos";
        }

        /* Displacement of the first touch point */
        var xDsp0 = currPos[0][0] - prevPos[0][0];
        var yDsp0 = currPos[0][1] - prevPos[0][1];

        /* Displacement of the second touch point */
        var xDsp1 = currPos[1][0] - prevPos[1][0];
        var yDsp1 = currPos[1][1] - prevPos[1][1];

        var output = {
            type      : null,
            panX      : null,
            panY      : null,
            zoomRatio : null
        };

        if (xDsp0 * xDsp1 > 0 || yDsp0 * yDsp1 > 0) {
            /* Pan */
            output.type = "PinchPan";

            output.panX = (xDsp0 + xDsp1) * 0.5;
            output.panY = (yDsp0 + yDsp1) * 0.5;
        } else {
            output.type = "PinchZoom";

            /* Previous distance */
            var dstPrev = (prevPos[0][0] - prevPos[1][0]) * (prevPos[0][0] - prevPos[1][0]) +
                          (prevPos[0][1] - prevPos[1][1]) * (prevPos[0][1] - prevPos[1][1]);
            /* Current distance */
            var dstCurr = (currPos[0][0] - currPos[1][0]) * (currPos[0][0] - currPos[1][0]) +
                          (currPos[0][1] - currPos[1][1]) * (currPos[0][1] - currPos[1][1]);

            if (dstCurr > dstPrev) { /* Zoom out */
                output.zoomRatio = PinchEvent.prototype.fixedPinchZoomRatio;
            } else if (dstCurr < dstPrev) { /* Zoom in */
                output.zoomRatio = -1 * PinchEvent.prototype.fixedPinchZoomRatio;
            }

        }

        return output;

    };

   return PinchEvent;
});