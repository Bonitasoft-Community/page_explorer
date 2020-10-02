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
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfos;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.external.DatabaseQuery;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.quartz.impl.matchers.StringMatcher.StringOperatorName;

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceUpdater;

public class ExplorerCase {

    public static final String JSON_STARTBYNAME = "startbyname";
    public static final String JSON_ENDDATEST = "enddatest";
    public static final String JSON_SCOPE = "scope";
    public static final String JSON_PROCESSVERSION = "processversion";
    public static final String JSON_PROCESSNAME = "processname";
    public static final String JSON_STARTDATEST = "startdatest";
    public static final String JSON_STARTDATE = "startdate";
    public static final String JSON_ENDDATE = "enddate";
    public static final String JSON_CASEID = "caseid";
    public static final String JSON_PROCESSDEFINITIONID = "processid";

    public static final String JSON_STRINGINDEX5 = "stringindex5";


    public static final String JSON_STRINGINDEX4 = "stringindex4";


    public static final String JSON_STRINGINDEX3 = "stringindex3";


    public static final String JSON_STRINGINDEX2 = "stringindex2";


    public static final String JSON_STRINGINDEX1 = "stringindex1";
    
    
    private final static BEvent eventSearchActiveCase = new BEvent(ExplorerCase.class.getName(), 1, Level.ERROR,
            "Error during Archive search", "Error when the search failed", "The search failed", "Check the exception");
    private final static BEvent eventSearchArchivedCase = new BEvent(ExplorerCase.class.getName(), 2, Level.ERROR,
            "Error during Archive search", "Error when the search failed", "The search failed", "Check the exception");

    
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public static class ExplorerCaseResult {

        public List<BEvent> listEvents = new ArrayList<>();

        public List<Map<String, Object>> listCases = new ArrayList<>();

        public int totalNumberOfResult=0;
        public int firstrecord=0;
        public int lastrecord=0;
        
        public Map<String, Object> toMap() {
            Map<String, Object> information = new HashMap<>();
            if (!listEvents.isEmpty())
                information.put("listevents", BEventFactory.getSyntheticHtml(listEvents));
            information.put("list", listCases);
            information.put("count", totalNumberOfResult);
            information.put("first", firstrecord);
            information.put("last", lastrecord);
            
            return information;
        }
    } // end Case Result

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult searchCases(Parameter parameter, ExplorerParameters explorerParameters, ProcessAPI processAPI, IdentityAPI identityAPI) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();

        List<Boolean> listSearch = new ArrayList<>();
        if (parameter.searchActive) 
            listSearch.add( true );
        if (parameter.searchArchive) 
            listSearch.add( false );
            
        // build search now
        for (Boolean isActive : listSearch) {
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, parameter.caseperpages);

            if (parameter.searchYear != null && parameter.searchStartDateBeg == null)
                parameter.searchStartDateBeg = TypesCast.getLongDateFromYear(parameter.searchYear);
            if (parameter.searchYear != null && parameter.searchStartDateEnd == null)
                parameter.searchStartDateEnd = TypesCast.getLongDateFromYear(parameter.searchYear + 1);

            if (parameter.searchText != null) {
                sob.leftParenthesis();
                sob.filter( getAttributDescriptor(JSON_STRINGINDEX1, isActive), parameter.searchText);
                sob.or();
                sob.filter( getAttributDescriptor(JSON_STRINGINDEX2, isActive), parameter.searchText);
                sob.or();
                sob.filter( getAttributDescriptor(JSON_STRINGINDEX3, isActive), parameter.searchText);
                sob.or();
                sob.filter( getAttributDescriptor(JSON_STRINGINDEX4, isActive), parameter.searchText);
                sob.or();
                sob.filter( getAttributDescriptor(JSON_STRINGINDEX5, isActive), parameter.searchText);
                sob.rightParenthesis();
            }

            if (parameter.searchCaseId != null)
                sob.filter( getAttributDescriptor(JSON_CASEID, isActive), parameter.searchCaseId);
                
