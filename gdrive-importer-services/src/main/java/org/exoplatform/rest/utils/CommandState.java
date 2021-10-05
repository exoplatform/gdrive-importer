package org.exoplatform.rest.utils;

import org.exoplatform.services.cms.drives.DriveData;

public class CommandState {

    /** The service url. */
    final String    serviceUrl;

    /** The drive. */
    final ClonedDrive drive;

    /** The error. */
    final String    error;

    /** The progress. */
    final int       progress;

    /**
     * Instantiates a new command state.
     *
     * @param drive the drive
     * @param progress the progress
     * @param serviceUrl the service url
     */
    CommandState(ClonedDrive drive, int progress, String serviceUrl) {
        this.drive = drive;
        this.progress = progress;
        this.serviceUrl = serviceUrl;
        this.error = "";
    }

    /**
     * Instantiates a new command state.
     *
     * @param drive the drive
     * @param error the error
     * @param progress the progress
     * @param serviceUrl the service url
     */
    CommandState(ClonedDrive drive, String error, int progress, String serviceUrl) {
        this.drive = drive;
        this.error = error;
        this.progress = progress;
        this.serviceUrl = serviceUrl;
    }

    /**
     * Instantiates a new command state.
     *
     * @param error the error
     * @param progress the progress
     * @param serviceUrl the service url
     */
    CommandState(String error, int progress, String serviceUrl) {
        this(null, error, progress, serviceUrl);
    }

    /**
     * Gets the drive.
     *
     * @return the drive
     */
    public ClonedDrive getDrive() {
        return drive;
    }

    /**
     * Gets the progress.
     *
     * @return the progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Gets the service url.
     *
     * @return the serviceUrl
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public String getError() {
        return error;
    }
}


