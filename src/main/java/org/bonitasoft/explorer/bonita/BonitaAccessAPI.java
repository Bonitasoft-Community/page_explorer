package org.bonitasoft.explorer.bonita;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.casedetails.CaseDetails;
import org.bonitasoft.casedetails.CaseDetailsAPI;
import org.bonitasoft.casedetails.CaseDetails.CaseDetailComment;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.casedetails.CaseDetailsAPI.LOADCOMMENTS;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.explorer.ExplorerCase;
import org.bonitasoft.explorer.ExplorerParameters;
import org.bonitasoft.explorer.TypesCast;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.ExplorerCase.ExplorerCaseResult;
import org.bonitasoft.explorer.ExplorerJson;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import com.bonitasoft.engine.bpm.flownode.ArchivedProcessInstancesSearchDescriptor;
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;

public class BonitaAccessAPI {
    private final static BEvent eventSearchCase = new BEvent(BonitaAccessAPI.class.getName(), 1, Level.ERROR,
            "Error during case search", "A case is searched, the request failed", "The search failed", "Check the exception");
    private final static BEvent eventLoadTasksCase = new BEvent(BonitaAccessAPI.class.getName(), 2, Level.ERROR,
            "Error during load Task case", "Tasks are loaded from a specific case. The load failed", "Tasks are incompletes", "Check the exception");

