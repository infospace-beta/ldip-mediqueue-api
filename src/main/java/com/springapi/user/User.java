package com.springapi.user;

import com.springapi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Table name is "users" because "user" is a reserved word in PostgreSQL.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, length = 100)
	private String fullName;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

}
