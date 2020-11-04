package org.bonitasoft.explorer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.search.Order;
import org.bonitasoft.explorer.ExplorerAPI.Parameter;
import org.bonitasoft.explorer.ExplorerParameters.TYPEDATASOURCE;
import org.bonitasoft.explorer.bonita.BonitaAccessSQL;
import org.bonitasoft.explorer.external.ExternalAccess;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

public class ExplorerCase {

  
    
    public static class ExplorerCaseResult {

        public boolean isAdmin;
        public List<BEvent> listEvents = new ArrayList<>();

        public List<Map<String, Object>> listCases = new ArrayList<>();
        public List<Map<String, Object>> externalcase = new ArrayList<>();
        public List<Map<String, Object>> listTasks = new ArrayList<>();
        public List<Map<String, Object>> listComments = new ArrayList<>();
        public List<String> sqlRequests = new ArrayList<>();
        
        private Map<String, Long> chronos = new HashMap<>();
        private long totalChronos=0;
        
        public List<String> debugInformation = new ArrayList<>();
        
        
        public int totalNumberOfResult = 0;
        public int firstrecord = 0;
        public int lastrecord = 0;
        public int pageNumber;
        public boolean paginationIsPossible= false;
        
        public ExplorerCaseResult( boolean isAdmin) {
            this.isAdmin = isAdmin;
            
        }
        
        public void add( ExplorerCaseResult explorerExternal ) {
            listCases.addAll(explorerExternal.listCases);
            listTasks.addAll( explorerExternal.listTasks);
            listComments.addAll( explorerExternal.listComments);
            listEvents.addAll(explorerExternal.listEvents);
            debugInformation.addAll( explorerExternal.debugInformation);
            totalNumberOfResult += explorerExternal.totalNumberOfResult;
            sqlRequests.addAll(explorerExternal.sqlRequests);
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
            information.put("pagenumber", pageNumber);
            information.put("paginationispossible", paginationIsPossible);
            if (isAdmin) {
                information.put("debugInformation", debugInformation);
                information.put("sqlRequests", sqlRequests);
            }
            return information;
        }

        public Map<String, Object> getExternalCase(long caseId) {
             // search the process instance
            for (Map<String, Object> processVariable : externalcase) {
                if (processVariable.get( ExplorerJson.JSON_CASEID).equals(caseId))
                    return processVariable;
            }
            // not found : create one
            Map<String, Object> processVariable = new HashMap<>();
            processVariable.put( ExplorerJson.JSON_CASEID, caseId);
            processVariable.put( ExplorerJson.JSON_VARIABLES, new ArrayList<>());
            processVariable.put( ExplorerJson.JSON_DOCUMENTS, new ArrayList<>());
            externalcase.add(processVariable);
            return processVariable;
        }

    } // end Case Result

    /**
     * @param parameter
     * @return
     */
    public ExplorerCaseResult searchCases(Parameter parameter, ExplorerParameters explorerParameters) {
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult( parameter.isUserAdmin() );

        
        List<Boolean> listSearch = new ArrayList<>();
        if (parameter.searchActive)
            listSearch.add(true);
        if (parameter.searchArchive)
            listSearch.add(false);
        // first pass : pagination is working only with one source
        int countSource = listSearch.size();
        if (parameter.searchExternal)
            countSource++;
        if (countSource>1)
            parameter.pagenumber=0;
        // build search now            
        
        for (Boolean isActive : listSearch) {
            BonitaAccessSQL bonitaAccessSQL = new BonitaAccessSQL( null );
            long beginTime = System.currentTimeMillis();
            ExplorerCaseResult explorerExternal = bonitaAccessSQL.searchCases(parameter, isActive, false, explorerParameters, null);
            explorerCaseResult.addChronometer(Boolean.TRUE.equals(isActive)? "activeRequest": "archiveRequest", System.currentTimeMillis()-beginTime);
            explorerCaseResult.add(explorerExternal);
        }

        if (parameter.searchExternal) {
            explorerCaseResult.listEvents.addAll(explorerParameters.load(false));
            TYPEDATASOURCE typeDataSource = explorerParameters.getTypeExternalDatasource();
            
            
            if (TYPEDATASOURCE.EXTERNAL.equals( typeDataSource)) {
                ExternalAccess externalAccess = new ExternalAccess();
                long beginTime = System.currentTimeMillis();
                ExplorerCaseResult explorerExternal = externalAccess.searchCases(explorerParameters.getExternalDataSource(), explorerParameters.getPolicyOverview(), parameter);
                explorerCaseResult.addChronometer("external", System.currentTimeMillis()-beginTime);
                explorerCaseResult.add(explorerExternal);
            } else {
                BonitaAccessSQL externalAccess = new BonitaAccessSQL( explorerParameters.getExternalDataSource() );
                long beginTime = System.currentTimeMillis();
                ExplorerCaseResult explorerExternal = externalAccess.searchCases(parameter, false, true, explorerParameters, explorerParameters.getBonitaServerUrl());
                explorerCaseResult.addChronometer( "archiveRequest", System.currentTimeMillis()-beginTime);
                explorerCaseResult.add(explorerExternal);
            }            
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
                else if (o2==null)
                    compareValue= Integer.valueOf(1).compareTo(Integer.valueOf(0));
                else if (o1 instanceof Integer)                
                    compareValue = ((Integer) o1).compareTo( TypesCast.getInteger(o2, 0));
                else if (o1 instanceof Long)
                    compareValue = ((Long) o1).compareTo( TypesCast.getLong( o2, 0L));
                else if (o1 instanceof Date)
                    compareValue = ((Date) o1).compareTo( TypesCast.getDate( o2, null));
                else
                    compareValue = o1.toString().compareTo(o2 == null ? "" : o2.toString());

                // identical? CaseId is then the second comparaison
                if (compareValue==0) {
                    Long case1 = TypesCast.getLong(s1.get( ExplorerJson.JSON_CASEID), 0L);
                    Long case2 = TypesCast.getLong(s2.get( ExplorerJson.JSON_CASEID), 0L);
                    compareValue = case1.compareTo(case2);
                }
                return Order.ASC.equals(parameter.orderdirection) ? compareValue : -compareValue;
            }
        });

        // get the first row
        if (explorerCaseResult.listCases.size() > parameter.caseperpages)
            explorerCaseResult.listCases = explorerCaseResult.listCases.subList(0, parameter.caseperpages);
        explorerCaseResult.firstrecord = (parameter.pagenumber-1)*parameter.caseperpages+1;
        explorerCaseResult.lastrecord = explorerCaseResult.firstrecord-1+explorerCaseResult.listCases.size();
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
        ExplorerCaseResult explorerCaseResult = new ExplorerCaseResult(parameter.isUserAdmin());
        explorerCaseResult.listEvents.addAll(explorerParameters.load(false));
        if (BEventFactory.isError(explorerCaseResult.listEvents))
            return explorerCaseResult;
        
        ExternalAccess externalAccess = new ExternalAccess();
        
        if ( ExplorerJson.JSON_ORIGIN_V_EXTERNALCASE.equals(parameter.origin)) {
            explorerCaseResult = externalAccess.loadCase(explorerParameters.getExternalDataSource(), parameter);
        } else {
            BonitaAccessSQL bonitaAccess = new BonitaAccessSQL(null);
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
