define([], function() {
    // Constructor
    var LimitedStack = function(capacity) {
        if (typeof capacity !== "number" || capacity < 0) {
            throw "Invalid capacity";
        }

        var self = this;

        this.capacity = capacity;
        this.stack = new Array();

        this.push = function(obj) {
            self.stack.push(obj);

            if (self.stack.length > self.capacity) {
                self.stack = self.stack.slice(1, self.stack.length);
            }
        };

        this.pop = function() {
            return self.stack.pop();
        };

        this.peek = function() {
            return self.stack[self.stack.length - 1];
        };

        this.get = function(idx) {
            return self.stack[idx];
        };

        this.isEmpty = function() {
            return this.stack.length == 0;
        };

        this.getCapacity = function() {
            return self.capacity;
        };

        this.size = function() {
            return self.stack.length;
        };

    };

    return LimitedStack;
});