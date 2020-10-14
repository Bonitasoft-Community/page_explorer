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
import org.bonitasoft.explorer.bonita.BonitaAccessSQL;
import org.bonitasoft.explorer.external.ExternalAccess;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;

public class ExplorerCase {

  
    
    public static class ExplorerCaseResult {

        public List<BEvent> listEvents = new ArrayList<>();

        public List<Map<String, Object>> listCases = new ArrayList<>();
        public List<Map<String, Object>> externalcase = new ArrayList<>();
        public List<Map<String, Object>> listTasks = new ArrayList<>();
        public List<Map<String, Object>> listComments = new ArrayList<>();
        private Map<String, Long> chronos = new HashMap<>();
        private long totalChronos=0;
        
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
        public void addChronometer(String name, long time ) {
            chronos.put(name, time);
            totalChronos+=time;
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
            information.put("chronos", chronos);
            information.put("totalchronos", totalChronos);
            return information;
        }

        public Map<String, Object> getExternalCase(long caseId) {
             // search the process instance
            for (Map<String, Object> processVariable : externalcase) {
                if (processVariable.get( ExplorerJson.JSON_CASEID).equals(caseId))
                    return (Map<String, Object>) processVariable;
            }
            // not found : create one
            Map<String, Object> processVariable = new HashMap<>();
            processVariable.put( ExplorerJson.JSON_CASEID, caseId);
            processVariable.put( ExplorerJson.JSON_VARIABLES, new ArrayList<>());
            processVariable.put( ExplorerJson.JSON_DOCUMENTS, new ArrayList<>());
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
            BonitaAccessSQL bonitaAccessSQL = new BonitaAccessSQL();
            long beginTime = System.currentTimeMillis();
            ExplorerCaseResult explorerExternal = bonitaAccessSQL.searchCases(parameter, isActive, explorerParameters);
            explorerCaseResult.addChronometer(isActive? "active": "archive", System.currentTimeMillis()-beginTime);
            explorerCaseResult.add(explorerExternal);
        }

        if (parameter.searchExternal) {
            ExternalAccess externalAccess = new ExternalAccess();

            explorerCaseResult.listEvents.addAll(explorerParameters.load(false));
            long beginTime = System.currentTimeMillis();
            ExplorerCaseResult explorerExternal = externalAccess.searchCases(explorerParameters.getExternalDataSource(), parameter);
            explorerCaseResult.addChronometer("external", System.currentTimeMillis()-beginTime);
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
        
        if ( ExplorerJson.JSON_SCOPE_V_EXTERNAL.equals(parameter.scope)) {
            explorerCaseResult = externalAccess.loadCase(explorerParameters.getExternalDataSource(), parameter);
        } else {
            BonitaAccessSQL bonitaAccess = new BonitaAccessSQL();
            ExplorerCaseResult explorerExternal = bonitaAccess.loadCommentsCase(parameter);
            explorerCaseResult.add(explorerExternal);
            if (parameter.isUserAdmin()) {
                explorerExternal = bonitaAccess.loadTasksCase(parameter);
                explorerCaseResult.add(explorerExternal);
            }
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
