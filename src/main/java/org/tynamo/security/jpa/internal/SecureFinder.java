package org.tynamo.security.jpa.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.jpa.annotations.RequiresAssociation;
import org.tynamo.security.services.SecurityService;

/**
 * SecureFinder provides common functionality for finding entities securely. We are advising (the object that creates) EntityManager, but we
 * need to share the implemention with AssociatedEntities service (that provides secure find for all entities of certain types) because
 * EntityManager API doesn't include any operation for findAll().
 *
 */
public class SecureFinder {

	public static Object getConfiguredPrincipal(SecurityService securityService, String realmName, Class principalType) {
		if (securityService.getSubject() == null || securityService.getSubject().getPrincipals() == null) return null;
		if (!realmName.isEmpty()) {
			Collection principals = securityService.getSubject().getPrincipals().fromRealm(realmName);
			if (principalType == null) principals.iterator().next();
			else {
				for (Object availablePrincipal : principals)
					if (availablePrincipal.getClass().isAssignableFrom(principalType)) { return availablePrincipal; }
			}
		}
		if (principalType != null) {
			Object principal = securityService.getSubject().getPrincipals().oneByType(principalType);
			if (principal == null)
				throw new NullPointerException("Subject is required to have a configured principal of type '" + principalType
					+ "' for secure entity relation checks");
			return principal;
		}
		return securityService.getSubject().getPrincipals().getPrimaryPrincipal();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<T> find(EntityManager delegate,
		HttpServletRequest request,
		Object principal,
		String requiredAssociationValue,
		Predicate predicate1,
		Class<T> entityClass,
		Object entityId,
		LockModeType lockMode,
		Map<String, Object> properties) {
		String requiredRoleValue = RequiresAnnotationUtil.getRequiredRole(entityClass, Operation.READ);

		if (requiredRoleValue != null && request.isUserInRole(requiredRoleValue))
			return Stream.of(delegate.find(entityClass, entityId, lockMode, properties)).collect(Collectors.toList());

		if (requiredAssociationValue == null) {
			// proceed as normal if there's neither RequiresRole nor RequiresAssociation, directly return null if role didn't match
			if (requiredRoleValue != null) return Collections.emptyList();
			if (entityId != null)
				return Stream.of(delegate.find(entityClass, entityId, lockMode, properties)).collect(Collectors.toList());
			// even if assocation is not required for read, we can still use it to find the entity
			RequiresAssociation annotation = entityClass.getAnnotation(RequiresAssociation.class);
			if (annotation == null) return Collections.emptyList();
			requiredAssociationValue = annotation.value();
		}

		// return immediately if user is guest
		if (request.getRemoteUser() == null) return Collections.emptyList();

		CriteriaBuilder builder = delegate.getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = builder.createQuery(entityClass);
		Root<T> from = criteriaQuery.from(entityClass);
		CriteriaQuery<T> select = criteriaQuery.select(from);
		Metamodel metamodel = delegate.getMetamodel();

		EntityType entityType = metamodel.entity(entityClass);
		Type idType;
		SingularAttribute idAttr;
		Predicate predicate2 = null;

		// Object principal = getConfiguredPrincipal();
		// throw IllegalArgumentException below if idType doesn't match with principal's type

		// empty string indicates association to "self"
		if (requiredAssociationValue.isEmpty()) {
			idType = entityType.getIdType();
			// entityId may be null when finding entity by association
			if (entityId == null) entityId = principal;
			else if (!entityId.equals(principal)) return Collections.emptyList();
		} else {
			String[] associationAttributes = String.valueOf(requiredAssociationValue).split("\\.");
			Path<?> path = null;

			// find the type of the top entity while traversing the property path
			for (String attributeName : associationAttributes) {
				path = path == null ? from.get(attributeName) : path.get(attributeName);
				Attribute attribute = entityType.getAttribute(attributeName);
				if (!attribute.isAssociation() && !attribute.isCollection()) throw new EntityNotFoundException(
					"association " + requiredAssociationValue + " does not exist for base type" + entityType.getName());
				if (attribute.isCollection()) {
					Root<?> pathRoot = criteriaQuery.from(entityType);
					entityType = metamodel.entity(((PluralAttribute) attribute).getBindableJavaType());
					path = pathRoot.join(attributeName);
				} else entityType = metamodel.entity(attribute.getJavaType());
			}

			idType = entityType.getIdType();
			idAttr = entityType.getId(idType.getJavaType());

			// TODO handle subject == null
			predicate2 = builder.equal(path.get(idAttr.getName()), principal);

		}

		// predicate1 is for finding the entity itself in case entityId was given as an argument or deduced
		// Predicate predicate1 = null;
		// if (entityId != null) {
		// idType = entityType.getIdType();
		// idAttr = entityType.getId(idType.getJavaType());
		// predicate1 = builder.equal(from.get(idAttr.getName()), entityId);
		// }
		criteriaQuery
			.where(predicate1 == null ? predicate2 : predicate2 == null ? predicate1 : builder.and(predicate1, predicate2));
		// getSingleResult throws an exception if no results are found, so get the list instead
		// List results = delegate.createQuery(criteriaQuery).getResultList();
		return delegate.createQuery(criteriaQuery).getResultList();
		// if (results.size() > 1)
		// throw new NonUniqueResultException("More than a single result of type " + entityClass.getName() + " found for "
		// + (entityId == null ? "association " + requiredAssociationValue : "id " + entityId));
		// return (T) (results.size() <= 0 ? null : results.get(0));
	}

}
