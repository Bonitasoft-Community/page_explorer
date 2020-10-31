package org.bonitasoft.explorer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileCriterion;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.explorer.external.DatabaseDefinition;
import org.bonitasoft.explorer.external.ExternalAccess;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.web.extension.page.PageResourceProvider;
import org.json.simple.JSONValue;



public class ExplorerAPI {

    static Logger logger = Logger.getLogger(ExplorerAPI.class.getName());

    private static BEvent eventErrorLoadDocExternal = new BEvent(ExplorerAPI.class.getName(), 1, Level.ERROR,
            "Error during download External Document", "An error arrived during a download of an external document", "Download failed", "Check the exception");

    private final static BEvent eventSaveFilterError = new BEvent(ExplorerAPI.class.getName(), 1, Level.ERROR,
            "Save Filter failed", "Error during save Filters", "Filters are not saved", "Check the exception");
    
    private final static BEvent eventSaveFilterOk = new BEvent(ExplorerAPI.class.getName(), 2, Level.SUCCESS,
            "Filters saved", "Filters saved with success");

    private final static BEvent eventSearchProcessError = new BEvent(ExplorerAPI.class.getName(), 3, Level.ERROR,
            "Search all processes failed", "Error during search processes", "Process list will be empty", "Check the exception");
    

     public static class Parameter {

              public APISession apiSession;
        public ProcessAPI processAPI;
        public IdentityAPI identityAPI;
        public ProfileAPI profileAPI;

        public long tenantId;
        public String visibility;
        public boolean searchActive;
        public boolean searchArchive;
        public boolean searchExternal;
        public String searchText;
        public Long searchCaseId;
        public String searchProcessName;
        public Long searchStartDateFrom;
        public Long searchStartDateTo;
        public Long searchEndedDateFrom;
        public Long searchEndedDateTo;
        public int caseperpages;
        public int pagenumber;
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

                parameter.visibility = TypesCast.getStringNullIsEmpty(information.get( ExplorerJson.JSON_VISIBILITY), "user");
                parameter.searchActive = TypesCast.getBoolean(information.get( ExplorerJson.JSON_SCOPE_V_OPENCASE), true);
                parameter.searchArchive = TypesCast.getBoolean(information.get( ExplorerJson.JSON_SCOPE_V_ARCHIVEDCASE), true);
                parameter.searchExternal = TypesCast.getBoolean(information.get( ExplorerJson.JSON_SCOPE_V_EXTERNALCASE), true);
                parameter.searchText = TypesCast.getStringNullIsEmpty(information.get( ExplorerJson.JSON_SEARCHKEY), null);
                parameter.searchCaseId = TypesCast.getLong(information.get( ExplorerJson.JSON_CASEID), null);
                if ( information.get( ExplorerJson.JSON_PROCESSNAME) instanceof Map) {
                    parameter.searchProcessName  =  TypesCast.getStringNullIsEmpty( ((Map)information.get( ExplorerJson.JSON_PROCESSNAME)).get("id"), null);
                } else
                    parameter.searchProcessName = TypesCast.getStringNullIsEmpty(information.get( ExplorerJson.JSON_PROCESSNAME), null);
                // date format : 2020-10-01T00:33:00.000Z or a Long (saved JSON)
                parameter.searchStartDateFrom = TypesCast.getHtml5DateToLong( information.get(ExplorerJson.JSON_STARTDATEBEG), null);
                parameter.searchStartDateTo = TypesCast.getHtml5DateToLong( information.get(ExplorerJson.JSON_STARTDATEEND), null);
                parameter.searchEndedDateFrom = TypesCast.getHtml5DateToLong( information.get(ExplorerJson.JSON_ENDDATEBEG), null);
                parameter.searchEndedDateTo = TypesCast.getHtml5DateToLong( information.get(ExplorerJson.JSON_ENDDATEEND), null);

                parameter.pagenumber = TypesCast.getInteger(information.get( ExplorerJson.JSON_PAGENUMBER), 1);
                parameter.caseperpages = TypesCast.getInteger(information.get(ExplorerJson.JSON_CASEPERPAGES), 25);
                parameter.orderby = TypesCast.getStringNullIsEmpty(information.get(ExplorerJson.JSON_ORDERBY), ExplorerJson.JSON_CASEID);
                String orderDirectionSt = TypesCast.getStringNullIsEmpty(information.get(ExplorerJson.JSON_ORDERDIRECTION), null);
                if ("asc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.ASC;
                else if ("desc".equalsIgnoreCase(orderDirectionSt))
                    parameter.orderdirection = Order.DESC;
                else
                    parameter.orderdirection = Order.ASC;
                parameter.docId = TypesCast.getLong(information.get("docid"), null);
                parameter.scope = TypesCast.getStringNullIsEmpty(information.get(ExplorerJson.JSON_SCOPE), null);

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                logger.severe("Parameter: ~~~~~~~~~~  : ERROR " + e + " at " + exceptionDetails);
            }
            return parameter;
        }

