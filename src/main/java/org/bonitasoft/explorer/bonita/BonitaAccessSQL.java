package org.bonitasoft.explorer.bonita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.casedetails.CaseDetails;
import org.bonitasoft.casedetails.CaseDetails.CaseDetailComment;
import org.bonitasoft.casedetails.CaseDetailsAPI;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.casedetails.CaseDetailsAPI.LOADCOMMENTS;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.ExplorerCase;
import org.bonitasoft.explorer.ExplorerCase.ExplorerCaseResult;
import org.bonitasoft.explorer.external.DatabaseDefinition;
import org.bonitasoft.explorer.ExplorerJson;
import org.bonitasoft.explorer.ExplorerParameters;
import org.bonitasoft.explorer.TypesCast;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaEngineConnection;
import org.bonitasoft.properties.DatabaseConnection;

/**
 * Access by the SQL to include permission
 */
public class BonitaAccessSQL {

    private final static BEvent eventSearchCase = new BEvent(BonitaAccessSQL.class.getName(), 1, Level.ERROR,
            "Error during case search", "A case is searched, the request failed", "The search failed", "Check the exception");
    private final static BEvent eventLoadTasksCase = new BEvent(BonitaAccessSQL.class.getName(), 2, Level.ERROR,
            "Error during load Task case", "Tasks are loaded from a specific case. The load failed", "Tasks are incompletes", "Check the exception");

    private String dataSource;

    /**
     * if the datasoruce is null, then this is the Local server. Else this is an external datasource
     * Default Constructor.
     * 
     * @param dataSource
     */
    public BonitaAccessSQL(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Search cases
     * 
     * @param parameter
     * @param isActive
     * @param explorerParameters
     * @param bonitaserverurl give the url to access the overview. If null, this is the local server
     * @return
     */
    public ExplorerCaseResult searchCases(Parameter parameter, boolean isActive, ExplorerParameters explorerParameters, String bonitaServerUrl) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin());

        StringBuilder sqlRequest = new StringBuilder();
        List<Object> sqlParam = new ArrayList<>();
        String tableName = (isActive ? "PROCESS_INSTANCE" : "ARCH_PROCESS_INSTANCE");
        sqlRequest.append("select pi.*, ");
        sqlRequest.append("pd.NAME as PROCESSNAME, pd.VERSION as PROCESSVERSION");
        sqlRequest.append(" from " + tableName + " as pi ");
        sqlRequest.append("  join PROCESS_DEFINITION as pd on (pi.PROCESSDEFINITIONID = pd.PROCESSID)");
        sqlRequest.append(" where 1=1 ");

        if (parameter.searchText != null) {
            sqlRequest.append(" and (");
            sqlRequest.append(" pi.STRINGINDEX1 like ?");
            sqlRequest.append(" or pi.STRINGINDEX2 like ?");
            sqlRequest.append(" or pi.STRINGINDEX3 like ?");
            sqlRequest.append(" or pi.STRINGINDEX4 like ?");
            sqlRequest.append(" or pi.STRINGINDEX5 like ?");
            sqlRequest.append(")");
            for (int i = 0; i < 5; i++)
                sqlParam.add("%" + parameter.searchText + "%");
        }

        if (parameter.searchCaseId != null) {
            if (isActive)
                sqlRequest.append(" and pi.ID=?");
            else
                sqlRequest.append(" and pi.SOURCEOBJECTID=?");
            sqlParam.add(parameter.searchCaseId);

        }

        if (parameter.searchProcessName != null) {
            // calculate all process with this process name
            List<Long> listProcessDefinition = getListProcess(parameter.searchProcessName, parameter.processAPI);
            if (listProcessDefinition.isEmpty()) {
                // a process is required, but not process is found : do an impossible search
                sqlRequest.append(" and 1=0");
            } else {
                sqlRequest.append(" and pi.PROCESSDEFINITIONID in (");

                for (int i = 0; i < listProcessDefinition.size(); i++) {
                    if (i > 0)
                        sqlRequest.append(",");
                    sqlRequest.append(" ? ");
                    sqlParam.add(listProcessDefinition.get(i));
                }
                sqlRequest.append(")");
            }
        }
        if (parameter.searchStartDateFrom != null) {
            sqlRequest.append(" and pi.STARTDATE >= ?");
            sqlParam.add(parameter.searchStartDateFrom);
        }
        if (parameter.searchStartDateTo != null) {
            sqlRequest.append(" and pi.STARTDATE < ?");
            sqlParam.add(parameter.searchStartDateTo);
        }
        if (parameter.searchEndedDateFrom != null) {
            if (isActive)
                sqlRequest.append(" and 1=0");
            else {
                sqlRequest.append(" and pi.ENDDATE is not null and pi.ENDDATE >= ?");
                sqlParam.add(parameter.searchEndedDateFrom);
            }
        }
        if (parameter.searchEndedDateTo != null) {
            if (isActive)
                sqlRequest.append(" and 1=0");
            else {
                sqlRequest.append(" and pi.ENDDATE is not null and pi.ENDDATE > 0 and pi.ENDDATE < ?");
                sqlParam.add(parameter.searchEndedDateTo);
            }
        }

