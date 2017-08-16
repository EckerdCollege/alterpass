# AlterPass Server

**Purpose:** Provide capabilities for password management at Eckerd College, 
through forgot password and change password for current students and employees.

### Configuration

This application is configured through environmental variables loaded at runtime.
These variables are necessary and the program will fail to execute without their
presence. They are located at `src/main/scala/edu/eckerd/alterpass/Configuration.scala`


`ORACLE_HOST` - Oracle Host to Connect to.  
`ORACLE_PORT` - Port at which the ORACLE_SID can be found  
`ORACLE_SID ` - Database Identifier  
`ORACLE_USER` - Oracle Database User with correct read permissions on `GOREMAL` to connect with  
`ORACLE_PASS` - Oracle Password for the User  

The above are necessary for the Hikari Connection Pool to communicate with Banner via jdbc. 
The ojdbc7.jar will need to be provided in the `lib/` folder in this directory as an unmanaged dependency as
it is not open source.


`GOOGLE_DOMAIN` - Google Domain Being Managed  
`GOOGLE_SERVICE_ACCOUNT` - Service Account for Connecting as, with appropriate Google API permissions for 
User Management. As consistent with google's `AdminDirectory` API.  
`GOOGLE_ADMIN_ACCOUNT` - This is the administrator account for impersonation with User level permissions for 
managing the Google Domain.  
`GOOGLE_CREDENTIALS_FILE` - This is a file provided by google 
`GOOGLE_APP_NAME` - Application name indicated for API access.

This is required for `google-api-scala` which communicates with the Google API's to handle changes in Google.

`SQLLITE_PATH` - This is the path to the folder into which the SqlLite Database `alterpass.db` will be placed.
User Requires Write Access to the Folder.

`AGING_FILE_PATH` This is the path the aging file is written to, it is transferred to a secondary server outside of
the current scope of the application to handle password aging.

`LDAP_HOST` - LDAP host to connect to.  
`LDAP_BASE_DN` - This is the base DN for searching within.  
`LDAP_SEARCH_ATTR` - This is the attribute that the username entered is checked against for bind, and existence.  
`LDAP_USER` - The LDAP administrative user.  
`LDAP_PASS` - The LDAP administrative user password.  

`SMTP_HOSTNAME` - SMTP Server to Send Mail Via  
`SMTP_USER` - User to Send Mail As  
`SMTP_PASS` - User Password for Sending Mail  
`EMAIL_LINK` - The Forgot Password Email Link to Be Rendered when sending the email. 
Allows backend to run at any location, and for the email to handle the redirection as necessary. 
This is the base uri/link without the additional path segments.

### Files


**OJDBC - REQUIRED** - As stated previously this is a closed source component and needs to be added. It needs to be placed in the
`/opt/alterpass/src/lib/` folder. It can be found on the 
[Oracle site](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-ucp-122-3110062.html).

**Google P12 - REQUIRED** - Google Provides a connection file which is used for API access. This needs to be present.
Default location assumes `/opt/alterpass/conf/alterpass.p12`

**Images** - This site makes use of some static binary image content that needs to be placed at 
`/opt/alterpass/src/src/main/resources/static/img`. Without these the backgrounds will be bland, and a broken image link will appear, but
will function equivalently, as images are graphical aides only.

**Fonts** - Eckerd College Utilizes Proprietary Fonts and as a result these font files need to be placed in
`/opt/alterpass/src/src/main/resources/static/fonts`. Without these it will not appear identical but will function equivalently.


### Install Instructions - EL 7

1. Execute `bin/setupEL.sh`
2. Add Users with access to alterpass group.
3. Add files listed above to acceptable locations.
4. Fill out `/opt/alterpass/conf/alterpass.conf` with appropriate variables.

### Eckerd Variables

1. The home for these files is on morpheus in `/home/alterpass/files`.





