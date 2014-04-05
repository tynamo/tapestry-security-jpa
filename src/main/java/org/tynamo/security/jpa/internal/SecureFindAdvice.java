package org.tynamo.security.jpa.internal;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.jpa.annotations.RequiresRole;
import org.tynamo.security.services.SecurityService;

@Deprecated
public class SecureFindAdvice implements MethodAdvice {
	private SecurityService securityService;
	private HttpServletRequest request;

	public SecureFindAdvice(final SecurityService securityService, HttpServletRequest request) {
		this.securityService = securityService;
		this.request = request;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void advise(MethodInvocation invocation) {
		Class aClass = (Class) invocation.getParameter(0);
		RequiresRole requiresRole = (RequiresRole) aClass.getAnnotation(RequiresRole.class);
		String requiredRoleValue = RequiresAnnotationUtil.getRequiredRole(aClass, Operation.READ);

		if (requiredRoleValue != null && request.isUserInRole(requiredRoleValue)) {
			invocation.proceed();
			return;
		}

		String requiredAssociationValue = RequiresAnnotationUtil.getRequiredAssociation(aClass, Operation.READ);

		if (requiredAssociationValue == null) {
			// proceed as normal if there's neither RequiresRole nor RequiresAssociation, directly return null if role didn't match
			if (requiredRoleValue == null) invocation.proceed();
			else invocation.setReturnValue(null);
			return;
		}
		EntityManager entityManager = (EntityManager) invocation.getInstance();
		// FIXME handle empty value, i.e. association to "self"
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> criteriaQuery = builder.createQuery();
		Root<?> from = criteriaQuery.from(aClass);
		CriteriaQuery<Object> select = criteriaQuery.select(from);
		Metamodel metamodel = entityManager.getMetamodel();

		EntityType entityType = metamodel.entity(aClass);
		Type idType;
		SingularAttribute idAttr;

		Object entityId = invocation.getParameter(1);
		Predicate predicate1 = null;
		if (entityId != null) {
			idType = entityType.getIdType();
			idAttr = entityType.getId(idType.getJavaType());
			predicate1 = builder.equal(from.get(idAttr.getName()), invocation.getParameter(1));
		}

		String[] associationAttributes = String.valueOf(requiredAssociationValue).split("\\.");
		Path<?> path = null;
		// find the type of the top entity while traversing the property path
		for (String attributeName : associationAttributes) {
			path = path == null ? from.get(attributeName) : path.get(attributeName);
			Attribute attribute = entityType.getAttribute(attributeName);
			// TODO handle if !attribute.isAssociation()
			entityType = metamodel.entity(attribute.getJavaType());
		}

		idType = entityType.getIdType();
		idAttr = entityType.getId(idType.getJavaType());

		// TODO handle subject == null
		// TODO allow configuring the principal for it rather than using primary
		Predicate predicate2 = builder.equal(path.get(idAttr.getName()), securityService.getSubject().getPrincipals()
			.getPrimaryPrincipal());
		criteriaQuery.where(predicate1 == null ? predicate2 : builder.and(predicate1, predicate2));
		invocation.setReturnValue(entityManager.createQuery(criteriaQuery).getSingleResult());
	}
}
