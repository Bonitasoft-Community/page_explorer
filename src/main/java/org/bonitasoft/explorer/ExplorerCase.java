package org.bonitasoft.explorer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.bonita.BonitaAccess;
import org.bonitasoft.explorer.external.ExternalAccess;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;

public class ExplorerCase {

    public static final String JSON_STARTEDBYNAME = "startedbyname";            
    public static final String JSON_STARTEDBYSUBSTITUTENAME = "startedbysubstitutename";            
    public static final String JSON_TYPEPROCESSINSTANCE="typeprocessinstance";
    public static final String JSON_TYPEPROCESSINSTANCE_V_SUBPROCESS = "subprocess";
    public static final String JSON_TYPEPROCESSINSTANCE_V_ROOT = "root";
    
    public static final String JSON_VARIABLES = "variables";
    public static final String JSON_PROCESSINSTANCE = "processinstance";
    public static final String JSON_NAME = "name";
    public static final String JSON_DISPLAYNAME ="displayname";
    public static final String JSON_DISPLAYDESCRIPTION ="displaydescription";
    public static final String JSON_BDMNAME = "bdmname";
    public static final String JSON_BDMISMULTIPLE = "bdmismultiple";
    public static final String JSON_CLASSNAME = "classname";
    public static final String JSON_VALUE = "value";
    public static final String JSON_TYPEVARIABLE = "type";
    public static final String JSON_TYPEVARIABLE_V_STRING="string";
    public static final String JSON_TYPEVARIABLE_V_DATE = "date";
    public static final String JSON_TYPEVARIABLE_V_NUMBER = "number";
    public static final String JSON_TYPEVARIABLE_V_LIST = "list";
    public static final String JSON_TYPEVARIABLE_V_DOC = "doc";
    
    public static final String JSON_STARTBYNAME = "startbyname";
    public static final String JSON_SCOPE = "scope";
    public static final String JSON_SCOPE_V_EXTERNAL = "external";
    public static final String JSON_SCOPE_V_ACTIVE = "active";
    public static final String JSON_SCOPE_V_ARCHIVE = "archive";
    
    public static final String JSON_PROCESSVERSION = "processversion";
    public static final String JSON_PROCESSNAME = "processname";
    public static final String JSON_STARTDATE = "startdate";
    public static final String JSON_STARTDATEST = "startdatest";
    public static final String JSON_ENDDATE = "enddate";
    public static final String JSON_ENDDATEST = "enddatest";
    public static final String JSON_CASEID = "caseid";
    public static final String JSON_PROCESSDEFINITIONID = "processid";

    public static final String JSON_STRINGINDEX1 = "stringindex1";
    public static final String JSON_STRINGINDEX2 = "stringindex2";
    public static final String JSON_STRINGINDEX3 = "stringindex3";
    public static final String JSON_STRINGINDEX4 = "stringindex4";
    public static final String JSON_STRINGINDEX5 = "stringindex5";

    public static final String JSON_URLOVERVIEW = "urloverview";

    
    
    public static final String JSON_DOCUMENTS = "documents";
    public static final String JSON_VERSION = "version";
    public static final String JSON_ID  = "id";
    public static final String JSON_KIND ="kind";
    public static final String JSON_AUTHOR ="author";
    public static final String JSON_FILENAME="filename";
    public static final String JSON_MIMETYPE="mimetype";
    public static final String JSON_URL ="url";
    public static final String JSON_HASCONTENT ="hascontent";
    public static final String JSON_EXECUTEDBY = "executedby";
    public static final String JSON_EXECUTEDBYNAME = "executedbnyname";
    public static final String JSON_EXECUTEDBYSUBSTITUTE = "executedby";
    public static final String JSON_EXECUTEDBYSUBSTITUTENAME= "executedbyname";
    public static final String JSON_USERBY = "userby";
    public static final String JSON_USERBYNAME ="userbyname";
    
    public static final String JSON_CONTENT ="content";
    
    
    public static class ExplorerCaseResult {

        public List<BEvent> listEvents = new ArrayList<>();

        public List<Map<String, Object>> listCases = new ArrayList<>();
        public List<Map<String, Object>> externalcase = new ArrayList<>();
        public List<Map<String, Object>> listTasks = new ArrayList<>();
        public List<Map<String, Object>> listComments = new ArrayList<>();

        public int totalNumberOfResult = 0;
        public int firstrecord = 0;
        public int lastrecord = 0;

        public void add( ExplorerCaseResult explorerExternal ) {
            listCases.addAll(explorerExternal.listCases);
            listTasks.addAll( explorerExternal.listTasks);
            listComments.addAll( explorerExternal.listComments);
            listEvents.addAll(explorerExternal.listEvents);
            totalNumberOfResult += explorerExternal.totalNumberOfResult;
        }
        public Map<String, Object> toMap() {
            Map<String, Object> information = new HashMap<>();
            if (!listEvents.isEmpty())
                information.put("listevents", BEventFactory.getSyntheticHtml(listEvents));
            information.put("list", listCases);
            information.put("count", totalNumberOfResult);
            information.put("first", firstrecord);
            information.put("last", lastrecord);
            information.put("externalcase", externalcase);
            information.put("tasks", listTasks);
            information.put("comments", listComments);
            return information;
        }

