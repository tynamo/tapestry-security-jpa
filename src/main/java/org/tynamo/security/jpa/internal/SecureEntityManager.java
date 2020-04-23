package org.tynamo.security.jpa.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.util.ThreadContext;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.tynamo.security.jpa.EntitySecurityException;
import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.services.SecurityService;

public class SecureEntityManager implements EntityManager {
	private final EntityManager delegate;
	private final SecurityService securityService;
	private final HttpServletRequest request;
	private final PropertyAccess propertyAccess;
	private String realmName;
	private Class principalType;

	public SecureEntityManager(final SecurityService securityService, final PropertyAccess propertyAccess,
		final HttpServletRequest request, final EntityManager delegate, String realmName, Class principalType) {
		this.securityService = securityService;
		this.propertyAccess = propertyAccess;
		this.request = request;
		this.delegate = delegate;
		this.realmName = realmName;
		this.principalType = principalType;
	}

	public void clear() {
		delegate.clear();
	}

	public void close() {
		delegate.close();
	}

	public boolean contains(Object arg0) {
		return delegate.contains(arg0);
	}

	public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
		return delegate.createNamedQuery(arg0, arg1);
	}

	public Query createNamedQuery(String arg0) {
		return delegate.createNamedQuery(arg0);
	}

	public Query createNativeQuery(String arg0, Class arg1) {
		return delegate.createNativeQuery(arg0, arg1);
	}

	public Query createNativeQuery(String arg0, String arg1) {
		return delegate.createNativeQuery(arg0, arg1);
	}

	public Query createNativeQuery(String arg0) {
		return delegate.createNativeQuery(arg0);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		return delegate.createQuery(arg0);
	}

	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		return delegate.createQuery(arg0, arg1);
	}

	public Query createQuery(String arg0) {
		return delegate.createQuery(arg0);
	}

	public void detach(Object arg0) {
		delegate.detach(arg0);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return ThreadContext.getSecurityManager() == null ? delegate.find(entityClass, primaryKey, lockMode, properties)
			: secureFind(entityClass, primaryKey, lockMode, properties);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return ThreadContext.getSecurityManager() == null ? delegate.find(entityClass, primaryKey, lockMode) : secureFind(
			entityClass, primaryKey, lockMode, null);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return ThreadContext.getSecurityManager() == null ? delegate.find(entityClass, primaryKey, properties)
			: secureFind(entityClass, primaryKey, null, properties);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return ThreadContext.getSecurityManager() == null ? delegate.find(entityClass, primaryKey) : secureFind(
			entityClass, primaryKey, null, null);
	}

	public void flush() {
		delegate.flush();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	public Object getDelegate() {
		return delegate.getDelegate();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return delegate.getEntityManagerFactory();
	}

	public FlushModeType getFlushMode() {
		return delegate.getFlushMode();
	}

	public LockModeType getLockMode(Object arg0) {
		return delegate.getLockMode(arg0);
	}

	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
	}

	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		return delegate.getReference(arg0, arg1);
	}

	public EntityTransaction getTransaction() {
		return delegate.getTransaction();
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public void joinTransaction() {
		delegate.joinTransaction();
	}

	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		delegate.lock(arg0, arg1, arg2);
	}

	public void lock(Object arg0, LockModeType arg1) {
		delegate.lock(arg0, arg1);
	}

	public <T> T merge(T entity) {
		checkWritePermissions(entity, Operation.UPDATE);
		return delegate.merge(entity);
	}

	public void persist(Object entity) {
		checkWritePermissions(entity, Operation.INSERT);
		delegate.persist(entity);
	}

	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		delegate.refresh(arg0, arg1, arg2);
	}

	public void refresh(Object arg0, LockModeType arg1) {
		delegate.refresh(arg0, arg1);
	}

	public void refresh(Object arg0, Map<String, Object> arg1) {
		delegate.refresh(arg0, arg1);
	}

	public void refresh(Object arg0) {
		delegate.refresh(arg0);
	}

	public void remove(Object entity) {
		checkWritePermissions(entity, Operation.DELETE);
		delegate.remove(entity);
	}

	public void setFlushMode(FlushModeType arg0) {
		delegate.setFlushMode(arg0);
	}

	public void setProperty(String arg0, Object arg1) {
		delegate.setProperty(arg0, arg1);
	}

	public <T> T unwrap(Class<T> arg0) {
		return delegate.unwrap(arg0);
	}

	private Object getConfiguredPrincipal() {
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
	private <T> T secureFind(Class<T> entityClass,
		Object entityId,
		LockModeType lockMode,
		Map<String, Object> properties) {
		String requiredAssociationValue = RequiresAnnotationUtil.getRequiredAssociation(entityClass, Operation.READ);
		// predicate1 is for finding the entity itself in case entityId was given as an argument or deduced
		EntityType entityType = delegate.getMetamodel().entity(entityClass);
		Predicate predicate1 = null;

		if (entityId != null) {
			Type idType = entityType.getIdType();
			SingularAttribute idAttr = entityType.getId(idType.getJavaType());
			CriteriaBuilder builder = delegate.getCriteriaBuilder();
			CriteriaQuery<T> criteriaQuery = builder.createQuery(entityClass);
			Root<T> from = criteriaQuery.from(entityClass);
			predicate1 = builder.equal(from.get(idAttr.getName()), entityId);
		}
		// getSingleResult throws an exception if no results are found, so get the list instead
		List results = SecureFinder.find(delegate, request,
			SecureFinder.getConfiguredPrincipal(securityService, realmName, principalType),
			requiredAssociationValue,
			predicate1, entityClass,
			entityId, lockMode,
			properties);
		if (results == null) return null;
		if (results.size() > 1)
			throw new NonUniqueResultException("More than a single result of type " + entityClass.getName() + " found for "
				+ (entityId == null ? "association " + requiredAssociationValue : "id " + entityId));
		return (T) (results.size() <= 0 ? null : results.get(0));
	}

	private Annotation getAnnotation(Member member, Class annotationType) {
		return member instanceof Field ? ((Field) member).getAnnotation(annotationType)
			: member instanceof Method ? ((Method) member).getAnnotation(annotationType) : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void checkWritePermissions(final Object entity, Operation writeOperation) {
		if (ThreadContext.getSecurityManager() == null) return;
		String requiredRoleValue = RequiresAnnotationUtil.getRequiredRole(entity.getClass(), writeOperation);

		if (requiredRoleValue != null && request.isUserInRole(requiredRoleValue)) return;

		String requiredAssociationValue = RequiresAnnotationUtil.getRequiredAssociation(entity.getClass(), writeOperation);

		if (requiredAssociationValue == null) {
			// proceed as normal if there's neither RequiresRole nor RequiresAssociation, throw an exception if role didn't match
			if (requiredRoleValue == null) return;
			else throw new EntitySecurityException("Currently executing subject is not permitted to " + writeOperation
				+ " entities of type " + entity.getClass().getSimpleName());
		}
		// throw exception if user is guest
		if (request.getRemoteUser() == null)
			throw new EntitySecurityException("Guest users are not permitted to " + writeOperation + " entities of type "
				+ entity.getClass().getSimpleName());

		Metamodel metamodel = delegate.getMetamodel();
		EntityType entityType = metamodel.entity(entity.getClass());
		// empty association value indicates association to "self"
		Object associatedObject = entity;
		if (!requiredAssociationValue.isEmpty()) {
			// find the top entity by traversing the property path
			String[] associationAttributes = String.valueOf(requiredAssociationValue).split("\\.");
			for (String attributeName : associationAttributes) {
				Attribute attribute = entityType.getAttribute(attributeName);

				if (!attribute.isAssociation() && !attribute.isCollection()) throw new EntityNotFoundException(
					"association " + requiredAssociationValue + " does not exist for base type" + entityType.getName());
				// TODO we only support many-to-many relations as the top entity
				if (attribute.isCollection()) {
					// for collection attributes, the simplest, although not the most performant, approach
					// is to query the principal entity, then break
					entityType = metamodel.entity(((PluralAttribute) attribute).getBindableJavaType());
					associatedObject = find(entityType.getJavaType(), getConfiguredPrincipal());
					break;
				} else {
					entityType = metamodel.entity(attribute.getJavaType());
					associatedObject = propertyAccess.get(associatedObject, attributeName);
				}
				if (associatedObject == null)
					throw new EntitySecurityException("Subject for the required association is not set when executing "
						+ writeOperation + " on instance '" + entity + "' of type " + entity.getClass().getSimpleName());
			}
		}

		Type idType = entityType.getIdType();
		SingularAttribute idAttr = entityType.getId(idType.getJavaType());

		// handle INSERT operation to "self" with a generated id as a special allowed case
		if (associatedObject == entity && getAnnotation(idAttr.getJavaMember(), GeneratedValue.class) != null
			&& Operation.INSERT.equals(writeOperation) && propertyAccess.get(entity, idAttr.getName()) == null) return;
		else {
			Object principal = getConfiguredPrincipal();
			// TODO throw IllegalArgumentException below if idType doesn't match with principal's type

			if (!propertyAccess.get(associatedObject, idAttr.getName()).equals(principal))
				throw new EntitySecurityException("Currently executing subject is not permitted to " + writeOperation
					+ " entities of type " + entity.getClass().getSimpleName() + " because the required association didn't exist");
		}
	}
}
