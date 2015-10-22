define(["jquery"], function($) {
    var MathJaxClient = function(options) {
        var self = this;

        this.options = {
            mathJaxEndpointURL    : "http://platonotes.org/math-jax/math-jax",
            mathJaxEndpointMethod : "POST",
            outputFormatNames     : {
                MathML  : "MathML",
                PNG     : "png"
            }
        };


        /* Public interface */

        /**
         * Convert Math TeX to MathML
         * @param      tex: Math TeX
         * @param      successCallback(responseJSON): success callback
         * @param      errorCallback(errorMsg): error callback
         */
        this.tex2mml = function(tex, successCallback, errorCallback) {
            self.tex2x(self.options.outputFormatNames.MathML, tex, successCallback, errorCallback);

        };

        /**
         * Covert Math Tex to PNG image, base-64 encoded
         * @param tex: Math Tex
         * @param successCallback(responseJSON)
         * @param errorCallback(errorMsg)
         */
        this.tex2png = function(tex, successCallback, errorCallback) {
            var additionalData = {
                "imageWidth": 400,
                "imageDpi" : 800
            };

            self.tex2x(self.options.outputFormatNames.PNG, tex, successCallback, errorCallback);

        };

        /* Private functions */
        this.tex2x = function(format, tex, successCallback, errorCallback, additionalData) {
            /* Input sanity check */
            self.checkTexInput(tex);

            var reqBody = {
                "mathTex"     : tex,
                "imageFormat" : format
            };

            if (typeof additionalData === "object" && additionalData !== null) {
                $.extend(reqBody, additionalData);
            }

            $.ajax({
                url    : self.options.mathJaxEndpointURL,
                method : self.options.mathJaxEndpointMethod,
                data   : JSON.stringify(reqBody),
                complete : function(resp) {
                    if (resp.status === 200) {
                        if (typeof successCallback === "function") {
                            successCallback(resp.responseJSON);
                        }
                    } else {
                        if (typeof errorCallback === "function") {
                            errorCallback();
                        }
                    }
                }
            });
        };

        this.checkTexInput = function(tex) {
            /* Input sanity check */
            if (typeof tex !== "string") {
                throw "Invalid input Math TeX"
            }
        };
    };

    return MathJaxClient;
});