        public Map<String, Object> getExternalCase(long caseId) {
             // search the process instance
            for (Map<String, Object> processVariable : externalcase) {
                if (processVariable.get(ExplorerCase.JSON_CASEID).equals(caseId))
                    return (Map<String, Object>) processVariable;
            }
            // not found : create one
            Map<String, Object> processVariable = new HashMap<>();
            processVariable.put(ExplorerCase.JSON_CASEID, caseId);
            processVariable.put(ExplorerCase.JSON_VARIABLES, new ArrayList<>());
            processVariable.put(ExplorerCase.JSON_DOCUMENTS, new ArrayList<>());
            externalcase.add(processVariable);
            return (Map<String, Object>) processVariable;
        }

    } // end Case Result

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult searchCases(Parameter parameter, ExplorerParameters explorerParameters) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();

        List<Boolean> listSearch = new ArrayList<>();
        if (parameter.searchActive)
            listSearch.add(true);
        if (parameter.searchArchive)
            listSearch.add(false);

        // build search now
        for (Boolean isActive : listSearch) {
            BonitaAccess bonitaAccess = new BonitaAccess();
            ExplorerCaseResult explorerExternal = bonitaAccess.searchCases(parameter, isActive, explorerParameters);
            explorerCaseResult.add(explorerExternal);
        }

        if (parameter.searchExternal) {
            ExternalAccess externalAccess = new ExternalAccess();

            explorerCaseResult.listEvents.addAll(explorerParameters.load(false));
            ExplorerCaseResult explorerExternal = externalAccess.searchCases(explorerParameters.getExternalDataSource(), parameter);
            explorerCaseResult.add(explorerExternal);
        }

        // so, we got potentially 3 times the size requested. So, now sort it, and get the first page requested

        Collections.sort(explorerCaseResult.listCases, new Comparator<Map<String, Object>>() {

            public int compare(Map<String, Object> s1,
                    Map<String, Object> s2) {
                Object o1 = s1.get(parameter.orderby);
                Object o2 = s2.get(parameter.orderby);
                int compareValue = 0;
                if (o1 == null && o2 == null)
                    compareValue = 0;
                else if (o1 == null)
                    compareValue = Integer.valueOf(0).compareTo(Integer.valueOf(1));
                else if (o1 instanceof Integer)
                    compareValue = ((Integer) o1).compareTo((Integer) o2);
                else if (o1 instanceof Long)
                    compareValue = ((Long) o1).compareTo((Long) o2);
                else if (o1 instanceof Date)
                    compareValue = ((Date) o1).compareTo((Date) o2);
                else
                    compareValue = o1.toString().compareTo(o2 == null ? "" : o2.toString());

                return Order.ASC.equals(parameter.orderdirection) ? compareValue : -compareValue;
            }
        });

        // get the first row
        if (explorerCaseResult.listCases.size() > parameter.caseperpages)
            explorerCaseResult.listCases = explorerCaseResult.listCases.subList(0, parameter.caseperpages);
        explorerCaseResult.firstrecord = 1;
        explorerCaseResult.lastrecord = explorerCaseResult.listCases.size();
        return explorerCaseResult;

    }

    /**
     * Load a case, external or not
     * @param parameter
     * @param explorerParameters
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public ExplorerCaseResult loadCase(Parameter parameter, ExplorerParameters explorerParameters) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();
        explorerCaseResult.listEvents.addAll(explorerParameters.load(false));
        if (BEventFactory.isError(explorerCaseResult.listEvents))
            return explorerCaseResult;
        ExternalAccess externalAccess = new ExternalAccess();
        
        if (JSON_SCOPE_V_EXTERNAL.equals(parameter.scope)) {
            explorerCaseResult = externalAccess.loadCase(explorerParameters.getExternalDataSource(), parameter);
        } else if (parameter.isUserAdmin()) {
            BonitaAccess bonitaAccess = new BonitaAccess();
            ExplorerCaseResult explorerExternal = bonitaAccess.loadTasksCase(parameter);
            explorerCaseResult.add(explorerExternal);
            explorerExternal = bonitaAccess.loadCommentsCase(parameter);
            explorerCaseResult.add(explorerExternal);
        }
        // load task and comment if the user is an admin
        return explorerCaseResult;
    }


    private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public static Long getFromDate(Date date) {
        if (date == null)
            return null;
        return date.getTime();
    }
    
    public static String getFromDateString(Long date) {
        if (date == null)
            return null;
        return ExplorerCase.sdf.format( date );
    }  
    
    public static String getFromDateString(Date date) {
        if (date == null)
            return null;
        return ExplorerCase.sdf.format( date.getTime() );
    }  

}
