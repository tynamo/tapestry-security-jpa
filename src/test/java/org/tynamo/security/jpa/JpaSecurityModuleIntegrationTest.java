package org.tynamo.security.jpa;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;

import org.eclipse.jetty.webapp.WebAppContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.tynamo.test.AbstractContainerTest;

import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JpaSecurityModuleIntegrationTest extends AbstractContainerTest
{

	private static final String STATUS_NOT_AUTH = "STATUS[Not Authenticated]";
	private static final String STATUS_AUTH = "STATUS[Authenticated]";
	private HtmlPage page;

	private static String APP_HOST_PORT;
	private static String APP_CONTEXT;

	@BeforeClass
	public void configureWebClient()
	{
		APP_HOST_PORT = "http://localhost:" + port;
		APP_CONTEXT = "/test/";
		BASEURI = APP_HOST_PORT + APP_CONTEXT;
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
	}

	@Override
	public WebAppContext buildContext()
	{
		WebAppContext context = new WebAppContext("src/test/webapp", "/test");
		/*
		 * Sets the classloading model for the context to avoid an strange "ClassNotFoundException: org.slf4j.Logger"
		 */
		context.setParentLoaderPriority(true);
		return context;
	}

	@BeforeMethod
	public void openIndexAsGuest() throws Exception {
		openBase();
		if (isElementPresent("tynamoLogoutLink")) clickOnLinkById("tynamoLogoutLink");
	}

	@Test
	public void updateMyData() throws Exception {
		clickOnLinkById("signinasuser");
		clickOnLinkById("updatemydata");
	}

	@Test
	public void insertAdminOnlyAsGuest() throws Exception {
		clickOnLinkById("insertAdminOnly");
		assertTrue(getTitle().contains("no permissions"));
	}

	@Test
	public void insertAdminOnlyAsUser() throws Exception {
		clickOnLinkById("signinasuser");
		clickOnLinkById("insertAdminOnly");
		assertTrue(getTitle().contains("no permissions"));
	}



	private String getAttribute(String id, String attr)
	{
		return page.getElementById(id).getAttribute(attr);
	}

	private String getText(String id)
	{
		return page.getElementById(id).asText();
	}

	protected void assertLoginPage()
	{
		assertNotNull(page.getElementById("tynamoLogin"), "Page doesn't contain login field. Not a login page.");
		assertEquals("password", getAttribute("tynamoPassword", "type"),
				"Page doesn't contain password field. Not a login page.");
		assertEquals("checkbox", getAttribute("tynamoRememberMe", "type"),
				"Page doesn't contain rememberMe field. Not a login page.");

		assertNotNull(page.getElementById("tynamoEnter"), "Page doesn't contain login form submit button. Not a login page.");
	}

	protected void assertUnauthorizedPage()
	{
		assertEquals(getTitle(), "Unauthorized", "Not Unauthorized page");
	}

	protected void assertUnauthorizedPage401()
	{
		assertEquals(getTitle(), "Error 401 Unauthorized", "Not Unauthorized page");
	}

	protected void openPage(String url) throws Exception
	{
		page = webClient.getPage(BASEURI + url);
	}

	protected void openBase() throws Exception
	{
		openPage("");
	}

	protected void clickOnBasePage(String elementId) throws Exception
	{
		openBase();
		clickOnLinkById(elementId);
	}

	protected void clickOnLinkById(String elementId) throws Exception
	{
		page = page.getHtmlElementById(elementId).click();
	}

//	@Test(groups = {"notLoggedIn"})
//	public void testInterceptServiceMethodDeny() throws Exception
//	{
//		clickOnBasePage("alphaServiceInvoke");
//		assertLoginPage();
//	}
//
//	@Test(dependsOnGroups = {"notLoggedIn"})
//	public void testLoginClick() throws Exception
//	{
//		clickOnBasePage("tynamoLoginLink");
//		assertLoginPage();
//	}


	private void type(String id, String value)
	{
		page.getForms().get(0).<HtmlInput>getInputByName(id).setValueAttribute(value);
	}

	private void click(String id) throws IOException
	{
		page = clickButton(page, id);
	}

	protected void assertAuthenticated()
	{
		assertEquals(getText("status"), STATUS_AUTH);
	}

	protected void assertNotAuthenticated()
	{
		assertEquals(getText("status"), STATUS_NOT_AUTH);
	}

	private boolean isElementPresent(String id)
	{
		return page.getElementById(id) != null;
	}


	private String getTitle()
	{
		return page.getTitleText();
	}

	private String getLocation()
	{
		return page.getWebResponse().getWebRequest().getUrl().toString();
	}

}
