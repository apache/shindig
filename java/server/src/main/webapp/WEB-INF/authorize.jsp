<%@ page import="net.oauth.OAuthConsumer" %>
<%@ page import="org.apache.shindig.social.opensocial.oauth.OAuthEntry" %>
<%@ page import="org.apache.shindig.social.opensocial.oauth.OAuthDataStore" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%
  // Gather data passed in to us.
  OAuthConsumer consumer = (OAuthConsumer)request.getAttribute("CONSUMER");
  OAuthEntry entry = (OAuthEntry) request.getAttribute("OAUTH_ENTRY");
  OAuthDataStore dataStore = (OAuthDataStore) request.getAttribute("OAUTH_DATASTORE");

  String appDesc = (String)consumer.getProperty("description");
  if (appDesc == null)
    appDesc = consumer.consumerKey;

  String token = (String)request.getAttribute("TOKEN");
  String callback = (String)request.getAttribute("CALLBACK");

  if (request.getParameter("userId") != null) {
    // User posted the form with the user_id setting.  Let's mark the token authorized and redirect back
    // This is ugly and insecure.  A production form would perform
    // proper authentication and use the container provided user id.
    dataStore.authorizeToken(entry, request.getParameter("userId"));
    response.sendRedirect("/oauth/authorize?oauth_token=" + token + "&oauth_callback=" + callback);
  }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>Your Friendly OAuth Provider</title>
</head>
<body>

<h3>"<%=appDesc%>" is trying to access your information.</h3>

Enter the userId you want to be known as:
<form name="authZForm" action="authorize" method="POST">
  <input type="text" name="userId" value="" size="20"/><br>
  <input type="hidden" name="oauth_token" value="<%= token %>"/>
  <input type="hidden" name="oauth_callback" value="<%= callback %>"/>
  <input type="submit" name="Authorize" value="Authorize"/>
</form>

</body>
</html>
</html>