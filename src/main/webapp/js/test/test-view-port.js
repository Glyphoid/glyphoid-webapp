define(["jasq"], function (jasq) {
    "use strict";

    describe("Tests for view-port", "view-port", function () {

        it("View port test 1: Pan and zoom", function (ViewPort) {
            var viewPort = new ViewPort({
                width: 400,
                height: 300
            });

            expect(viewPort.canvasWidth).toBe(400);
            expect(viewPort.canvasHeight).toBe(300);

            expect(viewPort.worldPos2CanvasPos([10, 10])).toEqual([10, 10]);
            expect(viewPort.canvasPos2WorldPos([10, 10])).toEqual([10, 10]);

            // Pan left
            viewPort.fovPan(-20, 0);
            expect(viewPort.worldPos2CanvasPos([10, 10])).toEqual([30, 10]);
            expect(viewPort.canvasPos2WorldPos([10, 10])).toEqual([-10, 10]);

            // Pan up
            viewPort.fovPan(0, -30);
            expect(viewPort.worldPos2CanvasPos([10, 10])).toEqual([30, 40]);
            expect(viewPort.canvasPos2WorldPos([10, 10])).toEqual([-10, -20]);

            // Pan back
            viewPort.fovPan(20, 30);
            expect(viewPort.worldPos2CanvasPos([10, 10])).toEqual([10, 10]);
            expect(viewPort.canvasPos2WorldPos([10, 10])).toEqual([10, 10]);

            expect(viewPort.fov).toEqual([0, 0, 400, 300]);

            // Zoom in
            viewPort.fovZoom(1.0);
            expect(viewPort.worldPos2CanvasPos([200, 150])).toEqual([200, 150]);
            expect(viewPort.worldPos2CanvasPos([220, 130])).toEqual([210, 140]);

            // Zoom out
            viewPort.fovZoom(-0.5);
            expect(viewPort.worldPos2CanvasPos([10, 10])).toEqual([10, 10]);

        });

        it("View port test 2: Invalid constructor arguments", function (ViewPort) {
            expect(function() {
                new ViewPort();
            }).toThrow(new Error("Incorrect input options to ViewPort constructor"));

            expect(function() {
                new ViewPort({
                    width: "foo"
                });
            }).toThrow(new Error("Incorrect input options to ViewPort constructor"));


            expect(function() {
                new ViewPort({
                    width: 400
                });
            }).toThrow(new Error("Incorrect input options to ViewPort constructor"));

        });

    });

});