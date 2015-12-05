package com.alfred.ishopper.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;


public class JsonRestClient {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonRestClient.class);

	private static final String HTTP_SCHEME = "http://";
	private static final String HTTPS_SCHEME = "https://";

	private static final int DEFAULT_HTTP_PORT = 80;
	private static final int DEFAULT_HTTPS_PORT = 443;
	
	private static final String HTTP_METHOD_GET = "GET";
	private static final String HTTP_METHOD_POST = "POST";
	private static final String HTTP_METHOD_PATCH = "PATCH";
	private static final String HTTP_METHOD_PUT = "PUT";
	private static final String HTTP_METHOD_DELETE = "DELETE";
	
	private static final int HTTP_STATUS_FORBIDDEN = 403;
	private static final int HTTP_STATUS_NOT_FOUND = 404;
	private static final int HTTP_STATUS_UNPROCESSABLE_ENTITY = 422;
	

	private static final String REQUEST_FAILED = "{} request failed";
	
	private static final String USER_ID = "userId";
	private static final String EMARKETS_ID = "emarketsId";

	public static final String APPLICATION_VND_API_JSON = "application/vnd.api+json";

	private static final String TICKET_KEY = "encodedTicket";

	private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
	
	private boolean convertStatusCodes = true;

	private static ObjectMapper mapper = new ObjectMapper();
	
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private WebResource webResource;
	private String encodedCredentials;
	private String mediaType = MediaType.APPLICATION_JSON;
	
	public JsonRestClient(Client client, ClientConfiguration config) throws URISyntaxException {	
		this.webResource = client.resource(new URI(
				(config.getBaseUri().startsWith(HTTP_SCHEME) && config.getBasePort() != DEFAULT_HTTP_PORT) ||
				(config.getBaseUri().startsWith(HTTPS_SCHEME) && config.getBasePort() != DEFAULT_HTTPS_PORT) ?
						config.getBaseUri() + ":" + config.getBasePort() :	config.getBaseUri()));
	}

	public JsonRestClient(Client client, AuthenticatedClientConfiguration config) throws URISyntaxException {
		this(client, (ClientConfiguration)config);
		try {
			final byte[] usernamePassword = (config.getUsername() + ":" + config.getPassword()).getBytes();
			this.encodedCredentials = "Basic " + new String(Base64.encode(usernamePassword), "ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Cannot encode Basic Authentication string", e);
		} 
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public void setConvertStatusCodes(boolean convertStatusCodes) {
		this.convertStatusCodes = convertStatusCodes;
	}

	public static Map<String, Cookie> convert(HttpServletRequest request) {
		Map<String, Cookie> cookies = new HashMap<String, Cookie>();

		for (javax.servlet.http.Cookie c : request.getCookies()) {
			cookies.put(c.getName(), new Cookie(c.getName(), c.getValue(), c.getPath(), c.getDomain(), c.getVersion()));
		}

		return cookies;
	}
	
	public ClientResponse getResponse(String resource, MultivaluedMap<String, String> params, Map<String, String> headers, UserAccount userAccount, String mediaType) {
		return getResponse(resource, params, null, null, headers, userAccount, mediaType == null ? this.mediaType : mediaType);
	}

	public String get(String resource, AuthUser user) {
		return get(resource, null, null, null, user);
	}

	public String get(String resource, MultivaluedMap<String, String> params, AuthUser user) {
		return get(resource, params, null, null, user);
	}

	public String get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader) {

		return get(resource, params, cookies, xForwardedForHeader, false, null);
	}

	public String get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader, AuthUser user) {

		return get(resource, params, cookies, xForwardedForHeader, false, user);
	}

	public String get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader, boolean requireTicket) {

		return get(resource, params, cookies, xForwardedForHeader, requireTicket, null);
	}
		
	public String get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader, boolean requireTicket, AuthUser user) {

		if (requireTicket && (cookies == null || !cookies.containsKey(TICKET_KEY))) {
			return "";
		}

		if (resource == null) {
			resource = "";
		}

		return getResponse(resource, params, cookies, xForwardedForHeader, user, mediaType).getEntity(String.class);
	}
	
	public <T> T get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader, Class<T> valueType) throws JsonRestClientException {

		return get(resource, params, cookies, xForwardedForHeader, null, valueType);
	}

	public <T> T get(String resource, MultivaluedMap<String, String> params, AuthUser user, Class<T> valueType)
			throws JsonRestClientException {
		
		return get(resource, params, null, null, user, valueType);
	}

	public <T> T get(String resource, MultivaluedMap<String, String> params, Map<String, Cookie> cookies,
			Object xForwardedForHeader, AuthUser user, Class<T> valueType) throws JsonRestClientException {

		String result = get(resource, params, cookies, xForwardedForHeader, user);

		return (result != null) ? map(result, valueType) : null;
	}

	protected ClientResponse getResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, AuthUser user, String mediaType) {
		if (user != null) {
			return getResponse(resource, params, cookies, xForwardedForHeader, null, user.getId(), user.getEmarketsId(), mediaType);
		} else {
			return getResponse(resource, params, cookies, xForwardedForHeader, null, null, null, mediaType);
		}
	}
	
	protected ClientResponse getResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, UserAccount userAccount, String mediaType) {
		if (userAccount != null) {
			return getResponse(resource, params, cookies, xForwardedForHeader, headers, userAccount.getUserId(), userAccount.getEmarketsId(), mediaType);
		} else {
			return getResponse(resource, params, cookies, xForwardedForHeader, headers, null, null, mediaType);
		}
	}

	protected ClientResponse getResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, Long userId, String emarketsId, String mediaType) {

		ClientResponse response = null;
		
		try {
			response =
					buildResourceBuilder(resource, params, cookies, combineHeaders(headers, xForwardedForHeader, userId, emarketsId), mediaType).get(
							ClientResponse.class);
		} catch (ClientHandlerException e) {
			LOGGER.error(REQUEST_FAILED, HTTP_METHOD_GET, e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			LOGGER.error(REQUEST_FAILED, HTTP_METHOD_GET, e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		if (response == null) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("HTTP code {} returned from {} with params {}", response.getStatus(), webResource.getURI() + resource, params);
		}

		if (convertStatusCodes && !mediaType.equals(APPLICATION_VND_API_JSON)) {
			Response.Status convertedResponseStatus = convertResponseStatus(response.getClientResponseStatus());
		
			if (convertedResponseStatus != Response.Status.OK) {
				response.close();
				throw new WebApplicationException(convertedResponseStatus);
			}
		}

		return response;
	}
	
	private Map<String, String> combineHeaders(Map<String, String> headers,
			Object xForwardedForHeader, Long userId, String emarketsId) {
		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		
		if (xForwardedForHeader != null) {
			headers.put(HEADER_X_FORWARDED_FOR, xForwardedForHeader.toString());
		}
		
		if (userId != null) {			
			headers.put(USER_ID, userId.toString());
		}
		
		if (emarketsId != null) {			
			headers.put(EMARKETS_ID, emarketsId);
		}
		
		return headers;
	}

	public ClientResponse delete(String resource, MultivaluedMap<String, String> params, Map<String, String> headers, UserAccount userAccount, String mediaType) {
		return deleteResponse(resource, params, null, null, headers, userAccount, mediaType == null ? this.mediaType : mediaType);
	}
	
	protected ClientResponse deleteResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, UserAccount userAccount, String mediaType) {
		if (userAccount != null) {
			return deleteResponse(resource, params, cookies, xForwardedForHeader, headers, userAccount.getUserId(), userAccount.getEmarketsId(), mediaType);
		} else {
			return deleteResponse(resource, params, cookies, xForwardedForHeader, headers, null, null, mediaType);
		}
	}

	protected ClientResponse deleteResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, Long userId, String emarketsId, String mediaType) {

		ClientResponse response = null;
		
		try {
			response = buildResourceBuilder(resource, params, cookies, combineHeaders(headers, xForwardedForHeader, userId, emarketsId), mediaType).delete(ClientResponse.class);
		} catch (ClientHandlerException e) {
			LOGGER.error(REQUEST_FAILED, HTTP_METHOD_DELETE, e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			LOGGER.error(REQUEST_FAILED, HTTP_METHOD_DELETE, e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		if (response == null) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("HTTP code {} returned from {} with params {}", response.getStatus(), webResource.getURI() + resource, params);
		}

		if (convertStatusCodes && !mediaType.equals(APPLICATION_VND_API_JSON)) {
			Response.Status convertedResponseStatus = convertResponseStatus(response.getClientResponseStatus());
		
			if (convertedResponseStatus != Response.Status.OK) {
				response.close();
				throw new WebApplicationException(convertedResponseStatus);
			}
		}

		return response;
	}
	
	public ClientResponse post(String resource, MultivaluedMap<String, String> params, Map<String, String> headers,
			UserAccount userAccount, String mediaType, Object entity) {
		return post(resource, params, null, null, headers, userAccount, mediaType == null ? this.mediaType : mediaType, entity);
	}
	
	public ClientResponse post(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, UserAccount userAccount, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}
		
		if (userAccount != null) {			
			return putPostResponse(resource, params, cookies, xForwardedForHeader, headers, userAccount.getUserId(), userAccount.getEmarketsId(), mediaType, HTTP_METHOD_POST, entity);
		} else {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, headers, null, null, mediaType, HTTP_METHOD_POST, entity);
		}
	}
	
	public String post(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, AuthUser user, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}
		
		if (user != null) {			
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, user.getId(), user.getEmarketsId(), mediaType, HTTP_METHOD_POST, entity).getEntity(String.class);
		} else {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, null, null, mediaType, HTTP_METHOD_POST, entity).getEntity(String.class);
		}
	}

	public ClientResponse patch(String resource, MultivaluedMap<String, String> params, Map<String, String> headers,
			UserAccount userAccount, String mediaType, Object entity) {
		
		return patch(resource, params, null, null, headers, userAccount, mediaType == null ? this.mediaType : mediaType, entity);
	}
	
	public String patch(String resource, AuthUser user, Object entity) {
		return patch(resource, null, null, null, user, mediaType, entity);
	}

	public String patch(String resource, AuthUser user, String mediaType, Object entity) {
		return patch(resource, null, null, null, user, mediaType, entity);
	}
	
	public ClientResponse patch(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, UserAccount userAccount, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}

		if (userAccount != null) {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, headers, userAccount.getUserId(), userAccount.getEmarketsId(), mediaType, HTTP_METHOD_PATCH, entity);
		} else {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, headers, null, null, mediaType, HTTP_METHOD_PATCH, entity);
		}
	}
	
	public String patch(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, AuthUser user, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}

		if (user != null) {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, user.getId(), user.getEmarketsId(), mediaType, HTTP_METHOD_PATCH, entity).getEntity(String.class);
		} else {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, null, null, mediaType, HTTP_METHOD_PATCH, entity).getEntity(String.class);
		}
	}
	
	public ClientResponse patchResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, AuthUser user, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}

		if (user != null) {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, user.getId(), user.getEmarketsId(), mediaType, HTTP_METHOD_PATCH, entity);
		} else {
			return putPostResponse(resource, params, cookies, xForwardedForHeader, null, null, null, mediaType, HTTP_METHOD_PATCH, entity);
		}
	}
	
	public ClientResponse put(String resource, MultivaluedMap<String, String> params, Map<String, String> headers, UserAccount userAccount, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}

		if (userAccount != null) {			
			return putPostResponse(resource, params, null, null, headers, userAccount.getUserId(), userAccount.getEmarketsId(), mediaType == null ? this.mediaType : mediaType, HTTP_METHOD_PUT, entity);
		} else {
			return putPostResponse(resource, params, null, null, headers, null, null, mediaType == null ? this.mediaType : mediaType, HTTP_METHOD_PUT, entity);
		}
	}

	public String put(String resource, AuthUser user, String mediaType, Object entity) {

		if (resource == null) {
			resource = "";
		}

		if (user != null) {
			return putPostResponse(resource, null, null, null, null, user.getId(), user.getEmarketsId(), mediaType, HTTP_METHOD_PUT, entity).getEntity(String.class);
		} else {
			return putPostResponse(resource, null, null, null, null, null, null, mediaType, HTTP_METHOD_PUT, entity).getEntity(String.class);
		}
	}
	
	protected ClientResponse putPostResponse(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader, Map<String, String> headers, Long userId, String emarketsId, String mediaType, String httpMethod, Object entity) {

		ClientResponse response = null;

		try {
			WebResource.Builder builder = buildResourceBuilder(resource, params, cookies, combineHeaders(headers, xForwardedForHeader, userId, emarketsId), mediaType);
			if (entity != null) {
				builder = builder.entity(entity, mediaType);
			}
			if (httpMethod.equals(HTTP_METHOD_PUT)) {
				response = builder.put(ClientResponse.class);
			} else if (httpMethod.equals(HTTP_METHOD_POST) || httpMethod.equals(HTTP_METHOD_PATCH)) {
				if (httpMethod.equals(HTTP_METHOD_PATCH)) {
					builder.header("X-HTTP-Method-Override", "PATCH");	
				}
				response = builder.post(ClientResponse.class);
			} else {
				throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
			}

		} catch (ClientHandlerException e) {
			LOGGER.error(REQUEST_FAILED, httpMethod, e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			LOGGER.error(REQUEST_FAILED, httpMethod, e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		if (response == null) {
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("HTTP code {} returned from {} with params {}", response.getStatus(), webResource.getURI() + resource, params);
		}

		if (convertStatusCodes && !mediaType.equals(APPLICATION_VND_API_JSON)) {
			Response.Status convertedResponseStatus = convertResponseStatus(response.getClientResponseStatus());
		
			if (convertedResponseStatus != Response.Status.OK) {
				response.close();
				throw new WebApplicationException(convertedResponseStatus);
			}
		}

		return response;
	}
	
	private Response.Status convertResponseStatus(Status status) {
		Response.Status convertedStatus = Response.Status.OK;
		switch (status.getFamily()) {
		case INFORMATIONAL: // 1xx
		case SUCCESSFUL: // 2xx
		case REDIRECTION: // 3xx
			break;
		case CLIENT_ERROR: // 4xx
			switch(status.getStatusCode()) {
			case HTTP_STATUS_FORBIDDEN:
				convertedStatus = Response.Status.FORBIDDEN;
				break;
			case HTTP_STATUS_NOT_FOUND:
				convertedStatus = Response.Status.NOT_FOUND;
				break;
			default:
				convertedStatus = Response.Status.SERVICE_UNAVAILABLE;
				break;
			}
			break;
		case SERVER_ERROR: // 5xx
			convertedStatus = Response.Status.SERVICE_UNAVAILABLE;
			break;
		case OTHER:
		default:
			convertedStatus = Response.Status.INTERNAL_SERVER_ERROR;
			break;
		}
		return convertedStatus;
	}

	protected WebResource.Builder buildResourceBuilder(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Map<String, String> headers, String mediaType) {

		/* XXX: We may need to remove userId and emarketsId from params. <thb> */
		
		WebResource.Builder builder =
				(params != null && !params.isEmpty()) ? webResource.path(resource).queryParams(params)
						.getRequestBuilder() : webResource.path(resource).getRequestBuilder();

		if (cookies != null) {
			for (Cookie cookie : cookies.values()) {
				builder.cookie(cookie);
			}
		}
		
		if (headers != null) {
			for (Entry<String, String> entry : headers.entrySet()) {
				builder.header(entry.getKey(), entry.getValue());
			}
		}
		
		if (encodedCredentials != null) {
			builder.header("Authorization", encodedCredentials);
		}

		if (mediaType != null) {
			builder = builder.accept(mediaType);
		}

		return builder;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getMap(String resource, MultivaluedMap<String, String> params,
			Map<String, Cookie> cookies, Object xForwardedForHeader) throws JsonRestClientException {

		return get(resource, params, cookies, xForwardedForHeader, Map.class);
	}

	@SuppressWarnings("unchecked")
	public static Map<String,Object> map(String json) throws JsonRestClientException {
		return map(json, Map.class);
	}
	
	public static <T> T map(String json, Class<T> valueType) throws JsonRestClientException {
		try {
			return mapper.readValue(json, valueType);
		} catch (JsonParseException e) {
			throw new JsonRestClientException("Unable to parse JSON", e);
		} catch (JsonMappingException e) {
			throw new JsonRestClientException("Unable to map JSON to "  + valueType.getCanonicalName(), e);
		} catch (IOException e) {
			throw new JsonRestClientException("Unable retrive JSON", e);
		}		
	}
}
