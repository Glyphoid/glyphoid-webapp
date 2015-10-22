define([], function() {
    var AwsHelper = function (config) {
        var self = this;

        this.config = config;

        AWS.config.update({
            accessKeyId: self.config.accessKey,
            secretAccessKey: self.config.secretKey,
            region: self.config.region
        });

        this.s3Client = new AWS.S3({
            apiVersion: '2006-03-01',
            region: self.config.region
        });

        this.sessions = [];
        this.sessionID2Prefix = {};

        this.getSessionPrefixes = function (datePrefix, clientType, successCallback) {
            self.sessions = [];
            self.sessionID2Prefix = {};

            var objectPrefix = clientType + "_" + datePrefix + "T";

            var listObjectsParams = {
                Bucket: self.config.bucketName,
                MaxKeys: 10,
                Prefix: objectPrefix
            };

            self.getSessionPrefixesInner(listObjectsParams, null, successCallback);
        };

        this.getSessionPrefixesInner = function (listObjectParams, marker, successCallback) {
            var realListObjectParams = listObjectParams;

            if (typeof marker == "string") {
                realListObjectParams.Marker = marker;
            }

            self.s3Client.listObjects(realListObjectParams, function (err, data) {
                if (err) {
                    console.error(err, err.stack);
                    throw new Error("Failed to get session prefixes");
                } else {
                    for (var i = 0; i < data.Contents.length; ++i) {
                        var key = data.Contents[i].Key;
                        var keyInfo = {};
                        keyInfo = self.parseKeyString(key);

                        if (keyInfo.action === "create-engine") {
                            self.sessions.push(keyInfo);
                            self.sessionID2Prefix[keyInfo.sessionID] = keyInfo.sessionPrefix;

                            console.log("Added session: \"" + keyInfo.sessionID + "\"");
                        }
                    }

                    if (data.IsTruncated) {
                        // Recursive call
                        self.getSessionPrefixesInner(listObjectParams, data.Contents[data.Contents.length - 1].Key, successCallback);
                    } else {
                        successCallback(self.sessions);
                    }
                }
            });
        };

        this.sessionData = [];
        this.actionID2ObjectKey = {};

        this.actionCounter = 0;

        this.getSessionData = function (sessionPrefix, successCallback) {
            self.sessionData = [];
            self.actionID2ObjectKey = [];

            self.actionCounter = 0;
            self.getSessionDataInner(sessionPrefix, null, successCallback);
        };

        this.getSessionDataInner = function (sessionPrefix, marker, successCallback) {
            var listObjectsParams = {
                Bucket: self.config.bucketName,
                MaxKeys: 10,
                Prefix: sessionPrefix
            };

            if (typeof marker === "string") {
                listObjectsParams.Marker = marker;
            }

            self.s3Client.listObjects(listObjectsParams, function (err, data) {
                if (err) {
                    console.error(err, err.stack);
                    throw new Error("Failed to get session data");
                } else {
                    for (var i = 0; i < data.Contents.length; ++i) {
                        var key = data.Contents[i].Key;
                        var keyInfo = self.parseKeyString(key);

                        keyInfo.actionID = "" + (self.actionCounter++) + "_" + keyInfo.action;

                        self.actionID2ObjectKey[keyInfo.actionID] = keyInfo.objectKey;

                        self.sessionData.push(keyInfo);
                        // TODO: sessionData.messageBodies.push();
                    }

                    if (data.IsTruncated) {
                        // Recursive call
                        self.getSessionDataInner(sessionPrefix, data.Contents[data.Contents.length - 1].Key, successCallback);
                    } else {
                        successCallback(self.sessionData);
                    }
                }
            });
        };

        this.getActionData = function (actionObjectKey, successCallback) {
            var getObjectParams = {
                Bucket: self.config.bucketName,
                Key: actionObjectKey
            };

            self.s3Client.getObject(getObjectParams, function (err, data) {
                if (err) {
                    console.error(err, err.stack);
                    throw new Error("getActionData failed: ", err, err.stack);
                } else {
                    var jsonString = String.fromCharCode.apply(null, data.Body);
                    var actionData = JSON.parse(jsonString);

                    successCallback(actionData);
                }
            });
        };

        // Helper functions
        this.formatDateISO = function (dateStr) {
            if (dateStr.indexOf("-") !== -1) {
                return (new Date(dateStr)).toISOString().split("T")[0];
            } else {
                return dateStr;
            }
        };

        this.parseKeyString = function (keyStr) {
            var key = keyStr.replace(".json", "");
            var keyItems = key.split("_");
            var keyInfo = {
                clientType: keyItems[0],
                sessionID: keyItems[2],
                sessionStartTimeStamp: new Date(keyItems[1]),
                actionTimeStamp: new Date(keyItems[3]),
                action: keyItems[keyItems.length - 1],
                objectKey: keyStr,
                sessionPrefix: keyItems[0] + "_" + keyItems[1] + "_" + keyItems[2] + "_"
            };



            return keyInfo;
        };

        this.getAdditionalClientData = function(objectKey, successCallback) {
            // Retrieve the content of the object to extract more information
            var getObjectParams = {
                Bucket: self.config.bucketName,
                Key: objectKey
            };

            self.s3Client.getObject(getObjectParams, function (err, data) {
                if (err) {
                    console.error(err, err.stack);
                    throw new Error("Failed to retrieve the object content of " + keyStr);
                } else {
                    var jsonString = String.fromCharCode.apply(null, data.Body);

                    var objectData = JSON.parse(jsonString);
                    if (typeof objectData.AdditionalClientData === "undefined") {
                        throw new Error("Undefined AdditionalClientData field");
                    }

                    var additionalClientData = objectData.AdditionalClientData;

                    if (typeof successCallback === "function") {
                        successCallback(additionalClientData);
                    }
                }
            });
        }


    };

    return AwsHelper;
});