define(["limited-stack"], function(LimitedStack) {
    // Constructor function
    var StateStack = function(capacity) {
        // Input sanity check
        if (typeof capacity !== "number" || capacity < 0) {
            throw "Invalid capacity";
        }

        var self = this;

        this.limitedStack = new LimitedStack(capacity);
        this.stackPointer = -1;

        this.capacity = capacity;

        this.push = function(state) {
            /* Pop out all states above the current stack pointer */
            if ( !self.limitedStack.isEmpty() ) {
                var nToPop = self.limitedStack.size() - 1 - self.stackPointer;

                for (var i = 0; i < nToPop; ++i) {
                    self.limitedStack.pop();
                }
            }

            self.limitedStack.push(state);

            self.stackPointer = self.limitedStack.size() - 1;
        };

        this.canUndo = function() {
            return self.limitedStack.size() > 0 && self.stackPointer >= 0;
        };

        this.canRedo = function() {
            return self.limitedStack.size() > 0 && self.stackPointer < self.limitedStack.size() - 1;
        };

        this.undo = function() {
            if (self.canUndo()) {
                self.stackPointer --;
            } else {
                throw new Error("No state to undo");
            }
        };

        this.redo = function() {
            if (self.canRedo()) {
                self.stackPointer ++;
            } else {
                throw new Error("No state to redo");
            }
        };

        this.getLastState = function() {
            return self.limitedStack.get(self.stackPointer);
        };

        this.getCapacity = function() {
            return self.capacity;
        };

        this.isEmpty = function() {
            return self.limitedStack.isEmpty();
        }

    };

    return StateStack;
});