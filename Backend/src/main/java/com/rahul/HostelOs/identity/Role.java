package com.rahul.HostelOs.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "name", nullable = false, length = 50, unique = true)
	private RoleName name;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	protected Role() {
	}

	public Long getId() {
		return id;
	}

	public RoleName getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
