package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interface to Inter Domain protocol for Shongo domains
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface InterDomainProtocol {

    public DomainLogin handleLogin(HttpServletRequest request);
    /**
     *
     * @param request
     * @return status of local domain
     */
    public DomainStatus handleDomainStatus(HttpServletRequest request);

    public List<ResourceSummary> handleListResources(HttpServletRequest request);


    /**
     * Represents Inter Domain response with status code
     */
    public static class InterDomainResponse {
        private String responseType;

        private Object response;

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }
    }

    /**
     * Represents status for Inter Domain response
     */
    public static class Status {
        public static final String CODE_OK = "ok";
        public static final String CODE_UNAUTHORIZED = "unauthorized";

        private String code;

        private String message;

        public Status(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}