        // get only the root case
        if (isActive)
            sqlRequest.append(" and pi.ROOTPROCESSINSTANCEID = pi.ID");
        else
            // Archive : there is multiple record for a Archive process instance, but only one with a enddate
            sqlRequest.append(" and pi.ROOTPROCESSINSTANCEID = pi.SOURCEOBJECTID and pi.ENDDATE>0");

        // visibility
        if (parameter.isUserAdmin() && ExplorerJson.JSON_VISILITY_V_ADMIN.equals(parameter.visibility)) {
            // nothing to add
        } else {
            sqlRequest.append(" and ( ");

            // PROCESS PERMISSION
            sqlRequest.append("pi.STARTEDBY = ?");
            sqlParam.add(parameter.apiSession.getUserId());

            // CASE PERMISSION
            String caseid;
            if (isActive)
                caseid = "pi.ID";
            else
                caseid = "pi.SOURCEOBJECTID";
            /*
             * all details
             * sqlRequest.append(" or exists ( select fln.PARENTCONTAINERID from FLOWNODE_INSTANCE as fln where fln.PARENTCONTAINERID=" + caseid
             * + " and fln.KIND = 'user'"
             * + " and fln.ASSIGNEEID = ?) ");
             * sqlRequest.append(" or exists( select fln.PARENTCONTAINERID from ARCH_FLOWNODE_INSTANCE as fln where fln.PARENTCONTAINERID=" + caseid
             * + " and fln.KIND = 'user'"
             * + " and fln.EXECUTEDBY = ?)");
             * sqlRequest.append(" or exists( select flnfather.PARENTCONTAINERID from ARCH_FLOWNODE_INSTANCE as flnfather,ARCH_FLOWNODE_INSTANCE fln "
             * + " where ( flnfather.PARENTCONTAINERID=" + caseid+" and flnfather.KIND = 'multi'"
             * + " and flnfather.sourceobjectid = fln.PARENTCONTAINERID and fln.kind='user' and fln.EXECUTEDBY = ?)");
             * sqlParam.add(parameter.apiSession.getUserId());
             * sqlParam.add(parameter.apiSession.getUserId());
             * sqlParam.add(parameter.apiSession.getUserId());
             */

            /* direct */
            sqlRequest.append(" or exists( select fln.PARENTCONTAINERID from ARCH_FLOWNODE_INSTANCE as fln "
                    + " where  fln.ROOTCONTAINERID=" + caseid + " and fln.KIND = 'user' and fln.EXECUTEDBY = ?)");
            sqlParam.add(parameter.apiSession.getUserId());

            sqlRequest.append(" ) ");

        }

        String direction = Order.ASC.equals(parameter.orderdirection)? "asc":"desc";
        String attributName = getAttributDescriptor(parameter.orderby, isActive);
        sqlRequest.append(" order by ");
        if (parameter.orderby != null && attributName!=null) {
            sqlRequest.append(attributName+" "+ direction+", ");
        }
        // add the case ID systematiclay
        sqlRequest.append(getAttributDescriptor(ExplorerJson.JSON_CASEID, isActive)+" "+direction);
        
