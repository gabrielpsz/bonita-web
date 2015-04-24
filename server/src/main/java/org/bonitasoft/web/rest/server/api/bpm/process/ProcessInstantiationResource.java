/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.web.rest.server.api.bpm.process;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.console.common.server.utils.ContractTypeConverter;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.web.rest.server.api.resource.CommonResource;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.restlet.resource.Post;

/**
 * @author Nicolas Tith
 */
public class ProcessInstantiationResource extends CommonResource {

    static final String PROCESS_DEFINITION_ID = "processDefinitionId";

    private static final String USER_PARAM = "user";

    private final ProcessAPI processAPI;

    protected ContractTypeConverter typeConverterUtil = new ContractTypeConverter(ContractTypeConverter.ISO_8601_DATE_PATTERNS);

    public ProcessInstantiationResource(final ProcessAPI processAPI) {
        this.processAPI = processAPI;
    }

    @Post("json")
    public ProcessInstance instanciateProcess(final Map<String, Serializable> inputs) throws ProcessDefinitionNotFoundException, ProcessActivationException,
            ProcessExecutionException {
    	final String userId = getRequestParameter(USER_PARAM);
        final long processDefinitionId = getProcessDefinitionIdParameter();
        try {
            final ContractDefinition processContract = processAPI.getProcessContract(processDefinitionId);
            final Map<String, Serializable> processedInputs = typeConverterUtil.getProcessedInput(processContract, inputs);
        	if (userId == null) {
                return processAPI.startProcessWithInputs(processDefinitionId, processedInputs);
    		} else {
                return processAPI.startProcessWithInputs(Long.parseLong(userId), processDefinitionId, processedInputs);
    		}
        } catch (final ContractViolationException e) {
            manageContractViolationException(e, "Cannot instanciate process task.");
            return null;
        } catch (final Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    protected long getProcessDefinitionIdParameter() {
        final String processDefinitionId = getAttribute(PROCESS_DEFINITION_ID);
        if (processDefinitionId == null) {
            throw new APIException("Attribute '" + PROCESS_DEFINITION_ID + "' is mandatory");
        }
        return Long.parseLong(processDefinitionId);
    }
}
