package org.bonitasoft.explorer.external;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.ExplorerCase;
import org.bonitasoft.explorer.ExplorerCase.ExplorerCaseResult;
import org.bonitasoft.explorer.TypesCast;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.properties.DatabaseConnection;


public class DatabaseQuery {

    
    private final static String LOGGER_LABEL = "DatabaseQuery";
    private final static Logger logger = Logger.getLogger(DatabaseQuery.class.getName());

    private static BEvent eventSearchExternal = new BEvent(DatabaseQuery.class.getName(), 1, Level.ERROR,
            "Error during selection", "An error arrived during selection", "Selection failed", "Check the exception");

    public ExplorerCaseResult searchCases(String datasource, Parameter parameter, ProcessAPI processAPI, IdentityAPI identityAPI) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult();
        List<Object> sqlParam = new ArrayList<>();

        if (parameter.searchYear != null && parameter.searchStartDateBeg == null)
            parameter.searchStartDateBeg = TypesCast.getLongDateFromYear( parameter.searchYear);
        if (parameter.searchYear != null && parameter.searchStartDateEnd == null)
            parameter.searchStartDateEnd = TypesCast.getLongDateFromYear(parameter.searchYear + 1);

        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("select * from " + DatabaseDefinition.BDE_TABLE_PROCESSINSTANCE + " where ");
        // only root cases
        sqlRequest.append(DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID + "=" + DatabaseDefinition.BDE_PROCESSINSTANCE_PROCESSINSTANCEID);
        // the archive search descriptor does not define 
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
        if (parameter.searchStartDateBeg != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " >= ?");
            sqlParam.add(parameter.searchStartDateBeg);
        }
        if (parameter.searchStartDateEnd != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE + " <= ?");
            sqlParam.add(parameter.searchStartDateEnd);
        }
        if (parameter.searchEndedDateBeg != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_ARCHIVEDATE + " >= ?");
            sqlParam.add(parameter.searchEndedDateBeg);
        }

        if (parameter.searchEndedDateEnd != null) {
            sqlRequest.append(" and " + DatabaseDefinition.BDE_PROCESSINSTANCE_ARCHIVEDATE + " <= ?");
            sqlParam.add(parameter.searchEndedDateEnd);
        }
        /** and active case does not have a end */
        DatabaseConnection.ConnectionResult connectionResult = null;
        try {
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

                    information.put(ExplorerCase.JSON_SCOPE, "external");

                    information.put(ExplorerCase.JSON_CASEID, rs.getObject(DatabaseDefinition.BDE_PROCESSINSTANCE_ROOTPROCESSINSTANCEID));
                    information.put(ExplorerCase.JSON_STARTDATE, rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE));
                    information.put(ExplorerCase.JSON_STARTDATEST, getDateFormat(rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_START_DATE)));
                    information.put(ExplorerCase.JSON_ENDDATE, rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE));
                    information.put(ExplorerCase.JSON_ENDDATEST, getDateFormat(rs.getLong(DatabaseDefinition.BDE_PROCESSINSTANCE_END_DATE)));
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
            }
        } finally {
            if (connectionResult != null && connectionResult.con != null)
                try {
                    connectionResult.con.close();
                } catch (Exception e) {
                }
        }
        return explorerCaseResult;
    }

    private String getDateFormat(Long dateTime) {
        if (dateTime == null)
            return null;
        try {
            return ExplorerCase.sdf.format(new Date(dateTime));
        } catch (Exception e) {
            return null;
        }
    }

   
}
