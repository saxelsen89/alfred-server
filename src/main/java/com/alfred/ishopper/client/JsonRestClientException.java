package com.alfred.ishopper.client;

public class JsonRestClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public JsonRestClientException(String message) {
		super(message);
	}

	public JsonRestClientException(String message, Throwable t) {
		super(message, t);
	}

}
