package org.exoplatform.rest.utils;

import org.exoplatform.services.cms.clouddrives.CloudProvider;

public class ConnectInit {

    /** The local user. */
    private final String        localUser;

    /** The provider. */
    private final CloudProvider provider;

    /** The host. */
    private final String        host;

    /**
     * Instantiates a new connect init.
     *
     * @param localUser the local user
     * @param provider the provider
     * @param host the host
     */
    public ConnectInit(String localUser, CloudProvider provider, String host) {
        this.localUser = localUser;
        this.provider = provider;
        this.host = host;
    }

    public String getLocalUser() {
        return localUser;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public String getHost() {
        return host;
    }
}