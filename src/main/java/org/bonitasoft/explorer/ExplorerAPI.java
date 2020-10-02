package org.bonitasoft.explorer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.json.simple.JSONValue;

public class ExplorerAPI {

    static Logger logger = Logger.getLogger(ExplorerAPI.class.getName());

    public static class Parameter {

        public APISession apiSession;

        public boolean searchActive;
        public boolean searchArchive;
        public boolean searchExternal;
        public Integer searchYear;
        public String searchText;
        public Long searchCaseId;
        public String searchProcessName;
        public Long searchStartDateBeg;
        public Long searchStartDateEnd;
        public Long searchEndedDateBeg;
        public Long searchEndedDateEnd;
        public int caseperpages;
        public String orderby;
        public Order orderdirection;

        public static Parameter getInstanceFromJson(String jsonSt, APISession apiSession) {
            Parameter parameter = new Parameter();
            parameter.apiSession = apiSession;
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
                parameter.searchStartDateBeg = TypesCast.getHtml5ToLongDate( (String) information.get("startdatebeg"), null);
                parameter.searchStartDateEnd = TypesCast.getHtml5ToLongDate( (String) information.get("startdateend"), null);
                parameter.searchEndedDateBeg = TypesCast.getHtml5ToLongDate( (String) information.get("enddatebeg"), null);
                parameter.searchEndedDateEnd = TypesCast.getHtml5ToLongDate( (String) information.get("enddateend"), null);

                parameter.caseperpages = TypesCast.getInteger(information.get("caseperpages"), 25);
                parameter.orderby = TypesCast.getStringNullIsEmpty(information.get("orderby"), ExplorerCase.JSON_CASEID);
                String orderDirectionSt = TypesCast.getStringNullIsEmpty(information.get("orderdirection"), null);
                if ("asc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.ASC;
                else if ("desc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.DESC;
                else
                    parameter.orderdirection = Order.ASC;

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                logger.severe("Parameter: ~~~~~~~~~~  : ERROR " + e + " at " + exceptionDetails);
            }
            return parameter;
        }

    }

    public static ExplorerAPI getInstance() {
        return new ExplorerAPI();
    }

    public Map<String, Object> searchCases(Parameter parameter,  PageResourceProvider pageResourceProvider, ProcessAPI processAPI, IdentityAPI identityAPI) {
        ExplorerCase explorerCase = new ExplorerCase();
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        return explorerCase.searchCases(parameter, explorerParameters, processAPI, identityAPI).toMap();
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Parameters */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> saveParameters(Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider, ProcessAPI processAPI, IdentityAPI identityAPI) {
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
    public Map<String, Object> loadParameters(Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider, ProcessAPI processAPI, IdentityAPI identityAPI) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        List<BEvent> listEvents = explorerParameters.load(true);

        Map<String, Object> results = new HashMap<>();
        results.putAll(explorerParameters.getParameters());
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }
}