    public ExplorerCaseResult searchCases(Parameter parameter, boolean isActive, ExplorerParameters explorerParameters) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult( parameter.isUserAdmin() );
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, parameter.caseperpages);

      
        if (parameter.searchText != null) {
            sob.leftParenthesis();
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_STRINGINDEX1, isActive), parameter.searchText);
            sob.or();
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_STRINGINDEX2, isActive), parameter.searchText);
            sob.or();
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_STRINGINDEX3, isActive), parameter.searchText);
            sob.or();
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_STRINGINDEX4, isActive), parameter.searchText);
            sob.or();
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_STRINGINDEX5, isActive), parameter.searchText);
            sob.rightParenthesis();
        }

        if (parameter.searchCaseId != null)
            sob.filter(getAttributDescriptor( ExplorerJson.JSON_CASEID, isActive), parameter.searchCaseId);

        if (parameter.searchProcessName != null) {
            // calculate all process with this process name
            List<Long> listProcessDefinition = getListProcess(parameter.searchProcessName, parameter.processAPI);
            completeSob(sob, listProcessDefinition, getAttributDescriptor( ExplorerJson.JSON_PROCESSDEFINITIONID, isActive));
        }
        if (parameter.searchStartDateFrom != null) {
            sob.greaterOrEquals(getAttributDescriptor( ExplorerJson.JSON_STARTDATE, isActive), parameter.searchStartDateFrom);
        }
        if (parameter.searchStartDateTo != null) {
            sob.lessOrEquals(getAttributDescriptor( ExplorerJson.JSON_STARTDATE, isActive), parameter.searchStartDateTo);
        }
        if (!isActive && parameter.searchEndedDateFrom != null) {
            sob.greaterOrEquals(getAttributDescriptor( ExplorerJson.JSON_ENDDATE, isActive), parameter.searchEndedDateFrom);
        }
        if (!isActive && parameter.searchEndedDateTo != null) {
            sob.lessOrEquals(getAttributDescriptor( ExplorerJson.JSON_ENDDATE, isActive), parameter.searchEndedDateTo);
        }
        if (parameter.orderby != null) {
            sob.sort(getAttributDescriptor(parameter.orderby, isActive), parameter.orderdirection);
        }
        /** and active case does not have a end */
        try {
            if (isActive) {
                SearchResult<ProcessInstance> searchProcess = parameter.processAPI.searchProcessInstances(sob.done());
                explorerCaseResult.totalNumberOfResult += searchProcess.getCount();
                for (ProcessInstance processInstance : searchProcess.getResult()) {
                    explorerCaseResult.listCases.add(getFromProcessInstance(processInstance, parameter.processAPI, parameter.identityAPI));
                }
            } else {
                SearchResult<ArchivedProcessInstance> searchProcess = parameter.processAPI.searchArchivedProcessInstances(sob.done());
                explorerCaseResult.totalNumberOfResult += searchProcess.getCount();

                for (ArchivedProcessInstance processInstance : searchProcess.getResult()) {
                    explorerCaseResult.listCases.add(getFromArchivedProcessInstance(processInstance, parameter.processAPI, parameter.identityAPI));
                }
            }
        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchCase, e, "Exception " + e.getMessage()));
        }
        return explorerCaseResult;
    }

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult loadTasksCase(Parameter parameter) {

        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin() );
        try {
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 1000);
            sob.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, parameter.searchCaseId);
            final SearchResult<FlowNodeInstance> searchFlowNode = parameter.processAPI.searchFlowNodeInstances(sob.done());

            for (final FlowNodeInstance activityInstance : searchFlowNode.getResult()) {
                Map<String, Object> taskContent = new HashMap<>();
                explorerCaseResult.listTasks.add(taskContent);
                taskContent.put( ExplorerJson.JSON_PROCESSINSTANCE, activityInstance.getParentProcessInstanceId());
                taskContent.put( ExplorerJson.JSON_NAME, activityInstance.getName());
                taskContent.put( ExplorerJson.JSON_DISPLAYNAME, activityInstance.getDisplayName());
                taskContent.put( ExplorerJson.JSON_DISPLAYDESCRIPTION, activityInstance.getDisplayDescription());
                taskContent.put( ExplorerJson.JSON_ID, activityInstance.getId());
                taskContent.put( ExplorerJson.JSON_KIND, activityInstance.getType().toString());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBY, activityInstance.getExecutedBy());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYNAME, getUser(activityInstance.getExecutedBy(), parameter.identityAPI));
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYSUBSTITUTE, activityInstance.getExecutedBySubstitute());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYSUBSTITUTENAME, getUser(activityInstance.getExecutedBySubstitute(), parameter.identityAPI));
                taskContent.put(ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(activityInstance.getLastUpdateDate()));
                taskContent.put(ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(activityInstance.getLastUpdateDate()));
            }
            // same in archive
            sob = new SearchOptionsBuilder(0, 1000);
            sob.filter(ArchivedFlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, parameter.searchCaseId);
            final SearchResult<ArchivedFlowNodeInstance> searchArchivedFlowNode = parameter.processAPI.searchArchivedFlowNodeInstances(sob.done());

            for (final ArchivedFlowNodeInstance archivedActivityInstance : searchArchivedFlowNode.getResult()) {
                Map<String, Object> taskContent = new HashMap<>();
                explorerCaseResult.listTasks.add(taskContent);
                taskContent.put( ExplorerJson.JSON_PROCESSINSTANCE, archivedActivityInstance.getSourceObjectId());
                taskContent.put( ExplorerJson.JSON_NAME, archivedActivityInstance.getName());
                taskContent.put( ExplorerJson.JSON_DISPLAYNAME, archivedActivityInstance.getDisplayName());
                taskContent.put( ExplorerJson.JSON_DISPLAYDESCRIPTION, archivedActivityInstance.getDisplayDescription());
                taskContent.put( ExplorerJson.JSON_ID, archivedActivityInstance.getId());
                taskContent.put( ExplorerJson.JSON_KIND, archivedActivityInstance.getType().toString());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBY, archivedActivityInstance.getExecutedBy());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYNAME, getUser(archivedActivityInstance.getExecutedBy(), parameter.identityAPI));
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYSUBSTITUTE, archivedActivityInstance.getExecutedBySubstitute());
                taskContent.put( ExplorerJson.JSON_EXECUTEDBYSUBSTITUTENAME, getUser(archivedActivityInstance.getExecutedBySubstitute(), parameter.identityAPI));
                taskContent.put( ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(archivedActivityInstance.getLastUpdateDate()));
                taskContent.put( ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(archivedActivityInstance.getLastUpdateDate()));
            }
            // now order by date
            Collections.sort(explorerCaseResult.listTasks, new Comparator<Map<String, Object>>() {

                public int compare(Map<String, Object> s1,
                        Map<String, Object> s2) {
                    Long date1 = (Long) s1.get( ExplorerJson.JSON_ENDDATE);
                    Long date2 = (Long) s2.get( ExplorerJson.JSON_ENDDATE);
                    if (date1 == null)
                        return 0;
                    return date1.compareTo(date2);
                }
            });
        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventLoadTasksCase, e, "Exception " + e.getMessage()));
        }
        return explorerCaseResult;
    }

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult loadCommentsCase(Parameter parameter) {

        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin() );
        try {
            // the API dones not have a ROOT_PROCESS_INSTANCE for active case
            // and... there is not API to get all Process Instance and subprocess for a case Id.
            // so, thanks to the BonitaCaseDetails to get this for me.
            CaseDetailsAPI caseDetailAPI = new CaseDetailsAPI();
            CaseHistoryParameter caseHistoryParameter = new CaseHistoryParameter();
            caseHistoryParameter.tenantId = parameter.tenantId;
            caseHistoryParameter.caseId = parameter.searchCaseId;
            caseHistoryParameter.loadActivities=false;
            caseHistoryParameter.loadArchivedActivities=false;
            caseHistoryParameter.loadArchivedHistoryProcessVariable=false;
            caseHistoryParameter.loadArchivedProcessVariable=false;
            caseHistoryParameter.loadBdmVariables=false;
            caseHistoryParameter.loadContentBdmVariables=false;
            caseHistoryParameter.loadDocuments=false;
            caseHistoryParameter.loadEvents=false;
            caseHistoryParameter.loadProcessVariables=false;
            caseHistoryParameter.loadSubProcess=false;
            caseHistoryParameter.loadTimers=false;
            caseHistoryParameter.loadContract = false;
            
            caseHistoryParameter.loadComments=LOADCOMMENTS.ONLYUSERS;
            CaseDetails caseDetail = caseDetailAPI.getCaseDetails( caseHistoryParameter, parameter.processAPI, parameter.identityAPI, null, parameter.apiSession);
            
            for (CaseDetailComment comment : caseDetail.listComments)
            {
                Map<String, Object> commentContent = new HashMap<>();
                explorerCaseResult.listComments.add(commentContent);
                if (comment.comment!=null) {
                    commentContent.put( ExplorerJson.JSON_PROCESSINSTANCE, comment.comment.getProcessInstanceId());
                    commentContent.put( ExplorerJson.JSON_CONTENT, comment.comment.getContent());
                    commentContent.put( ExplorerJson.JSON_ID, comment.comment.getId());
                    commentContent.put( ExplorerJson.JSON_USERBY, comment.comment.getUserId());
                    User user =getUser(comment.comment.getUserId(), parameter.identityAPI);
                    commentContent.put( ExplorerJson.JSON_USERBYNAME, user==null ? null : user.getUserName());
                    commentContent.put( ExplorerJson.JSON_ENDDATE, comment.comment.getPostDate());
                    commentContent.put( ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(comment.comment.getPostDate()));
                }
                if (comment.archivedComment!=null) {
                    commentContent.put( ExplorerJson.JSON_PROCESSINSTANCE, comment.archivedComment.getProcessInstanceId());
                    commentContent.put( ExplorerJson.JSON_CONTENT, comment.archivedComment.getContent());
                    commentContent.put( ExplorerJson.JSON_ID, comment.archivedComment.getId());
                    commentContent.put( ExplorerJson.JSON_USERBY, comment.archivedComment.getUserId());
                    User user =getUser(comment.archivedComment.getUserId(), parameter.identityAPI);
                    commentContent.put( ExplorerJson.JSON_USERBYNAME, user==null ? null : user.getUserName());
                    commentContent.put( ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(comment.archivedComment.getPostDate()));
                    commentContent.put( ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(comment.archivedComment.getPostDate()));

                }
            }
                
         
            // now order by date
            Collections.sort(explorerCaseResult.listComments, new Comparator<Map<String, Object>>() {

                public int compare(Map<String, Object> s1,
                        Map<String, Object> s2) {
                    Long date1 = (Long) s1.get( ExplorerJson.JSON_ENDDATE);
                    Long date2 = (Long) s2.get( ExplorerJson.JSON_ENDDATE);
                    if (date1 == null)
                        return 0;
                    return date1.compareTo(date2);
                }
            });
        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventLoadTasksCase, e, "Exception " + e.getMessage()));
        }
        return explorerCaseResult;
    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Private */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    private String getAttributDescriptor(String name, boolean isActive) {
        if ( ExplorerJson.JSON_STRINGINDEX1.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_1;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_1;
        }
        if ( ExplorerJson.JSON_STRINGINDEX2.equals(isActive)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_2;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_2;
        }
        if ( ExplorerJson.JSON_STRINGINDEX3.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_3;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_3;
        }
        if ( ExplorerJson.JSON_STRINGINDEX4.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_4;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_4;
        }
        if ( ExplorerJson.JSON_STRINGINDEX5.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.STRING_INDEX_5;
            else
                return ArchivedProcessInstancesSearchDescriptor.STRING_INDEX_5;
        }
        if ( ExplorerJson.JSON_CASEID.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.ID;
            else
                return ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID;
        }

        if ( ExplorerJson.JSON_PROCESSDEFINITIONID.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID;
            else
                return ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID;
        }

        if ( ExplorerJson.JSON_STARTDATE.equals(name)) {
            if (isActive)
                return ProcessInstanceSearchDescriptor.START_DATE;
            else
                return ArchivedProcessInstancesSearchDescriptor.START_DATE;
        }
        if ( ExplorerJson.JSON_ENDDATE.equals(name)) {
            if (isActive)
                return null;
            else
                return ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE;
        }
        return null;

    }

    private void completeSob(SearchOptionsBuilder sob, List<Long> listProcessDefinition, String attributName) {
        if (listProcessDefinition.isEmpty()) {
            // a process is required, but not process is found : do an impossible search
            sob.filter(attributName, 0);
            return;
        }
        sob.leftParenthesis();
        for (int i = 0; i < listProcessDefinition.size(); i++) {
            if (i > 0)
                sob.or();
            sob.filter(attributName, listProcessDefinition.get(i));
        }
        sob.rightParenthesis();
    }

    /**
     * @param processName
     * @param processAPI
     * @return
     */
    private List<Long> getListProcess(String processName, ProcessAPI processAPI) {
        List<Long> listProcessDefinition = new ArrayList<>();
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
        sob.greaterOrEquals(ProcessDeploymentInfoSearchDescriptor.NAME, processName);
        sob.lessOrEquals(ProcessDeploymentInfoSearchDescriptor.NAME, processName + "z");

        SearchResult<ProcessDeploymentInfo> searchResult;
        try {
            searchResult = processAPI.searchProcessDeploymentInfos(sob.done());
            for (ProcessDeploymentInfo processInfo : searchResult.getResult())
                listProcessDefinition.add(processInfo.getProcessId());
        } catch (SearchException e) {
            // do nothing... should never arrived
        }
        return listProcessDefinition;

    }

    /**
     * 
     */
    private Map<String, Object> getFromProcessInstance(ProcessInstance processInstance, ProcessAPI processAPI, IdentityAPI identityAPI) {
        Map<String, Object> information = new HashMap<>();

        information.put( ExplorerJson.JSON_ORIGIN, ExplorerJson.JSON_ORIGIN_V_OPENCASE);

        information.put( ExplorerJson.JSON_CASEID, processInstance.getId());
        information.put( ExplorerJson.JSON_STARTDATE,  ExplorerCase.getFromDate(processInstance.getStartDate()));
        information.put( ExplorerJson.JSON_STARTDATEST, ExplorerCase.getFromDateString(processInstance.getStartDate()));

        ProcessDefinition processDefinition = getProcessDefinition(processInstance.getProcessDefinitionId(), processAPI);
        information.put( ExplorerJson.JSON_PROCESSNAME, processDefinition == null ? null : processDefinition.getName());
        information.put( ExplorerJson.JSON_PROCESSVERSION, processDefinition == null ? null : processDefinition.getVersion());
        information.put( ExplorerJson.JSON_PROCESSDEFINITIONID, processDefinition == null ? null : processDefinition.getId());

        User user = getUser(processInstance.getStartedBy(), identityAPI);
        information.put( ExplorerJson.JSON_STARTBYNAME, user == null ? null : user.getUserName());

        information.put( ExplorerJson.JSON_STRINGINDEX1, processInstance.getStringIndex1());
        information.put( ExplorerJson.JSON_STRINGINDEX2, processInstance.getStringIndex2());
        information.put( ExplorerJson.JSON_STRINGINDEX3, processInstance.getStringIndex3());
        information.put( ExplorerJson.JSON_STRINGINDEX4, processInstance.getStringIndex4());
        information.put( ExplorerJson.JSON_STRINGINDEX5, processInstance.getStringIndex5());
        information.put( ExplorerJson.JSON_URLOVERVIEW, "/bonita/portal/form/processInstance/" + processInstance.getId());

        return information;
    }

    private Map<String, Object> getFromArchivedProcessInstance(ArchivedProcessInstance archivedProcessInstance, ProcessAPI processAPI, IdentityAPI identityAPI) {
        Map<String, Object> information = new HashMap<>();
        information.put( ExplorerJson.JSON_ORIGIN,  ExplorerJson.JSON_ORIGIN_V_ARCHIVEDCASE);
        information.put( ExplorerJson.JSON_CASEID, archivedProcessInstance.getSourceObjectId());
        information.put( ExplorerJson.JSON_STARTDATE,  ExplorerCase.getFromDate(archivedProcessInstance.getStartDate()));
        information.put( ExplorerJson.JSON_STARTDATEST, ExplorerCase.getFromDateString(archivedProcessInstance.getStartDate()));
        information.put( ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(archivedProcessInstance.getEndDate()));
        information.put( ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(archivedProcessInstance.getEndDate()));

        ProcessDefinition processDefinition = getProcessDefinition(archivedProcessInstance.getProcessDefinitionId(), processAPI);
        information.put( ExplorerJson.JSON_PROCESSNAME, processDefinition == null ? null : processDefinition.getName());
        information.put( ExplorerJson.JSON_PROCESSVERSION, processDefinition == null ? null : processDefinition.getVersion());

        User user = getUser(archivedProcessInstance.getStartedBy(), identityAPI);
        information.put( ExplorerJson.JSON_STARTBYNAME, user == null ? null : user.getUserName());

        information.put( ExplorerJson.JSON_STRINGINDEX1, archivedProcessInstance.getStringIndexValue(1));
        information.put( ExplorerJson.JSON_STRINGINDEX2, archivedProcessInstance.getStringIndexValue(2));
        information.put( ExplorerJson.JSON_STRINGINDEX3, archivedProcessInstance.getStringIndexValue(3));
        information.put( ExplorerJson.JSON_STRINGINDEX4, archivedProcessInstance.getStringIndexValue(4));
        information.put( ExplorerJson.JSON_STRINGINDEX5, archivedProcessInstance.getStringIndexValue(5));

        information.put( ExplorerJson.JSON_URLOVERVIEW, "/bonita/portal/form/processInstance/" + archivedProcessInstance.getSourceObjectId());

        return information;
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
}