            if (parameter.searchProcessName != null) {
                // calculate all process with this process name
                List<Long> listProcessDefinition = getListProcess(parameter.searchProcessName, processAPI);
                completeSob(sob, listProcessDefinition, getAttributDescriptor(JSON_PROCESSDEFINITIONID, isActive));
            }
            if (parameter.searchStartDateBeg != null) {
                sob.greaterOrEquals( getAttributDescriptor(JSON_STARTDATE, isActive), parameter.searchStartDateBeg);
            }
            if (parameter.searchStartDateEnd != null) {
                sob.lessOrEquals( getAttributDescriptor(JSON_STARTDATE, isActive), parameter.searchStartDateEnd);
            }
            if (! isActive && parameter.searchEndedDateBeg != null) {
                sob.greaterOrEquals( getAttributDescriptor(JSON_ENDDATE, isActive),  parameter.searchStartDateEnd);
            }
            if (! isActive && parameter.searchEndedDateEnd != null) {
                sob.lessOrEquals( getAttributDescriptor(JSON_ENDDATE, isActive), parameter.searchStartDateEnd);
            }
            if (parameter.orderby!=null) {
                sob.sort( getAttributDescriptor(parameter.orderby, isActive), parameter.orderdirection);
            }
            /** and active case does not have a end */
            try {
                if (isActive) {
                    SearchResult<ProcessInstance> searchProcess = processAPI.searchProcessInstances(sob.done());
                    explorerCaseResult.totalNumberOfResult += searchProcess.getCount();
                    for (ProcessInstance processInstance : searchProcess.getResult()) {
                        explorerCaseResult.listCases.add(getFromProcessInstance(processInstance, processAPI, identityAPI));
                    }
                }
                else {
                    SearchResult<ArchivedProcessInstance> searchProcess = processAPI.searchArchivedProcessInstances(sob.done());
                    explorerCaseResult.totalNumberOfResult += searchProcess.getCount();
                    
                    for (ArchivedProcessInstance processInstance : searchProcess.getResult()) {
                        explorerCaseResult.listCases.add(getFromArchivedProcessInstance(processInstance, processAPI, identityAPI));
                    }
            
                }
                    
            } catch (Exception e) {
                explorerCaseResult.listEvents.add(new BEvent(eventSearchActiveCase, e, "Exception " + e.getMessage()));

            }

        }
        
        /*
         search in active ? 
        if (parameter.searchActive) {
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, parameter.caseperpages);

            if (parameter.searchYear != null && parameter.searchStartDateBeg == null)
                parameter.searchStartDateBeg = Long.valueOf(parameter.searchYear);
            if (parameter.searchYear != null && parameter.searchStartDateEnd == null)
                parameter.searchStartDateEnd = Long.valueOf(parameter.searchYear + 1);

           

            if (parameter.searchCaseId != null)
                sob.filter(ProcessInstanceSearchDescriptor.ID, parameter.searchCaseId);
            if (parameter.searchProcessName != null) {
                // calculate all process with this process name
                List<Long> listProcessDefinition = getListProcess(parameter.searchProcessName, processAPI);
                completeSob(sob, listProcessDefinition, ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID);
            }
            if (parameter.searchStartDateBeg != null) {
                sob.greaterOrEquals(ProcessInstanceSearchDescriptor.START_DATE, parameter.searchStartDateBeg);
            }
            if (parameter.searchStartDateEnd != null) {
                sob.lessOrEquals(ProcessInstanceSearchDescriptor.START_DATE, parameter.searchStartDateEnd);
            }
            
            if (parameter.orderby!=null) {
                Order order = Order.ASC;
                if ("asc".equalsIgnoreCase(parameter.orderdirection))
                    order = Order.ASC;
                if ("desc".equalsIgnoreCase(parameter.orderdirection))
                    order = Order.DESC;
            
                sob.sort(getAttributDescriptor( parameter.orderby, false), order);
            }
            * and active case does not have a end *
            try {
                SearchResult<ProcessInstance> searchProcess = processAPI.searchProcessInstances(sob.done());
                explorerCaseResult.totalNumberOfResult += searchProcess.getCount();
                for (ProcessInstance processInstance : searchProcess.getResult()) {
                    explorerCaseResult.listCases.add(getFromProcessInstance(processInstance, processAPI, identityAPI));
                }
            } catch (Exception e) {
                explorerCaseResult.listEvents.add(new BEvent(eventSearchActiveCase, e, "Exception " + e.getMessage()));

            }
        } // end active

        // it's very close... but not the same :-(        
        if (parameter.searchArchive) {
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, parameter.caseperpages);
            if (parameter.searchYear != null && parameter.searchStartDateBeg == null)
                parameter.searchStartDateBeg = Long.valueOf(parameter.searchYear);
            if (parameter.searchYear != null && parameter.searchStartDateEnd == null)
                parameter.searchStartDateEnd = Long.valueOf(parameter.searchYear + 1);

            // the archive search descriptor does not define 
            if (parameter.searchText != null) {
                sob.leftParenthesis();
                sob.filter(ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_1, parameter.searchText);
                sob.or();
                sob.filter(ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_2, parameter.searchText);
                sob.or();
                sob.filter(ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_3, parameter.searchText);
                sob.or();
                sob.filter(ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_4, parameter.searchText);
                sob.or();
                sob.filter(ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_5, parameter.searchText);
                sob.rightParenthesis();
            }

            if (parameter.searchCaseId != null)
                sob.filter(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, parameter.searchCaseId);
            if (parameter.searchProcessName != null) {
                // calculate all process with this process name
                List<Long> listProcessDefinition = getListProcess(parameter.searchProcessName, processAPI);
                completeSob(sob, listProcessDefinition, ArchivedProcessInstancesSearchDescriptor.PROCESS_DEFINITION_ID);
            }
            if (parameter.searchStartDateBeg != null) {
                sob.greaterOrEquals(ArchivedProcessInstancesSearchDescriptor.START_DATE, parameter.searchStartDateBeg);
            }
            if (parameter.searchStartDateEnd != null) {
                sob.lessOrEquals(ArchivedProcessInstancesSearchDescriptor.START_DATE, parameter.searchStartDateEnd);
            }
            if (parameter.searchEndedDateBeg != null) {
                sob.greaterOrEquals(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, parameter.searchStartDateEnd);
            }
            if (parameter.searchEndedDateEnd != null) {
                sob.lessOrEquals(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, parameter.searchStartDateEnd);
            }
            
            try {
                SearchResult<ArchivedProcessInstance> searchProcess = processAPI.searchArchivedProcessInstances(sob.done());
                explorerCaseResult.totalNumberOfResult += searchProcess.getCount();
                
                for (ArchivedProcessInstance processInstance : searchProcess.getResult()) {
                    explorerCaseResult.listCases.add(getFromArchivedProcessInstance(processInstance, processAPI, identityAPI));
                }
            } catch (SearchException e) {
                explorerCaseResult.listEvents.add(new BEvent(eventSearchArchivedCase, e, "Exception " + e.getMessage()));
            }
        }
*/
        
        if (parameter.searchExternal) {
            DatabaseQuery databaseQuery = new DatabaseQuery();
            
            explorerCaseResult.listEvents.addAll( explorerParameters.load( false ) );
            ExplorerCaseResult explorerExternal = databaseQuery.searchCases(explorerParameters.getExternalDataSource(), parameter, processAPI, identityAPI);
            explorerCaseResult.listCases.addAll( explorerExternal.listCases);
            explorerCaseResult.listEvents.addAll( explorerExternal.listEvents);
            explorerCaseResult.totalNumberOfResult += explorerExternal.totalNumberOfResult;
            
        }
        
        // so, we got potentially 3 times the size requested. So, now sort it, and get the first page requested
        
        Collections.sort(explorerCaseResult.listCases, new Comparator<Map<String, Object>>()
        {
          public int compare(Map<String, Object> s1,
                  Map<String, Object> s2)
          {
              Object o1 = s1.get(parameter.orderby);
              Object o2 = s2.get(parameter.orderby);
              int compareValue=0;
              if (o1==null && o2==null)
                  compareValue= 0;
              else if (o1==null)
                  compareValue = Integer.valueOf(0).compareTo(Integer.valueOf(1)) ;
              else if (o1 instanceof Integer)
                  compareValue = ((Integer) o1).compareTo((Integer) o2);
              else if (o1 instanceof Long)
                  compareValue = ((Long) o1).compareTo((Long) o2);
              else if (o1 instanceof Date)
                  compareValue = ((Date) o1).compareTo((Date) o2);
              else
                  compareValue = o1.toString().compareTo( o2==null ? "" : o2.toString());
              
              return Order.ASC.equals( parameter.orderdirection ) ? compareValue : - compareValue;
          }
        });

        // get the first row
        if (explorerCaseResult.listCases.size() > parameter.caseperpages)
            explorerCaseResult.listCases = explorerCaseResult.listCases.subList(0,  parameter.caseperpages);
        explorerCaseResult.firstrecord=1;
        explorerCaseResult.lastrecord = explorerCaseResult.listCases.size();
        return explorerCaseResult;

    }


    /**
     * 
     */
    private Map<String, Object> getFromProcessInstance(ProcessInstance processInstance, ProcessAPI processAPI, IdentityAPI identityAPI) {
        Map<String, Object> information = new HashMap<>();

        information.put( JSON_SCOPE, "active");

        information.put( JSON_CASEID, processInstance.getId());
        information.put( JSON_STARTDATE, getFromDate( processInstance.getStartDate()));
        information.put( JSON_STARTDATEST, sdf.format(processInstance.getStartDate()));

        ProcessDefinition processDefinition = getProcessDefinition(processInstance.getProcessDefinitionId(), processAPI);
        information.put( JSON_PROCESSNAME, processDefinition == null ? null : processDefinition.getName());
        information.put( JSON_PROCESSVERSION, processDefinition == null ? null : processDefinition.getVersion());
        information.put( JSON_PROCESSDEFINITIONID,processDefinition == null ? null : processDefinition.getId());
        
        User user = getUser(processInstance.getStartedBy(), identityAPI);
        information.put(JSON_STARTBYNAME, user == null ? null : user.getUserName());

        information.put( JSON_STRINGINDEX1, processInstance.getStringIndex1());
        information.put( JSON_STRINGINDEX2, processInstance.getStringIndex2());
        information.put( JSON_STRINGINDEX3, processInstance.getStringIndex3());
        information.put( JSON_STRINGINDEX4, processInstance.getStringIndex4());
        information.put( JSON_STRINGINDEX5, processInstance.getStringIndex5());

        return information;
    }

    private Map<String, Object> getFromArchivedProcessInstance(ArchivedProcessInstance archivedProcessInstance, ProcessAPI processAPI, IdentityAPI identityAPI) {
        Map<String, Object> information = new HashMap<>();
        information.put(JSON_SCOPE, "archive");
        information.put(JSON_CASEID, archivedProcessInstance.getSourceObjectId());
        information.put(JSON_STARTDATE, getFromDate( archivedProcessInstance.getStartDate()));
        information.put(JSON_STARTDATEST, sdf.format(archivedProcessInstance.getStartDate()));
        information.put(JSON_ENDDATE, getFromDate( archivedProcessInstance.getArchiveDate()));
        information.put(JSON_ENDDATEST, sdf.format(archivedProcessInstance.getArchiveDate()));

        ProcessDefinition processDefinition = getProcessDefinition(archivedProcessInstance.getProcessDefinitionId(), processAPI);
        information.put(JSON_PROCESSNAME, processDefinition == null ? null : processDefinition.getName());
        information.put(JSON_PROCESSVERSION, processDefinition == null ? null : processDefinition.getVersion());

        User user = getUser(archivedProcessInstance.getStartedBy(), identityAPI);
        information.put(JSON_STARTBYNAME, user == null ? null : user.getUserName());

        information.put(JSON_STRINGINDEX1, archivedProcessInstance.getStringIndexValue(1));
        information.put(JSON_STRINGINDEX2, archivedProcessInstance.getStringIndexValue(2));
        information.put(JSON_STRINGINDEX3, archivedProcessInstance.getStringIndexValue(3));
        information.put(JSON_STRINGINDEX4, archivedProcessInstance.getStringIndexValue(4));
        information.put(JSON_STRINGINDEX5, archivedProcessInstance.getStringIndexValue(5));

        return information;
    }

    /**
     * 
     */
    private Map<Long, ProcessDefinition> cacheProcessDefinition = new HashMap<>();

    private ProcessDefinition getProcessDefinition(Long processDefinitionId, ProcessAPI processAPI) {
        if (processDefinitionId == null)
            return null;
        try {
            if (cacheProcessDefinition.containsKey(processDefinitionId))
                return cacheProcessDefinition.get(processDefinitionId);
            ProcessDefinition processDefinition = processAPI.getProcessDefinition(processDefinitionId);
            cacheProcessDefinition.put(processDefinitionId, processDefinition);
            return processDefinition;
        } catch (Exception e) {
            // the ID come from the API, can't be here
            return null;
        }
    }

    /**
     * 
     */
    private Map<Long, User> cacheUsers = new HashMap<>();

    private User getUser(Long userId, IdentityAPI identityAPI) {
        if (userId == null || userId <= 0)
            return null;
        try {
            if (cacheUsers.containsKey(userId))
                return cacheUsers.get(userId);
            User processDefinition = identityAPI.getUser(userId);
            cacheUsers.put(userId, processDefinition);
            return processDefinition;
        } catch (Exception e) {
            // the ID come from the API, can't be here
            return null;
        }
    }

    /**
     * @param processName
     * @param processAPI
     * @return
     */
    private List<Long> getListProcess(String processName, ProcessAPI processAPI) {
        List<Long> listProcessDefinition = new ArrayList<>();
        return listProcessDefinition;

    }

    /**
     * @param sob
     * @param listProcessDefinition
     * @param attributName
     */
    private void completeSob(SearchOptionsBuilder sob, List<Long> listProcessDefinition, String attributName) {
        if (listProcessDefinition.isEmpty())
            return;
        sob.leftParenthesis();
        for (int i = 0; i < listProcessDefinition.size(); i++) {
            if (i > 0)
                sob.or();
            sob.filter(attributName, listProcessDefinition.get(i));
        }
        sob.rightParenthesis();
    }

    
    private String getAttributDescriptor( String name, boolean isActive) {
        if (JSON_STRINGINDEX1.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_1;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_1;
        }
        if (JSON_STRINGINDEX2.equals(isActive)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_2;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_2;
        }
        if (JSON_STRINGINDEX3.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_3;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_3;
        }
        if (JSON_STRINGINDEX4.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_4;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_4;
        }
        if (JSON_STRINGINDEX5.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_5;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_5;
        }
        if ( JSON_CASEID.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.ID;
            else
                return ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID;
        }

        if ( JSON_PROCESSDEFINITIONID.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID;
            else
                return ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID;
        }
        
        if ( JSON_STARTDATE.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.START_DATE;
            else
                return ArchivedProcessInstancesSearchDescriptor.START_DATE;
        }
        if ( JSON_ENDDATE.equals(name)) {
            if (isActive)
                return null;
            else
                return ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE;
        }
        return null;
        
    }
    
    private Long getFromDate( Date date) {
        if (date==null)
            return null;
        return date.getTime();
    }
}
