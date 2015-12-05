package com.alfred.ishopper.client;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticatedClientConfiguration extends ClientConfiguration {
	@Valid
	@NotNull
	@JsonProperty
	private String username;

	@Valid
	@NotNull
	@JsonProperty
	private String password;

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}
}
