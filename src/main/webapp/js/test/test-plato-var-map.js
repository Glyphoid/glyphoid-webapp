/* Unit test for state-stack */
define(["jasq"], function (jasq) {
    "use strict";

    describe("Tests for Plato Variable Map", "plato-var-map", function () {

        it("Test getVarDisplayName", function (PlatoVarMap) {
            var platoVarMap = new PlatoVarMap({
                tableId        : "varMapTable",
                headerId       : "varMapTableHeader"
            });

            expect(platoVarMap.getVarDisplayName("gr_pi")).toBe("π");
            expect(platoVarMap.getVarDisplayName("gr_Pi")).toBe("Π");

            expect(platoVarMap.getVarDisplayName("A_1")).toBe("A<sub>1</sub>");
            expect(platoVarMap.getVarDisplayName("A_123")).toBe("A<sub>123</sub>");
            expect(platoVarMap.getVarDisplayName("A_a")).toBe("A<sub>a</sub>");

            expect(platoVarMap.getVarDisplayName("gr_Ga_0")).toBe("Γ<sub>0</sub>");
            expect(platoVarMap.getVarDisplayName("gr_Ps_B")).toBe("Ψ<sub>B</sub>");

            expect(platoVarMap.getVarDisplayName("gr_Ga_gr_al")).toBe("Γ<sub>α</sub>");
        });


    });

});
