/**
 * Copyright (C) 2016 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

package org.bonitasoft.console.common.server.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.bonitasoft.console.common.server.preferences.properties.ConfigurationFilesManager;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.InvalidPlatformCredentialsException;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.util.APITypeManager;

/**
 * @author Baptiste Mesta
 */
public class PlatformManagementUtils {

    public static final String AUTOLOGIN_V6_JSON = "autologin-v6.json";

    private final ConfigurationFilesManager configurationFilesManager = ConfigurationFilesManager.getInstance();

    //package local for testing purpose
    boolean isLocal() throws UnknownAPITypeException, ServerAPIException, IOException {
        return ApiAccessType.LOCAL.equals(APITypeManager.getAPIType());
    }

    //package local for testing purpose
    PlatformAPI getPlatformAPI(final PlatformSession platformSession) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return PlatformAPIAccessor.getPlatformAPI(platformSession);
    }

    //package local for testing purpose
    PlatformLoginAPI getPlatformLoginAPI() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return PlatformAPIAccessor.getPlatformLoginAPI();
    }

    PlatformSession platformLogin() throws BonitaException, IOException {
        if (isLocal()) {
            try {
                return localPlatformLogin();
            } catch (final Exception e) {
                throw new ServerAPIException("unable to do the local login", e);
            }
        } else {
            String username = System.getProperty("org.bonitasoft.platform.username");
            String password = System.getProperty("org.bonitasoft.platform.password");
            try {

                return getPlatformLoginAPI().login(username,
                        password);
            } catch (InvalidPlatformCredentialsException e) {
                throw new InvalidPlatformCredentialsException("The portal is not able to login to the engine because " +
                        "system properties org.bonitasoft.platform.username and org.bonitasoft.platform.password are " +
                        "not set correctly to the platform administrator credentials.\n " +
                        "These properties must be set when connecting to a Bonita engine that is not local. " +
                        "If the engine is local, change the connection to LOCAL using the APITypeManager");
            }
        }
    }

    private PlatformSession localPlatformLogin()
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        final Class<?> api = Class.forName("org.bonitasoft.engine.LocalLoginMechanism");
        return (PlatformSession) api.getDeclaredMethod("login").invoke(api.newInstance());
    }

    void platformLogout(final PlatformSession platformSession) throws BonitaException {
        final PlatformLoginAPI platformLoginAPI = getPlatformLoginAPI();
        platformLoginAPI.logout(platformSession);
    }

    private void retrieveTenantsConfiguration(final PlatformAPI platformAPI) throws IOException {
        final Map<Long, Map<String, byte[]>> clientTenantConfigurations = platformAPI.getClientTenantConfigurations();
        for (final Map.Entry<Long, Map<String, byte[]>> tenantConfiguration : clientTenantConfigurations.entrySet()) {
            configurationFilesManager.setTenantConfigurations(tenantConfiguration.getValue(), tenantConfiguration.getKey());
        }
    }

    private void retrievePlatformConfiguration(final PlatformAPI platformAPI) throws IOException {
        final Map<String, byte[]> clientPlatformConfigurations = platformAPI.getClientPlatformConfigurations();
        configurationFilesManager.setPlatformConfigurations(clientPlatformConfigurations);
    }

    private void retrieveTenantAutologinConfiguration(final PlatformAPI platformAPI, final long tenantId) throws IOException {
        final byte[] tenantAutologinConfigurationContent = platformAPI.getClientTenantConfiguration(tenantId, AUTOLOGIN_V6_JSON);
        configurationFilesManager.setTenantConfiguration(AUTOLOGIN_V6_JSON, tenantAutologinConfigurationContent, tenantId);
    }

    public void initializePlatformConfiguration() throws BonitaException, IOException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        retrievePlatformConfiguration(platformAPI);
        retrieveTenantsConfiguration(platformAPI);
        platformLogout(platformSession);
    }

    public void updateConfigurationFile(final long tenantId, final String file, final byte[] content) throws IOException, BonitaException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        platformAPI.updateClientTenantConfigurationFile(tenantId, file, content);
        platformLogout(platformSession);
    }

    public void retrieveTenantsConfiguration() throws BonitaException, IOException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        retrieveTenantsConfiguration(platformAPI);
        platformLogout(platformSession);
    }

    public void retrieveAutologinConfiguration(final long tenantId) throws IOException, BonitaException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        retrieveTenantAutologinConfiguration(platformAPI, tenantId);
        platformLogout(platformSession);
    }
}
