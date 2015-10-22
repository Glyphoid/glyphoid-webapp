/* Unit test for limited-stack */
define(["jasq"], function (jasq) {
    "use strict";

    describe("Tests for limited-stack", "limited-stack", function () {

        it("Limited stack test 1", function (LimitedStack) {
            var limitedStack = new LimitedStack(3);

            expect(limitedStack.getCapacity()).toBe(3);
            expect(limitedStack.size()).toBe(0);
            expect(limitedStack.isEmpty()).toBe(true);

            for (var i = 0; i < 3; ++i) {
                limitedStack.push((i + 1) * 10);
                expect(limitedStack.getCapacity()).toBe(3);
                expect(limitedStack.size()).toBe(i + 1);

            }

            expect(limitedStack.get(0)).toBe(10);
            expect(limitedStack.get(1)).toBe(20);
            expect(limitedStack.get(2)).toBe(30);

            // Over capacity
            limitedStack.push(40);
            expect(limitedStack.getCapacity()).toBe(3);
            expect(limitedStack.size()).toBe(3);

            expect(limitedStack.get(0)).toBe(20);
            expect(limitedStack.get(1)).toBe(30);
            expect(limitedStack.get(2)).toBe(40);
            expect(limitedStack.isEmpty()).toBe(false);

            // Popping
            limitedStack.pop();
            expect(limitedStack.getCapacity()).toBe(3);
            expect(limitedStack.size()).toBe(2);
            expect(limitedStack.isEmpty()).toBe(false);
            expect(limitedStack.get(0)).toBe(20);
            expect(limitedStack.get(1)).toBe(30);
            expect(limitedStack.get(2)).toBe(undefined);

            limitedStack.pop();
            expect(limitedStack.getCapacity()).toBe(3);
            expect(limitedStack.size()).toBe(1);
            expect(limitedStack.isEmpty()).toBe(false);
            expect(limitedStack.get(0)).toBe(20);
            expect(limitedStack.get(1)).toBe(undefined);
            expect(limitedStack.get(2)).toBe(undefined);

            limitedStack.pop();
            expect(limitedStack.getCapacity()).toBe(3);
            expect(limitedStack.size()).toBe(0);
            expect(limitedStack.isEmpty()).toBe(true);
            expect(limitedStack.get(0)).toBe(undefined);
            expect(limitedStack.get(1)).toBe(undefined);
            expect(limitedStack.get(2)).toBe(undefined);

        });

  });

});
