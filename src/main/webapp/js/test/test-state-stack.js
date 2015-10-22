/* Unit test for state-stack */
define(["jasq"], function (jasq) {
    "use strict";

    describe("Tests for state-stack", "state-stack", function () {

        it("State stack test 1", function (StateStack) {
            var stateStack = new StateStack(3);

            expect(stateStack.getCapacity()).toBe(3);
            expect(stateStack.isEmpty()).toBe(true);
            expect(stateStack.canUndo()).toBe(false);
            expect(stateStack.canRedo()).toBe(false);

            // Push state 1
            stateStack.push("State 1");

            expect(stateStack.getCapacity()).toBe(3);
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 1");

            // Undo state 1
            stateStack.undo();
            expect(stateStack.getCapacity()).toBe(3);
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(false);
            expect(stateStack.canRedo()).toBe(true);
            expect(stateStack.getLastState()).toBe(undefined);

            // Redo state 1
            stateStack.redo();
            expect(stateStack.getCapacity()).toBe(3);
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 1");

            // Push state 2 and state 3
            stateStack.push("State 2");
            stateStack.push("State 3");
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 3");

            // Invalid attempt to redo
            expect(function() {
                stateStack.redo();
            }).toThrow(new Error("No state to redo"));

            // Undo state 3
            stateStack.undo();

            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(true);
            expect(stateStack.getLastState()).toBe("State 2");

            // Push state 3A
            stateStack.push("State 3A");
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 3A");

            // Push state 4. This should lead to overflow.
            stateStack.push("State 4");
            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 4");

            stateStack.undo();
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(true);
            expect(stateStack.getLastState()).toBe("State 3A");

            stateStack.undo();
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(true);
            expect(stateStack.getLastState()).toBe("State 2");

            stateStack.undo();
            expect(stateStack.canUndo()).toBe(false);
            expect(stateStack.canRedo()).toBe(true);
            expect(stateStack.getLastState()).toBe(undefined);

            // Invalid attempt to undo
            expect(function() {
                stateStack.undo();
            }).toThrow(new Error("No state to undo"));

            // Redo to the top
            stateStack.redo();
            stateStack.redo();
            stateStack.redo();

            expect(stateStack.isEmpty()).toBe(false);
            expect(stateStack.canUndo()).toBe(true);
            expect(stateStack.canRedo()).toBe(false);
            expect(stateStack.getLastState()).toBe("State 4");

        });


    });

});
