<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<head>
    <c:set var="contextPath" value="${pageContext.request.contextPath}"/>

    <title>Glyphoid Login</title>

    <link rel="stylesheet" href="${contextPath}/res/css/common.css"/>

    <style>
        body {
            font-size: 120%;
            align-items: center;
        }
    </style>
</head>
<body>
<div style="position:absolute; left: 20%; width:60%; height: 50%; top:40%">
    <div style="float:left;">
    <div id="logo" style="float:left; vertical-align: middle; width: 100%">
        <div style="float:left;padding-top: 8px; text-align: center; vertical-align: middle; font-size: 30px;">
            <img style="width:32px; height:32px; vertical-align:middle;" src="res/images/glyphoid_32x32.ico" />
            <span style="font-weight:bold;">Glyphoid: </span>
            <span>Handwritten Formula Parser and Calculator</span>
            <div style="font-size:75%">(Internal testing)</div>
            <div style="font-size:75%">(C) 2015 Shanqing Cai</div>
        </div>

    </div>

    <div style="clear:both; width: 100%; padding-top:5px; padding-left: 10px; margin-left: 10px">
        <form action="<c:url value='/j_spring_security_check' />" method="post">
            <table>
                <tr>
                    <td style="font-size: 18px"><b>Login: </b></td>
                    <td>
                        <input type='text' name="j_username" size="40"/>
                    </td>
                </tr>
                <tr>
                    <td style="font-size: 18px"><b>Password: </b></td>
                    <td>
                        <input type='password' name='j_password' size="40"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <div style="float: right">
                            <input type="submit" class="btn" value="Login"/>
                        </div>
                    </td>
                </tr>
            </table>
            <br/>
            <c:if test="${!empty sessionScope['SPRING_SECURITY_LAST_EXCEPTION'].message}">
                <div class="error_message">
                    Your login attempt is failed<br/>
                    Reason: ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
                </div>
            </c:if>
        </form>
    </div>
    </div>
</div>

</body>
</html>
