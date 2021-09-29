package org.exoplatform.rest.utils;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceResponse {

    /** The status. */
    Response.Status status;

    /** The entity. */
    Object              entity;

    /** The headers. */
    Map<String, String> headers = new HashMap<String, String>();

    /** The cookies. */
    List<NewCookie> cookies = new ArrayList<NewCookie>();

    /**
     * Status.
     *
     * @param status the status
     * @return the service response
     */
    public ServiceResponse status(Response.Status status) {
        this.status = status;
        return this;
    }

    /**
     * Ok.
     *
     * @return the service response
     */
    public ServiceResponse ok() {
        status = Response.Status.OK;
        return this;
    }

    /**
     * Client error.
     *
     * @param entity the entity
     * @return the service response
     */
    public ServiceResponse clientError(Object entity) {
        this.status = Response.Status.BAD_REQUEST;
        this.entity = entity;
        return this;
    }

    /**
     * Error.
     *
     * @param entity the entity
     * @return the service response
     */
    public ServiceResponse error(Object entity) {
        this.status = Response.Status.INTERNAL_SERVER_ERROR;
        this.entity = entity;
        return this;
    }

    /**
     * Entity.
     *
     * @param entity the entity
     * @return the service response
     */
    public ServiceResponse entity(Object entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Adds the header.
     *
     * @param name the name
     * @param value the value
     * @return the service response
     */
    public ServiceResponse addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * Cookie.
     *
     * @param cookie the cookie
     * @return the service response
     */
    public ServiceResponse cookie(NewCookie cookie) {
        cookies.add(cookie);
        return this;
    }

    /**
     * Cookie.
     *
     * @param name the name
     * @param value the value
     * @param path the path
     * @param domain the domain
     * @param comment the comment
     * @param maxAge the max age
     * @param secure the secure
     * @return the service response
     */
    public ServiceResponse cookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure) {
        cookies.add(new NewCookie(name, value, path, domain, comment, maxAge, secure));
        return this;
    }

    /**
     * Builds the.
     *
     * @return the response
     */
    public Response build() {
        Response.ResponseBuilder builder = Response.status(status != null ? status : Response.Status.OK);

        if (entity != null) {
            builder.entity(entity);
        }

        if (cookies.size() > 0) {
            builder.cookie(cookies.toArray(new NewCookie[cookies.size()]));
        }

        for (Map.Entry<String, String> he : headers.entrySet()) {
            builder.header(he.getKey(), he.getValue());
        }

        return builder.build();
    }

}
