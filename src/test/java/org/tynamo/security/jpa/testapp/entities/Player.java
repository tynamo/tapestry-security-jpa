package org.tynamo.security.jpa.testapp.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.tynamo.security.jpa.annotations.RequiresAssociation;

@RequiresAssociation("team.owner")
@Entity
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	public Long getId() {
		return id;
	}

	 @ManyToOne
	 private Team team;

	 public Team getTeam() {
	 return team;
	 }

	 public void setTeam(Team team) {
	 this.team = team;
	 }


}