        explorerCaseResult.debugInformation.add((isActive ? "ACTIVE:" : "ARCHIVE:") + sqlRequest.toString() + "; Param=" + sqlParam.toString());
        /** and active case does not have a end */
        explorerCaseResult.sqlRequests.add(sqlRequest.toString());
        ResultSet rs=null;
        try (Connection con = getConnection();
                PreparedStatement pstmt = con.prepareStatement(sqlRequest.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {

            for (int i = 0; i < sqlParam.size(); i++)
                pstmt.setObject(i + 1, sqlParam.get(i));

            rs = pstmt.executeQuery();

            // page number >0 ?
            if (parameter.pagenumber==0)
                parameter.pagenumber=1;
            int advanceCount = (parameter.pagenumber -1) * parameter.caseperpages;
            for (int i = 0; i < advanceCount; i++)
                rs.next();

            while (rs.next()) {
                if (explorerCaseResult.listCases.size() > parameter.caseperpages)
                    break;
                Map<String, Object> information = new HashMap<>();
                explorerCaseResult.listCases.add(information);
                information.put(ExplorerJson.JSON_SCOPE, isActive ? ExplorerJson.JSON_SCOPE_V_OPENCASE : ExplorerJson.JSON_SCOPE_V_ARCHIVEDCASE);

                Long processInstanceId = rs.getLong(isActive ? "ID" : "SOURCEOBJECTID");
                information.put(ExplorerJson.JSON_CASEID, processInstanceId);
                information.put(ExplorerJson.JSON_STARTDATE, rs.getLong("STARTDATE"));
                information.put(ExplorerJson.JSON_STARTDATEST, ExplorerCase.getFromDateString(rs.getLong("STARTDATE")));
                if (!isActive) {
                    information.put(ExplorerJson.JSON_ENDDATE, rs.getLong("ENDDATE"));
                    information.put(ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(rs.getLong("ENDDATE")));
                }
                ProcessDefinition processDefinition = getProcessDefinition(rs.getLong("PROCESSDEFINITIONID"), parameter.processAPI);
                information.put(ExplorerJson.JSON_PROCESSNAME, processDefinition == null ? null : processDefinition.getName());
                information.put(ExplorerJson.JSON_PROCESSVERSION, processDefinition == null ? null : processDefinition.getVersion());
                information.put(ExplorerJson.JSON_PROCESSDEFINITIONID, processDefinition == null ? null : processDefinition.getId());
                information.put(ExplorerJson.JSON_STARTBYNAME, getUserFirstNameLastName(rs.getLong("STARTEDBY"), parameter.identityAPI));
                information.put(ExplorerJson.JSON_STRINGINDEX1, rs.getString("STRINGINDEX1"));
                information.put(ExplorerJson.JSON_STRINGINDEX2, rs.getString("STRINGINDEX2"));
                information.put(ExplorerJson.JSON_STRINGINDEX3, rs.getString("STRINGINDEX3"));
                information.put(ExplorerJson.JSON_STRINGINDEX4, rs.getString("STRINGINDEX4"));
                information.put(ExplorerJson.JSON_STRINGINDEX5, rs.getString("STRINGINDEX5"));

                String processName = rs.getString("PROCESSNAME");
                String processVersion = rs.getString("PROCESSVERSION");
                // information.put(ExplorerJson.JSON_URLOVERVIEW, (bonitaServerUrl==null ? "" : bonitaServerUrl)+"/bonita/portal/form/processInstance/" + caseId);
                if (bonitaServerUrl == null)
                    information.put(ExplorerJson.JSON_URLOVERVIEW, "/bonita/portal/resource/processInstance/" + processName + "/" + processVersion + "/content/?id=" + processInstanceId);
                else
                    information.put(ExplorerJson.JSON_URLEXTOVERVIEW, bonitaServerUrl + "/bonita/portal/resource/processInstance/" + processName + "/" + processVersion + "/content/?id=" + processInstanceId);

            }
            // how many record ? 
            rs.last();
            explorerCaseResult.totalNumberOfResult = rs.getRow();

            rs.close();
            rs=null;
        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchCase, e, "Exception " + e.getMessage()));
        }
        finally {
            if (rs!=null)
                try {
                    rs.close();
                } catch (SQLException e) {                 
                }
        }
        return explorerCaseResult;
    }

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult loadTasksCase(Parameter parameter) {

        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin());
        try {
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 1000);
            sob.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, parameter.searchCaseId);
            final SearchResult<FlowNodeInstance> searchFlowNode = parameter.processAPI.searchFlowNodeInstances(sob.done());

            for (final FlowNodeInstance activityInstance : searchFlowNode.getResult()) {
                Map<String, Object> taskContent = new HashMap<>();
                explorerCaseResult.listTasks.add(taskContent);
                taskContent.put(ExplorerJson.JSON_PROCESSINSTANCE, activityInstance.getParentProcessInstanceId());
                taskContent.put(ExplorerJson.JSON_NAME, activityInstance.getName());
                taskContent.put(ExplorerJson.JSON_DISPLAYNAME, activityInstance.getDisplayName());
                taskContent.put(ExplorerJson.JSON_DISPLAYDESCRIPTION, activityInstance.getDisplayDescription());
                taskContent.put(ExplorerJson.JSON_ID, activityInstance.getId());
                taskContent.put(ExplorerJson.JSON_KIND, activityInstance.getType().toString());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBY, activityInstance.getExecutedBy());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYNAME, getUserFirstNameLastName(activityInstance.getExecutedBy(), parameter.identityAPI));
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYSUBSTITUTE, activityInstance.getExecutedBySubstitute());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYSUBSTITUTENAME, getUserFirstNameLastName(activityInstance.getExecutedBySubstitute(), parameter.identityAPI));
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
                taskContent.put(ExplorerJson.JSON_PROCESSINSTANCE, archivedActivityInstance.getSourceObjectId());
                taskContent.put(ExplorerJson.JSON_NAME, archivedActivityInstance.getName());
                taskContent.put(ExplorerJson.JSON_DISPLAYNAME, archivedActivityInstance.getDisplayName());
                taskContent.put(ExplorerJson.JSON_DISPLAYDESCRIPTION, archivedActivityInstance.getDisplayDescription());
                taskContent.put(ExplorerJson.JSON_ID, archivedActivityInstance.getId());
                taskContent.put(ExplorerJson.JSON_KIND, archivedActivityInstance.getType().toString());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBY, archivedActivityInstance.getExecutedBy());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYNAME, getUserFirstNameLastName(archivedActivityInstance.getExecutedBy(), parameter.identityAPI));
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYSUBSTITUTE, archivedActivityInstance.getExecutedBySubstitute());
                taskContent.put(ExplorerJson.JSON_EXECUTEDBYSUBSTITUTENAME, getUserFirstNameLastName(archivedActivityInstance.getExecutedBySubstitute(), parameter.identityAPI));
                taskContent.put(ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(archivedActivityInstance.getLastUpdateDate()));
                taskContent.put(ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(archivedActivityInstance.getLastUpdateDate()));
            }
            // now order by date
            Collections.sort(explorerCaseResult.listTasks, new Comparator<Map<String, Object>>() {

                public int compare(Map<String, Object> s1,
                        Map<String, Object> s2) {
                    Long date1 = (Long) s1.get(ExplorerJson.JSON_ENDDATE);
                    Long date2 = (Long) s2.get(ExplorerJson.JSON_ENDDATE);
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

        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin());
        try {
            // the API dones not have a ROOT_PROCESS_INSTANCE for active case
            // and... there is not API to get all Process Instance and subprocess for a case Id.
            // so, thanks to the BonitaCaseDetails to get this for me.
            CaseDetailsAPI caseDetailAPI = new CaseDetailsAPI();
            CaseHistoryParameter caseHistoryParameter = new CaseHistoryParameter();
            caseHistoryParameter.tenantId = parameter.tenantId;
            caseHistoryParameter.caseId = parameter.searchCaseId;
            caseHistoryParameter.loadActivities = false;
            caseHistoryParameter.loadArchivedActivities = false;
            caseHistoryParameter.loadArchivedHistoryProcessVariable = false;
            caseHistoryParameter.loadArchivedProcessVariable = false;
            caseHistoryParameter.loadBdmVariables = false;
            caseHistoryParameter.loadContentBdmVariables = false;
            caseHistoryParameter.loadDocuments = false;
            caseHistoryParameter.loadEvents = false;
            caseHistoryParameter.loadProcessVariables = false;
            caseHistoryParameter.loadSubProcess = false;
            caseHistoryParameter.loadTimers = false;
            caseHistoryParameter.loadContract = false;

            caseHistoryParameter.loadComments = LOADCOMMENTS.ONLYUSERS;
            CaseDetails caseDetail = caseDetailAPI.getCaseDetails(caseHistoryParameter, parameter.processAPI, parameter.identityAPI, null, parameter.apiSession);

            for (CaseDetailComment comment : caseDetail.listComments) {
                Map<String, Object> commentContent = new HashMap<>();
                explorerCaseResult.listComments.add(commentContent);
                if (comment.comment != null) {
                    commentContent.put(ExplorerJson.JSON_PROCESSINSTANCE, comment.comment.getProcessInstanceId());
                    commentContent.put(ExplorerJson.JSON_CONTENT, comment.comment.getContent());
                    commentContent.put(ExplorerJson.JSON_ID, comment.comment.getId());
                    commentContent.put(ExplorerJson.JSON_USERBY, comment.comment.getUserId());                    
                    commentContent.put(ExplorerJson.JSON_USERBYNAME, getUserFirstNameLastName(comment.comment.getUserId(), parameter.identityAPI));
                    commentContent.put(ExplorerJson.JSON_ENDDATE, comment.comment.getPostDate());
                    commentContent.put(ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(comment.comment.getPostDate()));
                }
                if (comment.archivedComment != null) {
                    commentContent.put(ExplorerJson.JSON_PROCESSINSTANCE, comment.archivedComment.getProcessInstanceId());
                    commentContent.put(ExplorerJson.JSON_CONTENT, comment.archivedComment.getContent());
                    commentContent.put(ExplorerJson.JSON_ID, comment.archivedComment.getId());
                    commentContent.put(ExplorerJson.JSON_USERBY, comment.archivedComment.getUserId());
                    commentContent.put(ExplorerJson.JSON_USERBYNAME, getUserFirstNameLastName(comment.archivedComment.getUserId(), parameter.identityAPI));
                    commentContent.put(ExplorerJson.JSON_ENDDATE, ExplorerCase.getFromDate(comment.archivedComment.getPostDate()));
                    commentContent.put(ExplorerJson.JSON_ENDDATEST, ExplorerCase.getFromDateString(comment.archivedComment.getPostDate()));

                }
            }

            // now order by date
            Collections.sort(explorerCaseResult.listComments, new Comparator<Map<String, Object>>() {

                public int compare(Map<String, Object> s1,
                        Map<String, Object> s2) {
                    Long date1 = (Long) s1.get(ExplorerJson.JSON_ENDDATE);
                    Long date2 = (Long) s2.get(ExplorerJson.JSON_ENDDATE);
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
        if (ExplorerJson.JSON_STRINGINDEX1.equals(name)) {
            return "pi.STRINGINDEX1";
        }
        if (ExplorerJson.JSON_STRINGINDEX2.equals(name)) {
            return "pi.STRINGINDEX2";
        }
        if (ExplorerJson.JSON_STRINGINDEX3.equals(name)) {
            return "pi.STRINGINDEX3";
        }
        if (ExplorerJson.JSON_STRINGINDEX4.equals(name)) {
            return "pi.STRINGINDEX4";
        }
        if (ExplorerJson.JSON_STRINGINDEX5.equals(name)) {
            return "pi.STRINGINDEX5";
        }
        if (ExplorerJson.JSON_CASEID.equals(name)) {
            if (isActive)
                return "pi.ID";
            else
                return "pi.SOURCEOBJECTID";
        }
        if (ExplorerJson.JSON_PROCESSDEFINITIONID.equals(name)) {
            return "pi.PROCESSDEFINITIONID";
        }

        if (ExplorerJson.JSON_STARTDATE.equals(name)) {
            return "pi.STARTDATE";
        }
        if (ExplorerJson.JSON_ENDDATE.equals(name)) {
            return "pi.ENDDATE";
        }
        if ("processname".equals(name))
            return name;
        return null;

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
    private Map<Long, User> cacheUsers = new HashMap<>();

    private String getUserFirstNameLastName(Long userId, IdentityAPI identityAPI) {
        User user = getUser(userId, identityAPI);
        if (user==null)
            return null;
        return user.getFirstName()+" "+user.getLastName();
    }
    private User getUser(Long userId, IdentityAPI identityAPI) {
        if (userId == null || userId <= 0)
            return null;
        try {
            if (cacheUsers.containsKey(userId))
                return cacheUsers.get(userId);
            User user = identityAPI.getUser(userId);
            cacheUsers.put(userId, user);
            return user;
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

    private Connection getConnection() throws SQLException {
        if (dataSource == null)
            return BonitaEngineConnection.getConnection();
        DatabaseConnection.ConnectionResult connection = DatabaseConnection.getConnection(Arrays.asList(dataSource));
        return connection.con;
    }
}