        public Map<String, Object> getMap( boolean toTheBrowser) {
            Map<String, Object> information = new HashMap<>();
            information.put( ExplorerJson.JSON_VISIBILITY, visibility);
            information.put( ExplorerJson.JSON_SCOPE_V_OPENCASE, searchActive );
            information.put( ExplorerJson.JSON_SCOPE_V_ARCHIVEDCASE, searchArchive );
            information.put( ExplorerJson.JSON_SCOPE_V_EXTERNALCASE, searchExternal );
            information.put( ExplorerJson.JSON_SEARCHKEY, searchText);
            information.put( ExplorerJson.JSON_CASEID, searchCaseId );
            information.put( ExplorerJson.JSON_PROCESSNAME, searchProcessName );
            // date format : 2020-10-01T00:33:00.000Z
            information.put( ExplorerJson.JSON_STARTDATEBEG, toTheBrowser? TypesCast.getHtml5DateFromLong( searchStartDateFrom)  : searchStartDateFrom);
            information.put( ExplorerJson.JSON_STARTDATEEND, toTheBrowser?TypesCast.getHtml5DateFromLong(searchStartDateTo)  : searchStartDateTo);
            information.put( ExplorerJson.JSON_ENDDATEBEG, toTheBrowser?TypesCast.getHtml5DateFromLong(searchEndedDateFrom ) : searchEndedDateFrom);
            information.put( ExplorerJson.JSON_ENDDATEEND, toTheBrowser?TypesCast.getHtml5DateFromLong(searchEndedDateTo) : searchEndedDateTo);

            information.put( ExplorerJson.JSON_CASEPERPAGES, String.valueOf(caseperpages));
            information.put( ExplorerJson.JSON_ORDERBY, orderby );
            information.put( ExplorerJson.JSON_ORDERDIRECTION, orderdirection == Order.ASC? "asc" : "desc");
            information.put( ExplorerJson.JSON_SCOPE, scope);
            return information;
        }
        
        public String toJson( boolean toTheBrowser) {
            return JSONValue.toJSONString(getMap( toTheBrowser ));

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
        results.put("parameters",explorerParameters.getParameters());

        Map<String, Object> user = new HashMap<>();
        results.put("user", user);
        user.put("isadmin", parameter.isUserAdmin());
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Filter */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> saveFilter(Parameter parameter, Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        explorerParameters.setParameters(jsonParam);
        
        // save it
        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        bonitaProperties.setCheckDatabase(false); // already done at load
        List<BEvent> listEvents = new ArrayList<>();
        try {
            listEvents.addAll(bonitaProperties.loaddomainName( parameter.apiSession.getUserName()));
            
            bonitaProperties.setProperty( "searchfilter", parameter.toJson( false ));
            listEvents.addAll(bonitaProperties.store());
            listEvents.add(eventSaveFilterOk);
        } catch (Exception e) {
            logger.severe("Exception " + e.toString());
            listEvents.add(new BEvent(eventSaveFilterError, e, "Error :" + e.getMessage()));
        }
        Map<String, Object> results = new HashMap<>();
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }

    public Map<String, Object> loadFilter(Parameter parameter, Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        explorerParameters.setParameters(jsonParam);
        
        // save it
        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        bonitaProperties.setCheckDatabase(false); // already done at load
        List<BEvent> listEvents = new ArrayList<>();
        Map<String, Object> results = new HashMap<>();
        try {
            listEvents.addAll(bonitaProperties.loaddomainName( parameter.apiSession.getUserName()));
            String parametersString = bonitaProperties.getProperty( "searchfilter");
            Parameter parameterFilter = Parameter.getInstanceFromJson(parametersString, parameter.tenantId, parameter.apiSession, null, null, null);
            results.put("searchcases", parameterFilter.getMap( true ) );
            listEvents.add(eventSaveFilterOk);
        } catch (Exception e) {
            logger.severe("Exception " + e.toString());
            listEvents.add(new BEvent(eventSaveFilterError, e, "Error :" + e.getMessage()));
        }
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }

    
    public Map<String, Object> loadParametersAndFilter(Parameter parameter, Map<String, Object> jsonParam, PageResourceProvider pageResourceProvider) {
        ExplorerParameters explorerParameters = new ExplorerParameters(pageResourceProvider);
        List<BEvent> listEvents = explorerParameters.load(true);

        Map<String, Object> results = new HashMap<>();
        results.put("parameters",explorerParameters.getParameters());
        results.putAll(loadFilter( parameter, jsonParam, pageResourceProvider ));

        // load process name
        List<String> listProcessesName = new ArrayList<>();
        listProcessesName.add(""); // no filter
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0,1000);
        sob.sort(ProcessDeploymentInfoSearchDescriptor.NAME, Order.ASC);
        sob.sort(ProcessDeploymentInfoSearchDescriptor.VERSION, Order.ASC);
        
        SearchResult<ProcessDeploymentInfo> result;
        try {
            result = parameter.processAPI.searchProcessDeploymentInfos( sob.done());
            for (ProcessDeploymentInfo processInfo : result.getResult()) {
                // only the name
                listProcessesName.add( processInfo.getName());
            }
        } catch (SearchException e) {
            logger.severe("Can't search process info " +e.getMessage());
            listEvents.add( new BEvent(eventSearchProcessError, e, ""));
        }
        results.put("listprocessesname", listProcessesName);
        
        Map<String, Object> user = new HashMap<>();
        results.put("user", user);
        user.put("isadmin", parameter.isUserAdmin());
        results.put("listevents", BEventFactory.getHtml(listEvents));
        return results;
    }
}
