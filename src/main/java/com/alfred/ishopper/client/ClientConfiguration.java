package com.alfred.ishopper.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientConfiguration {
	@JsonProperty
	private String baseUri;
	
	@JsonProperty
	private int basePort;

	public String getBaseUri() {
		return baseUri;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public int getBasePort() {
		return basePort;
	}

	public void setBasePort(int basePort) {
		this.basePort = basePort;
	}
}
