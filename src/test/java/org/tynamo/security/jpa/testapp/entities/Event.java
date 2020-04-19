package org.tynamo.security.jpa.testapp.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.tynamo.security.jpa.annotations.Operation;
import org.tynamo.security.jpa.annotations.RequiresAssociation;

// Event is an object that can have multiple managers
@RequiresAssociation(value = "managers", operations = Operation.ANY)
@Entity
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToMany
	private List<User> managers;

	private String value;

	public List<User> getManagers() {
		return managers;
	}

	public void setManagers(List<User> managers) {
		this.managers = managers;
	}

	public Long getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
