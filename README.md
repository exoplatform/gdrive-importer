# gdrive-importer
Google Drive Documents importer

Enable Google Drive API
-----------------------

- Go to the Google API Console : https://code.google.com/apis/console/
- Create an new API project
- In the APIs page, enable the Drive API

![Google Drive API](https://raw.github.com/exo-addons/cloud-drive-extension/master/documentation/readme/google-drive-api.png)

- You need OAuth consent screen for you app, if not yet already created - then fill this tab in API Credentials page: product name, homepage (below assumed as http://myplatform.com), logo, terms.
- In the API Credentials tab, add credentials of type "OAuth 2.0 client ID"
- Choose "Web application" type for your client ID 
- Enter a name of the client, e.g. "My Platform Web Client"
- Enter "Authorized Redirect URIs", e.g. http://myplatform.com/portal/rest/copygdrive/clone/cgdrive. Note that path in the URI should be exactly  "portal/rest/copygdrive/clone/cgdrive".
- Provide "Authorized JavaScript Origins": http://myplatform.com
- Click "Create" button
- Remember `Client ID` and `Client secret` for configuration below.

![Google Drive API Access](https://raw.github.com/exo-addons/cloud-drive-extension/master/documentation/readme/google-drive-access.png)

Configuration
-------------

Open the configuration file of your Platform server `/opt/platform-tomcat/gatein/conf/exo.properties`.

Add the following properties:

    copygdrive.service.schema=http
    copygdrive.service.host=myplatform.com
    copygdrive.google.client.id=00000000000@developer.gserviceaccount.com
    copygdrive.google.client.secret=secret_key

The `copygdrive.google.client.id` parameter is the `Client ID` of the service account (available in your Google console, see previous screenshot).
The `copygdrive.google.client.secret` parameter is `Client Secret` of the service account (available in your Google console, see above).

By default, Cloud Drive assumes that it runs on non-secure host (http protocol).
