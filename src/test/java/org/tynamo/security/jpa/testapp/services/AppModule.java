package org.tynamo.security.jpa.testapp.services;

import java.sql.SQLException;

import org.apache.shiro.realm.Realm;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.tynamo.security.jpa.EntitySecurityException;
import org.tynamo.security.jpa.JpaSecurityModule;
import org.tynamo.security.jpa.testapp.entities.AdminOnly;
import org.tynamo.security.jpa.testapp.entities.MyData;
import org.tynamo.security.jpa.testapp.entities.User;
import org.tynamo.security.jpa.testapp.pages.NoPermissions;
import org.tynamo.shiro.extension.realm.text.ExtendedPropertiesRealm;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to configure and extend Tapestry, or to
 * place your own service definitions.
 */
@SubModule(JpaSecurityModule.class)
public class AppModule {

	public static void bind(ServiceBinder binder) {
	}

	public static void contributeApplicationDefaults(MappedConfiguration<String, String> configuration) {
		configuration.add(SymbolConstants.PRODUCTION_MODE, "false");
		configuration.add(SymbolConstants.APPLICATION_VERSION, "0.0.1-SNAPSHOT");
	}

	public static void contributeWebSecurityManager(Configuration<Realm> configuration) {
		ExtendedPropertiesRealm realm = new ExtendedPropertiesRealm("classpath:users.properties");
		configuration.add(realm);
	}

	public static void contributeSeedEntity(OrderedConfiguration<Object> configuration) {
		User user = new User();
		user.setId("user");
		configuration.add("user", user);

		AdminOnly adminOnly = new AdminOnly();
		configuration.add("adminOnly", adminOnly);

		MyData myData = new MyData();
		myData.setOwner(user);
		configuration.add("myData", myData);
	}

	public void contributeRequestExceptionHandler(MappedConfiguration<Class, Object> configuration) {
		configuration.add(EntitySecurityException.class, NoPermissions.class);
	}

	@Startup
	public static void startH2WebServer(@Symbol(SymbolConstants.PRODUCTION_MODE) boolean productionMode)
		throws SQLException {
		if (!productionMode)
			org.h2.tools.Server.createWebServer(new String[] { "-web", "-webAllowOthers", "-webPort", "8082" }).start();
	}

}
