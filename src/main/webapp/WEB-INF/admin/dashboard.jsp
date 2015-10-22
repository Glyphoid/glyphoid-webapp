<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--<%@ taglib prefix="tg" tagdir="/WEB-INF/tags" %>--%>
<html>
<head>
    <title>Plato Server Dashboard</title>
    <c:set var="contextPath" value="${pageContext.request.contextPath}"/>
    <script type="text/javascript">
        function selectSessions(name) {
            var checkboxes = document.getElementsByName(name);
            for (var i = 0; i < checkboxes.length; i++) {
                checkboxes[i].checked = checkboxes[i].checked ? false : true;
            }
        }


    </script>

    <link rel="stylesheet" href="${contextPath}/res/css/table.css"/>
    <link rel="stylesheet" href="${contextPath}/res/css/common.css"/>
</head>
<body>



<!--<div style="float:left;"><img src="${contextPath}/res/bg_global_logo_small.jpg"></div>-->
<div style="float:left;">
    <div id="logo" style="float:left; vertical-align: middle; width: 100%">
        <div style="float:left;padding-top: 8px; text-align: center; vertical-align: middle; font-size: 30px;">
            <span>Plato Server</span>
        </div>
    </div>
</div>
<div style="float:right;padding-top: 20px; font-size: 16px;">Welcome,
    <b>${pageContext.request.userPrincipal.name}<b> | <a href="${contextPath}/j_spring_security_logout"> Logout</a></b>
</div>
<p><br/><br/>
<%--<form:form commandName="adminDashboardModel" action="${contextPath}/admin">--%>
<%--<form>--%>

    <div class="subsection">
        <span>Worker Pool Status</span>
    </div>
    <div style="float:left; width: 100%">
        <table class="tablebody" style="width: 100%">
            <tr>
                <td class="cell" style="width: 15%">Number of active workers</td>
                <td class="cell" style="width: 85%">
                    <input id="workerPool.currNumWorkers" name="workerPool.currNumWorkers"
                           value="${adminDashboardModel.currNumWorkers}"
                           type="text" size="40" disabled="disabled"/>
                </td>
            </tr>
            <tr>
                <td class="cell" style="width: 15%;">Number of workers ever created</td>
                <td class="cell" style="width: 85%">
                    <input id="workerPool.numEverCreatedWorkers" name="workerPool.numEverCreatedWorkers"
                           value="${adminDashboardModel.numEverCreatedWorkers}"
                           type="text" size="40" disabled="disabled"/>
                </td>
            </tr>
            <tr>
                <td class="cell" style="width: 15%;">Number of workers that have been purged</td>
                <td class="cell" style="width: 85%">
                    <input id="workerPool.numPurgedWorkers" name="workerPool.numPurgedWorkers"
                           value="${adminDashboardModel.numPurgedWorkers}"
                           type="text" size="40" disabled="disabled"/>
                </td>
            </tr>
            <tr>
                <td class="cell" style="width: 15%;">Number of workers that have been removed normally</td>
                <td class="cell" style="width: 85%">
                    <input id="workerPool.numNormallyRemovedWorkers" name="workerPool.numNormallyRemovedWorkers"
                           value="${adminDashboardModel.numNormallyRemovedWorkers}"
                           type="text" size="40" disabled="disabled"/>
                </td>
            </tr>

            <tr>
                <td class="cell" style="width: 200px;">List of workers</td>
                <td class="cell" style="width: 800px;">
                    <table class="workerTableBody" id="workerList">
                        <tr>
                            <th>Worker ID</th>
                            <th>Client IP address</th>
                            <th>Client host name</th>
                            <th>Client type</th>
                            <th>Platform version</th>
                            <th>App version</th>
                            <th>Custom client data</th>
                            <th>Previous worker ID</th>
                            <th>Time since creation </th>
                            <th>Time since last use</th>
                            <th>Message count</th>
                            <th>Current average message rate (s<sup>-1</sup>)</th>
                            <th>Effective average message rate (s<sup>-1</sup>)</th>
                            <th>Remove</th>
                        </tr>
                        <c:forEach var="workerAndClientInfo" items="${adminDashboardModel.workersAndClientsInfo}">
                            <tr>
                                <td>${workerAndClientInfo.workerID}</td>
                                <td>${workerAndClientInfo.workerClientInfo.clientIPAddress}</td>
                                <td>${workerAndClientInfo.workerClientInfo.clientHostName}</td>
                                <td>${workerAndClientInfo.clientType}</td>
                                <td>${workerAndClientInfo.workerClientInfo.clientPlatformVersion}</td>
                                <td>${workerAndClientInfo.workerClientInfo.clientAppVersion}</td>
                                <td>${workerAndClientInfo.customClientData}</td>
                                <td>${workerAndClientInfo.prevWorkerID}</td>>
                                <td>${workerAndClientInfo.timeSinceCreation}</td>
                                <td>${workerAndClientInfo.timeSinceLastUse}</td>
                                <td>${workerAndClientInfo.messageCount}</td>
                                <td>${workerAndClientInfo.currentAverageMessageRate}</td>
                                <td>${workerAndClientInfo.effectiveAverageMessageRate}</td>
                                <td><button class="removeWorkerButton" id="remove_${workerAndClientInfo.workerID}">Remove</button></td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
            </tr>
        </table>
    </div>

    <div class="subsection">
        <span>Worker Pool Settings</span>
    </div>
    <div style="float:left; width: 100%">
        <table class="tablebody" style="width: 600px">
            <tr>
                <td class="cell" style="width: 250px;">Maximum allowed number of workers</td>
                <td class="cell" style="width: 350px">
                    <input id="workerPool_maxNumWorkers" name="workerPool.maxNumWorkers"
                           value="${adminDashboardModel.maxNumWorkers}"
                           type="text" size="40" />
                </td>
            </tr>
            <tr>
                <td class="cell" style="width: 250px;">Worker timeout (ms)</td>
                <td class="cell" style="width: 350px">
                    <input id="workerPool_workerTimeoutMillis" name="workerPool.workerTimeoutMillis"
                           value="${adminDashboardModel.workerTimeoutMillis}"
                           type="text" size="40" />
                </td>
            </tr>

            <tr>
                <td class="cell" style="width: 250px;">Admin username</td>
                <td class="cell" style="width: 350px">
                    <input id="admin_username" name="admin.username"
                           type="text" size="40" />
                </td>
            </tr>
            <tr>
                <td class="cell" style="width: 250px;">Admin password</td>
                <td class="cell" style="width: 350px">
                    <input id="admin_password" name="admin.password"
                           type="password" size="40" />
                </td>
            </tr>
        </table>
    </div>
    <div style="float:left; width: 100%; padding-top: 20px">
        <button id="workerPoolSettingSubmit">Submit</button>
        <%--<input id="workerPoolSettingSubmit" type="submit" class="button" name="update_worker_pool_settings" value="Update worker pool settings"/>--%>
    </div>
