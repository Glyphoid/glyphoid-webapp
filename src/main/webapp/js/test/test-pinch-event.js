/* Unit test for pinch-event */
define(["jasq"], function (jasq) {
    "use strict";

    describe("Tests for pinch-event", "pinch-event", function () {

        it("Pinch event test 1: Pan", function(PinchEvent) {
            var pinchEvent = new PinchEvent();

            expect(typeof pinchEvent.fixedPinchZoomRatio).toBe("number");
            expect(pinchEvent.fixedPinchZoomRatio > 0).toBe(true);

            var prevPos1 = [
                [0, 0], [1, 1]
            ];
            var currPos1 = [
                [0.5, 0.5], [1.5, 1.5]
            ];

            var output1 = pinchEvent.getMovementInfo(prevPos1, currPos1);

            expect(output1.type).toBe("PinchPan");
            expect(output1.panX).toBe(0.5);
            expect(output1.panY).toBe(0.5);
            expect(output1.zoomRatio).toBe(null);
        });

        it("Pinch event test 2: Pan", function(PinchEvent) {
            var pinchEvent = new PinchEvent();

            var prevPos1 = [
                [0, 0], [1, 1]
            ];
            var currPos1 = [
                [0.5, -0.25], [1.25, 1.25]
            ];

            var output1 = pinchEvent.getMovementInfo(prevPos1, currPos1);

            expect(output1.type).toBe("PinchPan");
            expect(output1.panX).toBe(0.375);
            expect(output1.panY).toBe(0);
            expect(output1.zoomRatio).toBe(null);
        });

        it("Pinch event test 3: Zoom in", function(PinchEvent) {
            var pinchEvent = new PinchEvent();

            var prevPos1 = [
                [0, 0], [1, 1]
            ];
            var currPos1 = [
                [0.25, 0.25], [0.75, 0.75]
            ];

            var output1 = pinchEvent.getMovementInfo(prevPos1, currPos1);

            expect(output1.type).toBe("PinchZoom");
            expect(output1.panX).toBe(null);
            expect(output1.panY).toBe(null);
            expect(output1.zoomRatio).toBe(-pinchEvent.fixedPinchZoomRatio); // Zoom in
        });

        it("Pinch event test 4: Zoom out", function(PinchEvent) {
            var pinchEvent = new PinchEvent();

            var prevPos1 = [
                [0, 0], [1, 1]
            ];
            var currPos1 = [
                [-0.5, 0], [1.5, 1]
            ];

            var output1 = pinchEvent.getMovementInfo(prevPos1, currPos1);

            expect(output1.type).toBe("PinchZoom");
            expect(output1.panX).toBe(null);
            expect(output1.panY).toBe(null);
            expect(output1.zoomRatio).toBe(pinchEvent.fixedPinchZoomRatio); // Zoom out
        });

        it("Pinch event test 5: Zoom out", function(PinchEvent) {
            var pinchEvent = new PinchEvent();

            var prevPos1 = [
                [0, 0], [1, 1]
            ];
            var currPos1 = [
                [-0.5, 0], [1.5, 1]
            ];

            var output1 = pinchEvent.getMovementInfo(prevPos1, currPos1);

            expect(output1.type).toBe("PinchZoom");
            expect(output1.panX).toBe(null);
            expect(output1.panY).toBe(null);
            expect(output1.zoomRatio).toBe(pinchEvent.fixedPinchZoomRatio); // Zoom out
        });

    });

});
