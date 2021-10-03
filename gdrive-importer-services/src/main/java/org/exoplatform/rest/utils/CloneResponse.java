package org.exoplatform.rest.utils;

import org.exoplatform.services.cms.drives.DriveData;

import javax.ws.rs.core.Response;

public class CloneResponse extends ServiceResponse {

    /** The Constant CONNECT_COOKIE. */
    public static final String    CONNECT_COOKIE         = "cpgdrive-cloud-drive-connect-id";

    /** The Constant ERROR_COOKIE. */
    public static final String    ERROR_COOKIE           = "cpgdrive-cloud-drive-error";

    /** The Constant INIT_COOKIE. */
    public static final String    INIT_COOKIE            = "cpgdrive-cloud-drive-init-id";

    /** The Constant INIT_COOKIE_PATH. */
    public static final String    INIT_COOKIE_PATH       = "/portal/rest/copygdrive/clone";

    /**
     * Error cookie expire time in seconds.
     */
    public static final int       ERROR_COOKIE_EXPIRE    = 5;       // 5sec

    /** The service url. */
    String    serviceUrl;

    /** The progress. */
    int       progress;

    /** The drive. */
    ClonedDrive drive;

    /** The error. */
    String    error;

    /** The location. */
    String    location;

    /**
     * Service url.
     *
     * @param serviceUrl the service url
     * @return the connect response
     */
    public CloneResponse serviceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    /**
     * Progress.
     *
     * @param progress the progress
     * @return the connect response
     */
    public CloneResponse progress(int progress) {
        this.progress = progress;
        return this;
    }

    /**
     * Drive.
     *
     * @param drive the drive
     * @return the connect response
     */
    public CloneResponse drive(ClonedDrive drive) {
        this.drive = drive;
        return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @return the connect response
     */
    public CloneResponse error(String error) {
        this.error = error;
        return this;
    }

    /**
     * Location.
     *
     * @param location the location
     * @return the connect response
     */
    public CloneResponse location(String location) {
        this.location = location;
        return this;
    }

    /**
     * Connect error.
     *
     * @param error the error
     * @param connectId the connect id
     * @param host the host
     * @return the connect response
     */
    public CloneResponse connectError(String error, String connectId, String host) {
        if (connectId != null) {
            cookie(CONNECT_COOKIE, connectId, "/", host, "Cloud Drive connect ID", 0, false);
        }
        cookie(ERROR_COOKIE, error, "/", host, "Cloud Drive connection error", ERROR_COOKIE_EXPIRE, false);
        this.error = error;

        return this;
    }

    /**
     * Auth error.
     *
     * @param message the message
     * @param host the host
     * @param providerName the provider name
     * @param initId the init id
     * @param baseHost the base host
     * @return the connect response
     */
    public CloneResponse authError(String message, String host, String providerName, String initId, String baseHost) {
        if (initId != null) {
            // need reset previous cookie by expire time = 0
            cookie(INIT_COOKIE, initId, INIT_COOKIE_PATH, baseHost, "Cloud Drive init ID", 0, false);
        }
        cookie(ERROR_COOKIE, message, "/", host, "Cloud Drive connection error", ERROR_COOKIE_EXPIRE, false);
        super.entity("<!doctype html><html><head><script type='text/javascript'> setTimeout(function() {window.close();}, 4000);</script></head><body><div id='messageString'>"
                + (providerName != null ? providerName + " return error: " + message : message) + "</div></body></html>");

        return this;
    }

    /**
     * Auth error.
     *
     * @param message the message
     * @param host the host
     * @return the connect response
     */
    public CloneResponse authError(String message, String host) {
        return authError(message, host, null, null, null);
    }

    /**
     * Builds the.
     *
     * @return the response
     * @inherritDoc
     */

    @Override
    public Response build() {
        if (drive != null) {
            super.entity(new CommandState(drive, error, progress, serviceUrl));
        } else if (error != null) {
            super.entity(new CommandState(error, progress, serviceUrl));
        } else if (location != null) {
            super.addHeader("Location", location);
            super.entity("<!doctype html><html><head></head><body><div id='redirectLink'>" + "<a href='" + location
                    + "'>Use new location to the service.</a>" + "</div></body></html>");
        } // else - what was set in entity()
        return super.build();
    }
}
