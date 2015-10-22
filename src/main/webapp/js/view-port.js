define([], function() {
    'use strict';

    var ViewPort = function(options) {
        if (typeof options !== "object" ||
            typeof options.width !== "number" ||
            typeof options.height !== "number") {
            throw new Error("Incorrect input options to ViewPort constructor");
        }

        var self = this;

        /* Field of view (FOV), in world coordinates */
        this.canvasWidth = options.width;
        this.canvasHeight = options.height;

        // Field of view in world coordinates: Length-4 array: [xMin, yMin, xMax, yMax]
        this.fov = [0.0, 0.0, this.canvasWidth, this.canvasHeight];

        this.dx = 0.0;    // Translation x
        this.dy = 0.0;    // Translation y
        this.scale = 1.0; // Scaling factor

        /* FOV Pan */
        this.fovPan = function(panX, panY) {
            self.fov[0] += panX;
            self.fov[1] += panY;
            self.fov[2] += panX;
            self.fov[3] += panY;

            self.dx += panX;
            self.dy += panY;

            return self.fov;
        };

        this.relativeFovPan = function(relPanX, relPanY) {
            self.fovPan(relPanX * (self.fov[2] - self.fov[0]),
                        relPanY * (self.fov[3] - self.fov[1]));
        };

        /**
         * FOV Zoom:
         * @param zoomRatio: Ratio of zooming
         *                      > 0: zoom in
         *                      < 0; zoom out
         * @return fov */
        this.fovZoom = function(zoomRatio) {
            /* Calculate the center of the FOV. Zoom with respect to it */
            var ctrX = (self.fov[0] + self.fov[2]) * 0.5;
            var ctrY = (self.fov[1] + self.fov[3]) * 0.5;

            var halfW = (self.fov[2] - self.fov[0]) * 0.5;
            var halfH = (self.fov[3] - self.fov[1]) * 0.5;

            var r = 1.0 + zoomRatio;

            halfW *= r;
            halfH *= r;

            /* Update FOV */
            self.fov[0] = ctrX - halfW;
            self.fov[1] = ctrY - halfH;
            self.fov[2] = ctrX + halfW;
            self.fov[3] = ctrY + halfH;

            /* Update the state variables */
            self.scale *= r;

            /* TODO: Handle overflow */
            return self.fov;
        };

        /* Resest FOV to default */
        this.fovReset = function() {
            self.fov = [0, 0, self.canvasWidth, self.canvasHeight];

            self.dx = 0.0;
            self.dy = 0.0;
            self.scale = 1.0;
        };

        /* Coordination transform functions */
        /* Input sanity check*/
        this.checkInputXY = function(xy) {
            if ( !Array.isArray(xy) ) {
                throw "Input is not an array";
            }

            if ( xy.length != 2 ) {
                throw "Incorrect input length";
            }
        };

        /* Translate world coordinates to canvas coordinates */
        this.worldPos2CanvasPos = function(xy) {
            self.checkInputXY(xy);

            var x = xy[0];
            var y = xy[1];
            if ( !(typeof x === "number" && typeof y === "number") ) {
                throw "Input x and y are not both numbers";
            }

            // Relative x and y: in [0, 1] interval
            var rx = (x - self.fov[0]) / (self.fov[2] - self.fov[0]);
            var ry = (y - self.fov[1]) / (self.fov[3] - self.fov[1]);

            var canvasXY = new Array(2);
            canvasXY[0] = rx * self.canvasWidth;
            canvasXY[1] = ry * self.canvasHeight;

            return canvasXY;
        };

        /* Translate canvas coordinates to world coordinates */
        this.canvasPos2WorldPos = function(xy) {
            self.checkInputXY(xy);

            var x = xy[0];
            var y = xy[1];
            if ( !(typeof x === "number" && typeof y === "number") ) {
                throw "Input x and y are not both numbers";
            }

            var worldXY = new Array(2);

            // Relative x and y: in [0, 1] interval
            var rx = x / self.canvasWidth;
            var ry = y / self.canvasHeight;

            worldXY[0] = self.fov[0] + rx * (self.fov[2] - self.fov[0]);
            worldXY[1] = self.fov[1] + ry * (self.fov[3] - self.fov[1]);

            return worldXY;
        };

        /**
         * Translate canvas displacement vector to world displacement vector
         * @param       canvasDxy:  dx and dy array in canvas coordinates
         * @return      worldDxy:   dx and dy array in world coordinates
         */
        this.canvasDisplacementVector2WorldDisplacementVector = function(canvasDxy) {
            self.checkInputXY(canvasDxy);

            var worldDxy = new Array(canvasDxy.length);

            for (var i = 0; i < worldDxy.length; ++i) {
                worldDxy[i] = canvasDxy[i] * self.scale;
            }

            return worldDxy;
        };

        /* Map world bounds to canvas bounds */
        this.worldBnds2CanvasBnds = function(bnds) {
            if ( !Array.isArray(bnds) || bnds.length != 4 ) {
                throw "Incorrect input bnds";
            }

            var worldCorner0 = new Array(2);
            worldCorner0[0] = bnds[0]; /* x_min */
            worldCorner0[1] = bnds[1]; /* y_min */

            var worldCorner1 = new Array(2);
            worldCorner1[0] = bnds[2]; /* x_max */
            worldCorner1[1] = bnds[3]; /* y_max */

            var canvasCorner0 = self.worldPos2CanvasPos(worldCorner0);
            var canvasCorner1 = self.worldPos2CanvasPos(worldCorner1);

            var canvasBnds = new Array(4);
            canvasBnds[0] = canvasCorner0[0]; /* x_min */
            canvasBnds[1] = canvasCorner0[1]; /* y_min */
            canvasBnds[2] = canvasCorner1[0]; /* x_max */
            canvasBnds[3] = canvasCorner1[1]; /* y_max */

            return canvasBnds;
        };

    };

    return ViewPort;

});