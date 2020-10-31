package org.bonitasoft.explorer.filter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.bonitasoft.engine.api.ProcessAPI;

public abstract class ShadowUrl {

    private final static Logger logger = Logger.getLogger(ShadowUrl.class.getName());
        private final static String LOGGER_LABEL = "ExternalAccess";

        public String datasource;
        protected ProcessAPI processAPI;
    public String[] url;
    public String[] urlMatcher;
    public Long processInstanceId;
    public Long localId;
    public Long sourceObjectId;
    public String processName; 
    public String processVersion;
    
    public ShadowUrl() {}
    
    public void setParameters(String datasource, ProcessAPI processAPI) {        
        this.datasource  = datasource;
        this.processAPI = processAPI;        
    }
    
    public boolean isMatch( String[] url) {
        this.url= url;
        for (int i=0;i<urlMatcher.length;i++)
        {
            if (url.length< i)
                return false;
            if (urlMatcher[ i ].equals("%")) {
            }
            else if (urlMatcher[ i ].equals("{{caseid}}"))
                processInstanceId = Long.valueOf(url[ i ]);
            else if (urlMatcher[ i ].equals("{{localid}}"))
                localId = Long.valueOf(url[ i ]);
            else if (urlMatcher[ i ].equals("{{sourceobjectid}}"))
                sourceObjectId = Long.valueOf(url[ i ]);
            else if (urlMatcher[ i ].equals("{{processname}}"))
                processName = url[ i ];
            else if (urlMatcher[ i ].equals("{{processversion}}"))
                processVersion = url[ i ];
            else if (! urlMatcher[ i ].equals(url[ i ]))
                return false;
        }
        return true;
    }
    
    protected Map<String,Object> caseInformation= new HashMap<>();
    public boolean isInExternalDatabase() {
        StringBuilder sqlRequest = new StringBuilder();
        List<Object> sqlParam = new ArrayList();
        // We can't include the JAR file, else we will have an error at execution
        sqlRequest.append("select * from Bonita_Process where ");
        if (localId != null ) {
            sqlRequest.append(" localid=?");
            sqlParam.add(localId );
        } else {
            sqlRequest.append(" processinstanceid=?");
            sqlParam.add(processInstanceId );
        }
        
        try (Connection con = getConnection(Arrays.asList(datasource)) ) {
            PreparedStatement pstmt = con.prepareStatement(sqlRequest.toString());
            for (int i = 0; i < sqlParam.size(); i++)
                pstmt.setObject(i + 1, sqlParam.get(i));

            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData rsmd=rs.getMetaData();  
            if (rs.next()) {
                for (int i=1;i<=rsmd.getColumnCount();i++) {
                    caseInformation.put( rsmd.getColumnName(i).toLowerCase(), rs.getObject(rsmd.getColumnName(i)));
                }            
                return true;
            }
            return false;
        }
        catch(Exception e) {
            logger.severe( LOGGER_LABEL+" Can't execute ["+sqlRequest+"] Param["+sqlParam+"]");
            return false;
        }
         
    }
    
    public abstract boolean doFilter(ServletRequest request, ServletResponse servletResponse);
    
    
    
    public static Connection getConnection(List<String> listDataSourceName) throws SQLException {
        // logger.info(loggerLabel+".getDataSourceConnection() start");
        
        
        List<String> listCompletedDataSourceName = new ArrayList<>();
        listCompletedDataSourceName.addAll(listDataSourceName);
        // hum, the datasource should start with a java:xxx
        for (String dataSourceName : listDataSourceName) {
            if (dataSourceName.startsWith("java:/comp/env/")
                    || dataSourceName.startsWith("java:jboss/datasources/"))
                continue;
            listCompletedDataSourceName.add("java:/comp/env/" + dataSourceName);
        }
        // logger.info(loggerLabel + ".getDataSourceConnection() check[" + dataSourceString + "]");

        for (String dataSourceIterator : listCompletedDataSourceName) {
            try {

                final Context ctx = new InitialContext();
                final Object dataSource = ctx.lookup(dataSourceIterator);
                if (dataSource == null)
                    continue;
                // in Postgres, this object is not a javax.sql.DataSource, but a specific Object.
                // see https://jdbc.postgresql.org/development/privateapi/org/postgresql/xa/PGXADataSource.html
                // but each has a method "getConnection()
                Method m = dataSource.getClass().getMethod("getConnection");

                return  (Connection) m.invoke(dataSource);
            } catch (NameNotFoundException e) {
                // nothing to do, expected
            } catch (NamingException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            }
        }
        // here ? We did'nt connect then
        logger.severe(LOGGER_LABEL + "Can't access database");

        return null;
    }

}