<%--</form>--%>
<%--</form:form>--%>

<script type="text/javascript" src="js/libs/jquery-1.11.1.min.js"></script>
<script type="text/javascript">
    $(function() {

        $("#workerPoolSettingSubmit").on("click", function(e) {
            e.preventDefault();

            var maxNumWorkers = Number($("#workerPool_maxNumWorkers").val())
            var workerTimeoutMillis = Number($("#workerPool_workerTimeoutMillis").val());
            var username = $("#admin_username").val();
            var password = $("#admin_password").val();

            var reqBody = {
                username            : username,
                password            : password,
                maxNumWorkers       : maxNumWorkers,
                workerTimeoutMillis : workerTimeoutMillis
            };

            $.ajax({
                url    : "${contextPath}/config",
                method : "POST",
                data   : JSON.stringify(reqBody),
                complete : function(resp) {
                    if (resp.status === 200) {
                        console.log("Update succeeded: ", resp.responseJSON)
                    }
                    else {
                        console.log("Update failed: ", resp.responseJSON)
                    }
                }
            });

            console.log("Submitting worker pool settings");
        });

        $(".removeWorkerButton").on("click", function(event) {
            console.log("removeWorkerButton clicked");

            event.preventDefault();

            var workerId = event.target.id.replace("remove_", "");

            var removeReqData = {
                action     : "remove-engine",
                engineUuid : workerId
            };

            $.ajax({
                url    : "${contextPath}/handwriting",
                method : "POST",
                data   : JSON.stringify(removeReqData),
                complete : function(resp) {
                    if (resp.status === 200 &&
                        typeof resp.responseJSON === "object" &&
                        typeof resp.responseJSON.errors === "object" &&
                        resp.responseJSON.errors.length === 0 &&
                        typeof resp.responseJSON.removedEngineUuid === "string" &&
                        workerId === resp.responseJSON.removedEngineUuid) {
                        console.log("remove-worker request succeeded: ", resp.responseJSON);
                        window.location.reload();
                    } else {
                        console.log("remove-worker request failed: ", resp.responseJSON);
                    }
                }

            });
        });
    });


</script>

</body>
</html>
