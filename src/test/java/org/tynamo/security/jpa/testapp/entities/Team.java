package org.tynamo.security.jpa.testapp.entities;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.tynamo.security.jpa.JpaSecurityModuleUnitTest.TestOwnerEntity;
import org.tynamo.security.jpa.annotations.RequiresAssociation;

@RequiresAssociation("owner")
@Entity
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	public Long getId() {
		return id;
	}

	@ManyToOne
	private TestOwnerEntity owner;

	@OneToMany(mappedBy = "team")
	private Set<Player> players;

	public TestOwnerEntity getOwner() {
		return owner;
	}

	public void setOwner(TestOwnerEntity owner) {
		this.owner = owner;
	}

	public Set<Player> getPlayers() {
		return players;
	}

	public void setPlayers(Set<Player> players) {
		this.players = players;
	}

}
