define(["jasq", "jquery"], function (jasq, $) {
    "use strict";

    var trueCanvasW = Number($("#gestureCanvas").attr("width"));
    var trueCanvasH = Number($("#gestureCanvas").attr("height"));

    var mockAllTokenNames = ["root","p","z","gr_rh","b","6","5","]","9","(","8","[","gr_ph","f","g","d","e","c","a","l","m",")","j","k","h","i","X","gr_ga","gr_al","n","*","+","gr_be","gr_de","/","-","3","w","2","tick","v","1","u","t","0","s","7","r","q","4","y","\u003d","x","gr_ep","gr_xi","gr_mu","gr_la","gr_omega","gr_th","D","E","F","G","A","B","L","M","N","H","I","J","K","U","T","W","V","Q","P","R","Y","gr_pi","gr_et","gr_io","gr_si","gr_omicron","gr_Si","gr_Pi","gr_De","gr_La","gr_Ga","gr_Om","integ","gr_Up","gr_Ps","gr_Ph","gr_Th","gr_nu","."];

    // Mockup handwriting engine agent
    var mockHwEngAgent = function() {
        var self = this;
        this.tokens = [];

        this.getNewEngine = function(successCallback, errorCallback) {
            successCallback();
        };

        this.getAllTokenNames = function(successCallback, errorCallback) {
            var mockAllTokenNamesResp = {
                "allTokenNames": mockAllTokenNames,
                "writtenTokenSet": {"tokens":[]},
                "canUndoStrokeCuratorUserAction": false,
                "canRedoStrokeCuratorUserAction": false,
                "constituentStrokes":[],
                "errors":[]
            };

            successCallback(mockAllTokenNamesResp);
        };

        this.getNumTokens = function() {
            return self.tokens.length; // TODO
        };

        this.isLastTokenMerged = function() {
            return false; // TODO
        }
    };

    var getMockTouchManagerOptions = function() {
        return {
            elementId: "gestureCanvas",
            handwritingEngineAgent: new mockHwEngAgent()
        };
    };

    /**
     * Get mock touch start event. Assume the last touch is the one that has newly touched down.
     * @param xs   An array of x coordinates
     * @param ys   An array of y coordinates
     */
    var getMockTouchEvent = function(touchEventType, xs, ys) {
        var touchStartEvent = $.Event(touchEventType);

        var nTouches = xs.length;
        if (ys.length !== nTouches) {
            throw new Error("Mismatch in length between xs and ys");
        }

        touchStartEvent.touches = [];
        touchStartEvent.changedTouches = [];
        for (var i = 0; i < nTouches; ++i) {
            var touch = {
                pageX: xs[i],
                pageY: ys[i]
            };

            touchStartEvent.touches.push(touch);
            if (i == nTouches - 1) {
                touchStartEvent.changedTouches.push(touch);
            }
        }

        return touchStartEvent;
    };

    describe("Tests for Plato Gesture Manager", "plato-gesture-manager", function () {

        it("plato-gesture-manager constructor", function (TouchManager) {
            var touchManager = new TouchManager(getMockTouchManagerOptions());

            // Canvas size initialization
            expect(touchManager.canvasWidth).toBe(trueCanvasW);
            expect(touchManager.canvasHeight).toBe(trueCanvasH);

            // Initial state: touch
            expect(touchManager.touchDown).toBe(false);
            expect(touchManager.pts.length).toBe(0);

            // Initial state: pinch
            expect(touchManager.pinchMode).toBe(false);
            expect(touchManager.hasBeenPinch).toBe(false);

            // Initial state: strokes
            expect(touchManager.strokes.length).toBe(0);
            expect(touchManager.strokeBounds.length).toBe(0);
            expect(touchManager.strokeTokenOwners.length).toBe(0);
            expect(touchManager.constStrokes.length).toBe(0);

            // Initial state: tokens
            expect(touchManager.tokenBounds.length).toBe(0);
            expect(touchManager.tokenNames.length).toBe(0);

            // Initial state: state stack
            expect(touchManager.stateStack.isEmpty()).toBe(true);
            expect(touchManager.stateStack.canUndo()).toBe(false);
            expect(touchManager.stateStack.canRedo()).toBe(false);

            // Initial state: view port
            expect(touchManager.viewPort.canvasWidth).toBe(trueCanvasW);
            expect(touchManager.viewPort.canvasHeight).toBe(trueCanvasH);
            expect(touchManager.viewPort.scale).toBe(1);
            expect(touchManager.viewPort.dx).toBe(0);
            expect(touchManager.viewPort.dy).toBe(0);
            expect(touchManager.viewPort.fov).toEqual([0, 0, trueCanvasW, trueCanvasH]);

            // All tokens
            expect(touchManager.allTokens.length > 0).toBe(true);

        });

        it("plato-gesture-manager pinch zoom: 2 touches", function (TouchManager) {
            var tm = new TouchManager(getMockTouchManagerOptions());

            expect(tm.touchDown).toBe(false);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            var touchStartEvent1 = getMockTouchEvent("touchstart", [0.2 * trueCanvasW], [0.2 * trueCanvasH]);

            // Trigger first touchstart
            tm.el.trigger(touchStartEvent1);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            var touchStartEvent2 = getMockTouchEvent("touchstart",
                [0.2 * trueCanvasW, 0.4 * trueCanvasW], [0.2 * trueCanvasH, 0.4 * trueCanvasH]);

            // Trigger second touchstart
            tm.el.trigger(touchStartEvent2);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);
            expect(tm.viewPort.scale).toBe(1);

            // Mock pinching in
            var touchMoveEvent1 = getMockTouchEvent("touchmove",
                [0.25 * trueCanvasW, 0.35 * trueCanvasW], [0.25 * trueCanvasH, 0.35 * trueCanvasH]);
            tm.el.trigger(touchMoveEvent1);

            expect(tm.viewPort.scale > 1).toBe(true); // Zoomed out

            // First touchend
            var touchUpEvent1 = getMockTouchEvent("touchend",
                [0.25 * trueCanvasW], [0.25 * trueCanvasH]);
            tm.el.trigger(touchUpEvent1);

            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(true);

            // Second touchend
            var touchUpEvent2 = getMockTouchEvent("touchend", [], []);
            tm.el.trigger(touchUpEvent2);

            expect(tm.pinchMode).toBe(false);
            expect(tm.viewPort.scale > 1).toBe(true); // Zoomed stays
        });

        it("plato-gesture-manager pinch pan: 2 touches", function (TouchManager) {
            var tm = new TouchManager(getMockTouchManagerOptions());

            expect(tm.touchDown).toBe(false);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            var touchStartEvent1 = getMockTouchEvent("touchstart", [0.2 * trueCanvasW], [0.2 * trueCanvasH]);

            // Trigger first touchstart
            tm.el.trigger(touchStartEvent1);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            var touchStartEvent2 = getMockTouchEvent("touchstart",
                [0.2 * trueCanvasW, 0.4 * trueCanvasW], [0.2 * trueCanvasH, 0.4 * trueCanvasH]);

            // Trigger second touchstart
            tm.el.trigger(touchStartEvent2);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);
            expect(tm.viewPort.scale).toBe(1);
            expect(tm.viewPort.dx).toBe(0);
            expect(tm.viewPort.dy).toBe(0);

            // Mock pan to the right
            var touchMoveEvent1 = getMockTouchEvent("touchmove",
                [0.3 * trueCanvasW, 0.5 * trueCanvasW], [0.2 * trueCanvasH, 0.4 * trueCanvasH]);
            tm.el.trigger(touchMoveEvent1);

            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);
            expect(tm.viewPort.scale).toBe(1); // No zooming
            expect(tm.viewPort.dx < 0).toBe(true); //FOV panned to the left
            expect(tm.viewPort.dy).toBe(0); // No pan in the y dimension

            // First touchend
            var touchUpEvent1 = getMockTouchEvent("touchend",
                [0.25 * trueCanvasW], [0.25 * trueCanvasH]);
            tm.el.trigger(touchUpEvent1);

            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(true);

            // Second touchend
            var touchUpEvent2 = getMockTouchEvent("touchend", [], []);
            tm.el.trigger(touchUpEvent2);

            expect(tm.pinchMode).toBe(false);
            expect(tm.viewPort.scale).toBe(1); // No zooming
            expect(tm.viewPort.dx < 0).toBe(true); //FOV panned to the left
            expect(tm.viewPort.dy).toBe(0); // No pan in the y dimension
        });

        it("plato-gesture-manager pinch zoom: 3 touches", function (TouchManager) {
            var tm = new TouchManager(getMockTouchManagerOptions());

            expect(tm.touchDown).toBe(false);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            var touchStartEvent1 = getMockTouchEvent("touchstart", [0.2 * trueCanvasW], [0.2 * trueCanvasH]);

            // Trigger first touchstart
            tm.el.trigger(touchStartEvent1);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(false);
            expect(tm.hasBeenPinch).toBe(false);

            // Trigger second touchstart
            var touchStartEvent2 = getMockTouchEvent("touchstart",
                [0.2 * trueCanvasW, 0.4 * trueCanvasW], [0.2 * trueCanvasH, 0.4 * trueCanvasH]);
            tm.el.trigger(touchStartEvent2);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);
            expect(tm.viewPort.scale).toBe(1);

            // Trigger third touchstart
            var touchStartEvent3 = getMockTouchEvent("touchstart",
                [0.2 * trueCanvasW, 0.4 * trueCanvasW, 0.6 * trueCanvasW],
                [0.2 * trueCanvasH, 0.4 * trueCanvasH, 0.6 * trueCanvasH]);
            tm.el.trigger(touchStartEvent3);

            expect(tm.touchDown).toBe(true);
            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);
            expect(tm.viewPort.scale).toBe(1);

            // Mock pinching in
            var touchMoveEvent1 = getMockTouchEvent("touchmove",
                [0.25 * trueCanvasW, 0.35 * trueCanvasW, 0.6 * trueCanvasW],
                [0.25 * trueCanvasH, 0.35 * trueCanvasH, 0.6 * trueCanvasH]);
            tm.el.trigger(touchMoveEvent1);

            expect(tm.viewPort.scale > 1).toBe(true); // Zoomed out

            // First touchend
            var touchUpEvent1 = getMockTouchEvent("touchend",
                [0.25 * trueCanvasW, 0.35 * trueCanvasW],
                [0.25 * trueCanvasH, 0.35 * trueCanvasH]);
            tm.el.trigger(touchUpEvent1);

            expect(tm.pinchMode).toBe(true);
            expect(tm.hasBeenPinch).toBe(true);

            // Second touchend
            var touchUpEvent2 = getMockTouchEvent("touchend", [0.25 * trueCanvasW], [0.25 * trueCanvasH]);
            tm.el.trigger(touchUpEvent2);

            expect(tm.pinchMode).toBe(false);
            expect(tm.viewPort.scale > 1).toBe(true); // Zoomed stays

            // Second touchend
            var touchUpEvent3 = getMockTouchEvent("touchend", [], []);
            tm.el.trigger(touchUpEvent3);

            expect(tm.pinchMode).toBe(false);
            expect(tm.viewPort.scale > 1).toBe(true); // Zoomed stays
        });



    });

});
