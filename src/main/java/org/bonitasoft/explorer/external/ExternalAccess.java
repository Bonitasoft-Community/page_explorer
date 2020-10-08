package org.bonitasoft.explorer.external;

import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.logging.Logger;

import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.ExplorerCase;
import org.bonitasoft.explorer.ExplorerCase.ExplorerCaseResult;
import org.bonitasoft.explorer.TypesCast;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.properties.DatabaseConnection;

public class ExternalAccess {

    private final static String LOGGER_LABEL = "DatabaseQuery";
    private final static Logger logger = Logger.getLogger(ExternalAccess.class.getName());

    private static BEvent eventSearchExternal = new BEvent(ExternalAccess.class.getName(), 1, Level.ERROR,
            "Error during selection", "An error arrived during selection", "Selection failed", "Check the exception");

    public ExplorerCaseResult searchCases(String datasource, Parameter parameter) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();
        List<Object> sqlParam = new ArrayList<>();

        if (parameter.searchYear != null && parameter.searchStartDateFrom == null)
            parameter.searchStartDateFrom = TypesCast.getLongDateFromYear(parameter.searchYear);
        if (parameter.searchYear != null && parameter.searchStartDateTo == null)
            parameter.searchStartDateTo = TypesCast.getLongDateFromYear(parameter.searchYear + 1);

        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select * from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + " where ");
        // only root cases
        sqlRequest.append(DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + "=" + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID);
        // the archive search descriptor does not define
        sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID + "= ?");
        sqlParam.add(parameter.tenantId);

        if (parameter.searchText != null) {
            sqlRequest.append(" and (");
            sqlRequest.append(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX1 + " like ? ");
            sqlRequest.append(" or " + DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX2 + " like ? ");
            sqlRequest.append(" or " + DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX3 + " like ? ");
            sqlRequest.append(" or " + DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX4 + " like ? ");
            sqlRequest.append(" or " + DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX5 + " like ? ");
            sqlRequest.append(")");
            sqlParam.add("%" + parameter.searchText + "%");
            sqlParam.add("%" + parameter.searchText + "%");
            sqlParam.add("%" + parameter.searchText + "%");
            sqlParam.add("%" + parameter.searchText + "%");
            sqlParam.add("%" + parameter.searchText + "%");
        }

        if (parameter.searchCaseId != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + " = ?");
            sqlParam.add(parameter.searchCaseId);
        }
        if (parameter.searchProcessName != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSDEFINITIONNAME + " like ?");
            sqlParam.add("%" + parameter.searchProcessName + "%");
        }
        if (parameter.searchStartDateFrom != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " >= ?");
            sqlParam.add(parameter.searchStartDateFrom);
        }
        if (parameter.searchStartDateTo != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " <= ?");
            sqlParam.add(parameter.searchStartDateTo);
        }
        if (parameter.searchEndedDateFrom != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_ARCHIVEDATE + " >= ?");
            sqlParam.add(parameter.searchEndedDateFrom);
        }

        if (parameter.searchEndedDateTo != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_ARCHIVEDATE + " <= ?");
            sqlParam.add(parameter.searchEndedDateTo);
        }
        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            explorerCaseResult.listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(explorerCaseResult.listEvents))
                return explorerCaseResult;

            PreparedStatement pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            for (int i = 0; i < sqlParam.size(); i++)
                pstmt.setObject(i + 1, sqlParam.get(i));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> information = new HashMap<>();

                information.put(ExplorerCase.JSON_SCOPE, ExplorerCase.JSON_SCOPE_V_EXTERNAL);

                information.put(ExplorerCase.JSON_CASEID, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID));
                information.put(ExplorerCase.JSON_STARTDATE, rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE));
                information.put(ExplorerCase.JSON_STARTDATEST, ExplorerCase.getFromDateString(rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE)));
                information.put(ExplorerCase.JSON_ENDDATE, rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE));
                information.put(ExplorerCase.JSON_ENDDATEST, ExplorerCase.getFromDateString(rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE)));
                information.put(ExplorerCase.JSON_PROCESSNAME, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSDEFINITIONNAME));
                information.put(ExplorerCase.JSON_PROCESSVERSION, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSDEFINITIONVERSION));

                information.put(ExplorerCase.JSON_STARTBYNAME, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STARTEDBYNAME));

                information.put(ExplorerCase.JSON_STRINGINDEX1, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX1));
                information.put(ExplorerCase.JSON_STRINGINDEX2, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX2));
                information.put(ExplorerCase.JSON_STRINGINDEX3, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX3));
                information.put(ExplorerCase.JSON_STRINGINDEX4, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX4));
                information.put(ExplorerCase.JSON_STRINGINDEX5, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_STRINGINDEX5));

                explorerCaseResult.listCases.add(information);
                explorerCaseResult.totalNumberOfResult++;
            }
            rs.close();
        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));
        } finally {
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        return explorerCaseResult;
    }

    /**
     * load a case (variable, documents)
     * 
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public ExplorerCaseResult loadCase(String datasource, Parameter parameter) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();

        loadVariableCase(explorerCaseResult, datasource, parameter);
        loadDocumentsCase(explorerCaseResult, datasource, parameter);
        if (parameter.isUserAdmin()) {
            loadTasksCase(explorerCaseResult, datasource, parameter);
            loadCommentsCase(explorerCaseResult, datasource, parameter);
        }
        return explorerCaseResult;
    }

    /**
     * Load Variables case
     * 
     * @param explorerCaseResult
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     */
    public void loadVariableCase(ExplorerCaseResult explorerCaseResult, String datasource, Parameter parameter) {
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select *, ");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " as PROCESS_STARTDATE,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE + " as PROCESS_ENDDATE,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_STARTEDBYNAME + " as PROCESS_STARTBYNAME,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_STARTEDBYSUBSTITUTENAME + " as PROCESS_STARTBYSUBSTITUTENAME ");
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_DATAINSTANCE);
        sqlRequest.append(" left join " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + " on ("
                + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID + "="
                + DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID
                + " and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID + "="
                + DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_TENANTID

                + ")");
        sqlRequest.append(" where ");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_TENANTID + "= ?");
        sqlRequest.append(" and " + DatabaseDefinition.BDE_DATAINSTANCE_CONTAINERTYPE + " in (?,?)");

        sqlRequest.append(" and " + DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID
                + " in (select " + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID
                + "   from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE
                + "   where " + DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + " = ? "
                + "      and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID
                + "=" + DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_TENANTID
                + ")");
        // root case in first then
        sqlRequest.append(" order by " + DatabaseDefinition.BDE_TABLE_DATAINSTANCE + "." + DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID);

        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            explorerCaseResult.listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(explorerCaseResult.listEvents))
                return;

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, DatabaseDefinition.BDE_DATAINSTANCE_CONTAINERTYPE_V_PROCESS);
            pstmt.setObject(3, DatabaseDefinition.BDE_DATAINSTANCE_CONTAINERTYPE_V_BDM);
            pstmt.setObject(4, parameter.searchCaseId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> information = null;
                Map<String, Object> processCase = explorerCaseResult.getExternalCase(rs.getLong(DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID));
                List<Map<String, Object>> listValueVariable = (List<Map<String, Object>>) processCase.get(ExplorerCase.JSON_VARIABLES);

                // complete the variable
                if (parameter.searchCaseId.equals(processCase.get(ExplorerCase.JSON_CASEID))) {
                    processCase.put(ExplorerCase.JSON_STARTDATEST, ExplorerCase.getFromDateString(rs.getLong("PROCESS_STARTDATE")));
                    processCase.put(ExplorerCase.JSON_ENDDATEST, ExplorerCase.getFromDateString(rs.getLong("PROCESS_ENDDATE")));
                    processCase.put(ExplorerCase.JSON_STARTEDBYNAME, rs.getString("PROCESS_STARTBYNAME"));
                    if (rs.getString("PROCESS_STARTBYNAME") == null || rs.getString("PROCESS_STARTBYNAME").isEmpty())
                        processCase.put(ExplorerCase.JSON_STARTEDBYSUBSTITUTENAME, rs.getString("PROCESS_STARTBYSUBSTITUTENAME"));
                    processCase.put(ExplorerCase.JSON_TYPEPROCESSINSTANCE, ExplorerCase.JSON_TYPEPROCESSINSTANCE_V_ROOT);

                } else {
                    processCase.put(ExplorerCase.JSON_TYPEPROCESSINSTANCE, ExplorerCase.JSON_TYPEPROCESSINSTANCE_V_SUBPROCESS);
                }
                // Attention, a BDM Multiple has multiple record.
                String variableName = rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_NAME);
                if (Boolean.TRUE.equals(rs.getBoolean(DatabaseDefinition.BDE_DATAINSTANCE_BDMISMULTIPLE))) {
                    // search an existing information
                    Long currentProcessInstance = rs.getLong(DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID);
                    for (Map<String, Object> currentInfo : listValueVariable) {
                        if (currentInfo.get(ExplorerCase.JSON_PROCESSINSTANCE).equals(currentProcessInstance)
                                && currentInfo.get(ExplorerCase.JSON_NAME).equals(variableName)) {
                            // we get the same !
                            information = currentInfo;
                            break;
                        }
                    }

                }
                if (information == null) {
                    information = new HashMap<>();
                    listValueVariable.add(information);
                }

                information.put(ExplorerCase.JSON_BDMISMULTIPLE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_BDMISMULTIPLE));
                information.put(ExplorerCase.JSON_PROCESSINSTANCE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID));
                information.put(ExplorerCase.JSON_NAME, variableName);
                information.put(ExplorerCase.JSON_BDMNAME, rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMNAME));

                information.put(ExplorerCase.JSON_CLASSNAME, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_CLASSNAME));

                if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_VALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_VALUE));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_STRING);

                } else if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_DATEVALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, ExplorerCase.getFromDateString(rs.getLong(DatabaseDefinition.BDE_DATAINSTANCE_DATEVALUE)));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_DATE);

                } else if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_LONGVALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_LONGVALUE));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_NUMBER);

                } else if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_BOOLEANVALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_BOOLEANVALUE));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_NUMBER);

                } else if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_DOUBLEVALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_DOUBLEVALUE));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_NUMBER);

                } else if (rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_FLOATVALUE) != null) {
                    information.put(ExplorerCase.JSON_VALUE, rs.getObject(DatabaseDefinition.BDE_DATAINSTANCE_FLOATVALUE));
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_NUMBER);
                } else if (rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMNAME) != null) {
                    if (Boolean.TRUE.equals(rs.getBoolean(DatabaseDefinition.BDE_DATAINSTANCE_BDMISMULTIPLE))) {
                        Object value = information.get(ExplorerCase.JSON_VALUE);
                        List<String> listBdmName = value instanceof List ? (List) value : new ArrayList<>();
                        // start at 0
                        int index = rs.getInt(DatabaseDefinition.BDE_DATAINSTANCE_BDMINDEX);
                        while (listBdmName.size() <= index)
                            listBdmName.add(null);
                        listBdmName.set(index, rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMNAME) + "-" + rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMPERSISTENCEID));
                        information.put(ExplorerCase.JSON_VALUE, listBdmName);
                        information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_LIST);
                    } else {
                        information.put(ExplorerCase.JSON_VALUE, rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMNAME) + "-" + rs.getString(DatabaseDefinition.BDE_DATAINSTANCE_BDMPERSISTENCEID));
                        information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_STRING);

                    }
                }

            }
            rs.close();

            // order variables by processInstance / then name
            for (Map<String, Object> processVariable : explorerCaseResult.externalcase) {
                List<Map<String, Object>> listValueVariable = (List<Map<String, Object>>) processVariable.get(ExplorerCase.JSON_VARIABLES);
                if (listValueVariable == null)
                    continue;
                Collections.sort(listValueVariable, new Comparator<Map<String, Object>>() {

                    public int compare(Map<String, Object> s1,
                            Map<String, Object> s2) {
                        String name1 = (String) s1.get(ExplorerCase.JSON_NAME);
                        String name2 = (String) s2.get(ExplorerCase.JSON_NAME);
                        return name1.compareTo(name2);
                    }
                });
            }

        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                    // nothing to log
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
    }

    /**
     * Load all documents
     * 
     * @param explorerCaseResult
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     */
    public void loadDocumentsCase(ExplorerCaseResult explorerCaseResult, String datasource, Parameter parameter) {
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select *, ");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " as PROCESS_STARTDATE,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE + " as PROCESS_ENDDATE,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_STARTEDBYNAME + " as PROCESS_STARTBYNAME,");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_STARTEDBYSUBSTITUTENAME + " as PROCESS_STARTBYSUBSTITUTENAME ");
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE);
        sqlRequest.append(" left join " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + " on ("
                + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID + "="
                + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID
                + " and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID + "="
                + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_TENANTID

                + ")");
        sqlRequest.append(" where ");
        sqlRequest.append(DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_TENANTID + "= ?");

        sqlRequest.append(" and " + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID
                + " in (select " + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID
                + "   from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE
                + "   where " + DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + " = ? "
                + "      and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID
                + "=" + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_TENANTID
                + ")");
        // root case in first then
        sqlRequest.append(" order by " + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE + "." + DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID);

        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            explorerCaseResult.listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(explorerCaseResult.listEvents))
                return;

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, parameter.searchCaseId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> information = null;
                Map<String, Object> processCase = explorerCaseResult.getExternalCase(rs.getLong(DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID));
                List<Map<String, Object>> listDocumentsVariable = (List<Map<String, Object>>) processCase.get(ExplorerCase.JSON_DOCUMENTS);

                // complete the variable
                if (parameter.searchCaseId.equals(processCase.get(ExplorerCase.JSON_CASEID))) {
                    processCase.put(ExplorerCase.JSON_STARTDATEST, ExplorerCase.getFromDateString(rs.getLong("PROCESS_STARTDATE")));
                    processCase.put(ExplorerCase.JSON_ENDDATEST, ExplorerCase.getFromDateString(rs.getLong("PROCESS_ENDDATE")));
                    processCase.put(ExplorerCase.JSON_STARTEDBYNAME, rs.getString("PROCESS_STARTBYNAME"));
                    if (rs.getString("PROCESS_STARTBYNAME") == null || rs.getString("PROCESS_STARTBYNAME").isEmpty())
                        processCase.put(ExplorerCase.JSON_STARTEDBYSUBSTITUTENAME, rs.getString("PROCESS_STARTBYSUBSTITUTENAME"));
                    processCase.put(ExplorerCase.JSON_TYPEPROCESSINSTANCE, ExplorerCase.JSON_TYPEPROCESSINSTANCE_V_ROOT);

                } else {
                    processCase.put(ExplorerCase.JSON_TYPEPROCESSINSTANCE, ExplorerCase.JSON_TYPEPROCESSINSTANCE_V_SUBPROCESS);
                }

                String variableName = rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_NAME);
                if (Boolean.TRUE.equals(rs.getBoolean(DatabaseDefinition.BDE_DOCUMENTINSTANCE_ISMULTIPLE))) {
                    // search an existing information
                    Long currentProcessInstance = rs.getLong(DatabaseDefinition.BDE_DATAINSTANCE_PROCESSINSTANCEID);
                    for (Map<String, Object> currentInfo : listDocumentsVariable) {
                        if (currentInfo.get(ExplorerCase.JSON_PROCESSINSTANCE).equals(currentProcessInstance)
                                && currentInfo.get(ExplorerCase.JSON_NAME).equals(variableName)) {
                            // we get the same !
                            information = currentInfo;
                            break;
                        }
                    }

                }
                if (information == null) {
                    information = new HashMap<>();
                    listDocumentsVariable.add(information);
                }

                information.put(ExplorerCase.JSON_PROCESSINSTANCE, rs.getObject(DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID));
                information.put(ExplorerCase.JSON_NAME, variableName);
                information.put(ExplorerCase.JSON_VERSION, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_VERSION));
                Map<String, Object> docContent = new HashMap<>();
                docContent.put(ExplorerCase.JSON_ID, rs.getLong(DatabaseDefinition.BDE_DOCUMENTINSTANCE_ID));
                docContent.put(ExplorerCase.JSON_AUTHOR, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_AUTHOR));
                docContent.put(ExplorerCase.JSON_FILENAME, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_FILENAME));
                docContent.put(ExplorerCase.JSON_MIMETYPE, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_MIMETYPE));
                docContent.put(ExplorerCase.JSON_URL, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_URL));
                docContent.put(ExplorerCase.JSON_HASCONTENT, rs.getBoolean(DatabaseDefinition.BDE_DOCUMENTINSTANCE_HASCONTENT));

                if (Boolean.TRUE.equals(rs.getBoolean(DatabaseDefinition.BDE_DOCUMENTINSTANCE_ISMULTIPLE))) {
                    Object value = information.get(ExplorerCase.JSON_VALUE);
                    List<Map<String, Object>> listBdmName = value instanceof List ? (List) value : new ArrayList<>();
                    // start at 0
                    int index = rs.getInt(DatabaseDefinition.BDE_DOCUMENTINSTANCE_INDEX);
                    while (listBdmName.size() <= index)
                        listBdmName.add(null);

                    // complete the content
                    listBdmName.set(index, docContent);
                    information.put(ExplorerCase.JSON_VALUE, listBdmName);
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_LIST);
                } else {
                    information.putAll(docContent);
                    information.put(ExplorerCase.JSON_TYPEVARIABLE, ExplorerCase.JSON_TYPEVARIABLE_V_DOC);

                }

            }
            rs.close();

            // order variables by processInstance / then name
            for (Map<String, Object> processVariable : explorerCaseResult.externalcase) {
                List<Map<String, Object>> listDocumentsVariable = (List<Map<String, Object>>) processVariable.get(ExplorerCase.JSON_DOCUMENTS);
                if (listDocumentsVariable == null)
                    continue;
                Collections.sort(listDocumentsVariable, new Comparator<Map<String, Object>>() {

                    public int compare(Map<String, Object> s1,
                            Map<String, Object> s2) {
                        String name1 = (String) s1.get(ExplorerCase.JSON_NAME);
                        String name2 = (String) s2.get(ExplorerCase.JSON_NAME);
                        return name1.compareTo(name2);
                    }
                });
            }

        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                    // Nothing to log
                }
        }
    }

    /**
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public Map<String, Object> loadDocument(String datasource, Parameter parameter) {
        Map<String, Object> docContent = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select * ");
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE);
        sqlRequest.append(" where " + DatabaseDefinition.BDE_DOCUMENTINSTANCE_TENANTID + " = ?");
        sqlRequest.append(" and " + DatabaseDefinition.BDE_DOCUMENTINSTANCE_ID + " = ?");

        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(listEvents)) {
                docContent.put("listevents", listEvents);
                return docContent;
            }

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, parameter.docId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                docContent.put(ExplorerCase.JSON_PROCESSINSTANCE, rs.getObject(DatabaseDefinition.BDE_DOCUMENTINSTANCE_PROCESSINSTANCEID));
                docContent.put(ExplorerCase.JSON_NAME, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_NAME));
                docContent.put(ExplorerCase.JSON_VERSION, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_VERSION));
                docContent.put(ExplorerCase.JSON_ID, rs.getLong(DatabaseDefinition.BDE_DOCUMENTINSTANCE_ID));
                docContent.put(ExplorerCase.JSON_AUTHOR, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_AUTHOR));
                docContent.put(ExplorerCase.JSON_FILENAME, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_FILENAME));
                docContent.put(ExplorerCase.JSON_MIMETYPE, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_MIMETYPE));
                docContent.put(ExplorerCase.JSON_URL, rs.getString(DatabaseDefinition.BDE_DOCUMENTINSTANCE_URL));
                docContent.put(ExplorerCase.JSON_HASCONTENT, rs.getBoolean(DatabaseDefinition.BDE_DOCUMENTINSTANCE_HASCONTENT));
            }

            rs.close();

        } catch (Exception e) {
            listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        docContent.put("listevents", listEvents);
        return docContent;
    }

    /**
     * 
     * @param output
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public List<BEvent> loadDocumentOuput(OutputStream output, String datasource, Parameter parameter) {
        List<BEvent> listEvents = new ArrayList<>();
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select  "+DatabaseDefinition.BDE_DOCUMENTINSTANCE_CONTENT);
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_DOCUMENTINSTANCE);
        sqlRequest.append(" where " + DatabaseDefinition.BDE_DOCUMENTINSTANCE_TENANTID + " = ?");
        sqlRequest.append(" and " + DatabaseDefinition.BDE_DOCUMENTINSTANCE_ID + " = ?");

        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(listEvents)) {
                return listEvents;
            }

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, parameter.docId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                InputStream input = rs.getBinaryStream(DatabaseDefinition.BDE_DOCUMENTINSTANCE_CONTENT);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

            }
            rs.close();

        } catch (Exception e) {
            listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                    //nothing to log
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        return listEvents;

    }

    /**
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public void loadTasksCase(ExplorerCaseResult explorerCaseResult, String datasource, Parameter parameter) {
        
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select * ");
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_FLOWNODEINSTANCE);
        sqlRequest.append(" where " + DatabaseDefinition.BDE_FLOWNODEINSTANCE_TENANTID + " = ?");
        sqlRequest.append(" and " + DatabaseDefinition.BDE_FLOWNODEINSTANCE_PROCESSINSTANCEID
                + " in (select " + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID
                + "   from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE
                + "   where " + DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + " = ? "
                + "      and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID
                + "=" + DatabaseDefinition.BDE_TABLE_FLOWNODEINSTANCE + "." + DatabaseDefinition.BDE_FLOWNODEINSTANCE_TENANTID
                + ")");
        sqlRequest.append(" order by "+DatabaseDefinition.BDE_FLOWNODEINSTANCE_ARCHIVEDATE);
  
        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            explorerCaseResult.listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(explorerCaseResult.listEvents)) {
                return;
            }

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, parameter.searchCaseId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> taskContent = new HashMap<>();
                explorerCaseResult.listTasks.add( taskContent );
                taskContent.put(ExplorerCase.JSON_PROCESSINSTANCE, rs.getObject(DatabaseDefinition.BDE_FLOWNODEINSTANCE_PROCESSINSTANCEID));
                taskContent.put(ExplorerCase.JSON_NAME, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_NAME));
                taskContent.put(ExplorerCase.JSON_DISPLAYNAME, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_DISPLAYNAME));
                taskContent.put(ExplorerCase.JSON_DISPLAYDESCRIPTION, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_DISPLAYDESCRIPTION));
                taskContent.put(ExplorerCase.JSON_ID, rs.getLong(DatabaseDefinition.BDE_FLOWNODEINSTANCE_ID));
                taskContent.put(ExplorerCase.JSON_KIND, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_KIND));
                taskContent.put(ExplorerCase.JSON_EXECUTEDBY, rs.getLong(DatabaseDefinition.BDE_FLOWNODEINSTANCE_EXECUTEDBY));
                taskContent.put(ExplorerCase.JSON_EXECUTEDBYNAME, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_EXECUTEDBYNAME));
                taskContent.put(ExplorerCase.JSON_EXECUTEDBYSUBSTITUTE, rs.getLong(DatabaseDefinition.BDE_FLOWNODEINSTANCE_EXECUTEDBYSUBSTITUTE));
                taskContent.put(ExplorerCase.JSON_EXECUTEDBYSUBSTITUTENAME, rs.getString(DatabaseDefinition.BDE_FLOWNODEINSTANCE_EXECUTEDBYSUBSTITUTENAME));
                taskContent.put(ExplorerCase.JSON_ENDDATEST, ExplorerCase.getFromDateString( rs.getLong(DatabaseDefinition.BDE_FLOWNODEINSTANCE_ARCHIVEDATE)));
            }

            rs.close();

        } catch (Exception e) {
            explorerCaseResult.listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                    // nothing to log
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        return;
    }
    /**
     * @param datasource
     * @param parameter
     * @param processAPI
     * @param identityAPI
     * @return
     */
    public List<BEvent>  loadCommentsCase(ExplorerCaseResult explorerCaseResult, String datasource, Parameter parameter) {
        List<BEvent> listEvents = new ArrayList<>();
        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select * ");
        sqlRequest.append(" from " + DatabaseDefinition.BDE_TABLE_COMMENTINSTANCE);
        sqlRequest.append(" where " + DatabaseDefinition.BDE_COMMENTINSTANCE_TENANTID + " = ?");
        sqlRequest.append(" and " + DatabaseDefinition.BDE_COMMENTINSTANCE_PROCESSINSTANCEID
                + " in (select " + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID
                + "   from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE
                + "   where " + DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + " = ? "
                + "      and " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + "." + DatabaseDefinition.BDE_PROCESSINSTANCE_TENANTID
                + "=" + DatabaseDefinition.BDE_TABLE_COMMENTINSTANCE + "." + DatabaseDefinition.BDE_COMMENTINSTANCE_TENANTID
                + ")");
        sqlRequest.append("order by "+DatabaseDefinition.BDE_COMMENTINSTANCE_POSTDATE);
  
        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        PreparedStatement pstmt = null;
        try {
            connectionResult = DatabaseConnection.getConnection(Arrays.asList(datasource));
            listEvents.addAll(connectionResult.listEvents);
            if (BEventFactory.isError(listEvents)) {
                return listEvents;
            }

            pstmt = connectionResult.con.prepareStatement(sqlRequest.toString());
            pstmt.setObject(1, parameter.tenantId);
            pstmt.setObject(2, parameter.searchCaseId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> commentContent = new HashMap<>();
                explorerCaseResult.listComments.add( commentContent );

                commentContent.put(ExplorerCase.JSON_PROCESSINSTANCE, rs.getObject(DatabaseDefinition.BDE_COMMENTINSTANCE_PROCESSINSTANCEID));
                commentContent.put(ExplorerCase.JSON_CONTENT, rs.getString(DatabaseDefinition.BDE_COMMENTINSTANCE_CONTENT));
                commentContent.put(ExplorerCase.JSON_ID, rs.getLong(DatabaseDefinition.BDE_COMMENTINSTANCE_ID));
                commentContent.put(ExplorerCase.JSON_USERBY, rs.getLong(DatabaseDefinition.BDE_COMMENTINSTANCE_USERID));
                commentContent.put(ExplorerCase.JSON_USERBYNAME, rs.getString(DatabaseDefinition.BDE_COMMENTINSTANCE_USERNAME));
                commentContent.put(ExplorerCase.JSON_ENDDATEST, ExplorerCase.getFromDateString( rs.getLong(DatabaseDefinition.BDE_COMMENTINSTANCE_POSTDATE)));
            }

            rs.close();

        } catch (Exception e) {
            listEvents.add(new BEvent(eventSearchExternal, e, "Exception " + e.getMessage()));

        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e1) {
                }
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        return listEvents;
    }
  

}
