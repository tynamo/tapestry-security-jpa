package org.tynamo.security.jpa.internal;

import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.jpa.annotations.RequiresAssociation;
import org.tynamo.security.jpa.annotations.RequiresRole;

// We need to duplicate the code because annotations cannot be extended
public abstract class RequiresAnnotationUtil {
	public static String getRequiredRole(Class<?> aClass, Operation operation) {
		RequiresRole annotation = aClass.getAnnotation(RequiresRole.class);
		if (annotation == null) return null;

		for (Operation requiredOperation : annotation.operations()) {
			if (Operation.ANY.equals(requiredOperation) || operation.equals(requiredOperation)) return annotation.value();
			if (Operation.WRITE.equals(operation))
				if (Operation.INSERT.equals(requiredOperation) || Operation.UPDATE.equals(requiredOperation)
					|| Operation.DELETE.equals(requiredOperation)) return annotation.value();
		}
		return null;
	}

	public static String getRequiredAssociation(Class<?> aClass, Operation operation) {
		RequiresAssociation annotation = aClass.getAnnotation(RequiresAssociation.class);
		if (annotation == null) return null;

		for (Operation requiredOperation : annotation.operations()) {
			if (Operation.ANY.equals(requiredOperation) || operation.equals(requiredOperation)) return annotation.value();
			if (Operation.WRITE.equals(operation))
				if (Operation.INSERT.equals(requiredOperation) || Operation.UPDATE.equals(requiredOperation)
					|| Operation.DELETE.equals(requiredOperation)) return annotation.value();
		}
		return null;
	}

}
