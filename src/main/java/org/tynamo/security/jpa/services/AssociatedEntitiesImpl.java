package org.tynamo.security.jpa.services;

import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.tynamo.security.jpa.JpaSecuritySymbols;
import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.jpa.internal.RequiresAnnotationUtil;
import org.tynamo.security.jpa.internal.SecureFinder;
import org.tynamo.security.services.SecurityService;

public class AssociatedEntitiesImpl implements AssociatedEntities {
	private final EntityManager delegate;
	private final SecurityService securityService;
	private final HttpServletRequest request;
	private String realmName;
	private Class<?> principalType;

	public AssociatedEntitiesImpl(
		final SecurityService securityService,
		final HttpServletRequest request, final EntityManager delegate,
		@Inject @Symbol(JpaSecuritySymbols.ASSOCIATED_REALM) String realmName,
		@Inject @Symbol(JpaSecuritySymbols.ASSOCIATED_PRINCIPALTYPE) String principalType) throws ClassNotFoundException {
		this.securityService = securityService;
		this.request = request;
		this.delegate = delegate;
		this.realmName = realmName;
		this.principalType = principalType.isEmpty() ? null : Class.forName(principalType);
	}

	@Override
	public <T> List<T> findAll(EntityManager em, Class<T> entityClass) throws Exception {
		String requiredAssociationValue = RequiresAnnotationUtil.getRequiredAssociation(entityClass, Operation.READ);

		// return SecureFinder.find(delegate, request, SecureFinder.getConfiguredPrincipal(securityService,
		// realmName,
		// principalType),
		// requiredAssociationValue, null, entityClass,
		// null, null, null);
		
		Object principal = SecureFinder.getConfiguredPrincipal(securityService, realmName, principalType);
		// invoke with security disabled so we don't go to a recursion with em.find()
		return securityService.invokeWithSecurityDisabled(new Callable<List<T>>() {
			@Override
			public List<T> call() {
				return SecureFinder.find(delegate, request, principal,
					requiredAssociationValue,
					null, entityClass, null, null, null);
			}
		});
		
	}

}
