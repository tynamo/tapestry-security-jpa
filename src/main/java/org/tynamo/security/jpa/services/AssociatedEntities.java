package org.tynamo.security.jpa.services;

import java.util.List;

import javax.persistence.EntityManager;

public interface AssociatedEntities {
	<T> List<T> findAll(EntityManager em, Class<T> entityClass) throws Exception;

}
