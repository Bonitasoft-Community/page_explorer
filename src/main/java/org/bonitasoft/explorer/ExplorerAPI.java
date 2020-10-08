package org.bonitasoft.explorer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileCriterion;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.explorer.external.DatabaseDefinition;
import org.bonitasoft.explorer.external.ExternalAccess;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.web.extension.page.PageResourceProvider;
import org.json.simple.JSONValue;

import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate.Param;

public class ExplorerAPI {

    static Logger logger = Logger.getLogger(ExplorerAPI.class.getName());

    private static BEvent eventErrorLoadDocExternal = new BEvent(ExplorerAPI.class.getName(), 1, Level.ERROR,
            "Error during download External Document", "An error arrived during a download of an external document", "Download failed", "Check the exception");

    public static class Parameter {

        public APISession apiSession;
        public ProcessAPI processAPI;
        public IdentityAPI identityAPI;
        public ProfileAPI profileAPI;

        public long tenantId;
        public boolean searchActive;
        public boolean searchArchive;
        public boolean searchExternal;
        public Integer searchYear;
        public String searchText;
        public Long searchCaseId;
        public String searchProcessName;
        public Long searchStartDateFrom;
        public Long searchStartDateTo;
        public Long searchEndedDateFrom;
        public Long searchEndedDateTo;
        public int caseperpages;
        public String orderby;
        public Order orderdirection;

        public Long docId;

        public String scope;
        private Boolean saveIsUserAdmin = null;

        @SuppressWarnings("unchecked")
        public static Parameter getInstanceFromJson(String jsonSt, long tenantId, APISession apiSession,
                ProcessAPI processAPI, IdentityAPI identityAPI, ProfileAPI profileAPI) {
            Parameter parameter = new Parameter();
            parameter.tenantId = tenantId;
            parameter.apiSession = apiSession;
            parameter.processAPI = processAPI;
            parameter.identityAPI = identityAPI;
            parameter.profileAPI = profileAPI;
            
            if (jsonSt == null)
                return parameter;
            try {
                Map<String, Object> information = (Map<String, Object>) JSONValue.parse(jsonSt);

                parameter.searchActive = TypesCast.getBoolean(information.get("active"), false);
                parameter.searchArchive = TypesCast.getBoolean(information.get("archive"), false);
                parameter.searchExternal = TypesCast.getBoolean(information.get("external"), false);
                parameter.searchYear = TypesCast.getInteger(information.get("year"), null);
                parameter.searchText = TypesCast.getStringNullIsEmpty(information.get("text"), null);
                parameter.searchCaseId = TypesCast.getLong(information.get("caseid"), null);
                parameter.searchProcessName = TypesCast.getStringNullIsEmpty(information.get("processname"), null);
                // date format : 2020-10-01T00:33:00.000Z
                parameter.searchStartDateFrom = TypesCast.getHtml5ToLongDate((String) information.get("startdatebeg"), null);
                parameter.searchStartDateTo = TypesCast.getHtml5ToLongDate((String) information.get("startdateend"), null);
                parameter.searchEndedDateFrom = TypesCast.getHtml5ToLongDate((String) information.get("enddatebeg"), null);
                parameter.searchEndedDateTo = TypesCast.getHtml5ToLongDate((String) information.get("enddateend"), null);

                parameter.caseperpages = TypesCast.getInteger(information.get("caseperpages"), 25);
                parameter.orderby = TypesCast.getStringNullIsEmpty(information.get("orderby"), ExplorerCase.JSON_CASEID);
                String orderDirectionSt = TypesCast.getStringNullIsEmpty(information.get("orderdirection"), null);
                if ("asc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.ASC;
                else if ("desc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.DESC;
                else
                    parameter.orderdirection = Order.ASC;
                parameter.docId = TypesCast.getLong(information.get("docid"), null);
                parameter.scope = TypesCast.getStringNullIsEmpty(information.get("scope"), null);

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                logger.severe("Parameter: ~~~~~~~~~~  : ERROR " + e + " at " + exceptionDetails);
            }
            return parameter;
        }

        public boolean isUserAdmin() {
            if (saveIsUserAdmin != null)
                return saveIsUserAdmin;
            saveIsUserAdmin = false;
            List<Profile> listProfiles = profileAPI.getProfilesForUser(apiSession.getUserId(), 0, 10000, ProfileCriterion.NAME_ASC);
            for (Profile profile : listProfiles) {
                if (profile.getName().equals("Administrator"))
                    saveIsUserAdmin = true;;
            }
            return saveIsUserAdmin;
        }

    }

    public static ExplorerAPI getInstance() {
        return new ExplorerAPI();
    }

    /**
     * @param parameter
     * @param pageResourceProvider
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public Map<String, Object> searchCases(Parameter parameter, PageResourceProvider pageResourceProvider) {
        ExplorerCase explorerCase = new ExplorerCase();
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        return explorerCase.searchCases(parameter, explorerParameters).toMap();
    }

    public Map<String, Object> loadCase(Parameter parameter, PageResourceProvider pageResourceProvider) {
        ExplorerCase explorerCase = new ExplorerCase();
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        return explorerCase.loadCase(parameter, explorerParameters).toMap();

    }

    /**
     * Download a document from the external database
     * 
     * @param parameter
     * @param response
     * @param pageResourceProvider
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public List<BEvent> downloadDocExternalAccess(Parameter parameter, HttpServletResponse response, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);

        List<BEvent> listEvents = explorerParameters.load(false);
        if (BEventFactory.isError(listEvents))
            return listEvents;

        // ATTENTION : on a Linux Tomcat, order is important : first, HEADER then CONTENT. on Windows Tomcat, don't care
        ExternalAccess externalAccess = new ExternalAccess();
        Map<String, Object> documentAttributes = externalAccess.loadDocument(explorerParameters.getExternalDataSource(), parameter);
        response.addHeader("content-type", (String) documentAttributes.get(DatabaseDefinition.BDE_DOCUMENTINSTANCE_MIMETYPE));
        response.addHeader("content-disposition", "attachment; filename=" + (String) documentAttributes.get(DatabaseDefinition.BDE_DOCUMENTINSTANCE_FILENAME));

        // now the core document
        try {
            OutputStream output = response.getOutputStream();
            externalAccess.loadDocumentOuput(output, explorerParameters.getExternalDataSource(), parameter);

            output.flush();
            output.close();
            return listEvents;
        } catch (Exception e) {
            logger.severe("Explorer: downloadDocument " + e.getMessage());
            listEvents.add(new BEvent(eventErrorLoadDocExternal, e, "DocId[" + parameter.docId + "]"));
            return listEvents;
        }
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Parameters */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> saveParameters(Parameter parameter, Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        explorerParameters.setParameters(jsonParam);
        List<BEvent> listEvents = explorerParameters.save();

        Map<String, Object> results = new HashMap<>();
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }

    /**
     * @param jsonParam
     * @param pageResourceProvider
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public Map<String, Object> loadParameters(Parameter parameter, Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        List<BEvent> listEvents = explorerParameters.load(true);

        Map<String, Object> results = new HashMap<>();
        results.putAll(explorerParameters.getParameters());

        Map<String, Object> user = new HashMap<>();
        results.put("user", user);
        user.put("isadmin", parameter.isUserAdmin());
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }
}
