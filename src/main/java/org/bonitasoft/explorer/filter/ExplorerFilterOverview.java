package org.bonitasoft.explorer.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.bonitasoft.console.common.server.login.HttpServletRequestAccessor;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;

public class ExplorerFilterOverview implements Filter {

    /**
     * Set it in the webapps/bonita/WEB_INF/web.xml
     * <!-- ExplorerFilter --> <filter> <filter-name>ExplorerFilter</filter-name>
     * <filter-class>org.bonitasoft.explorer.filter.ExplorerFilterOverview</filter-class>
     * ___ <init-param> <param-name>datasource</param-name>
     * ___ <param-value>ExplorerData</param-value> </init-param> </filter>
     * <filter-mapping> 
     * <filter-name>ExplorerFilter</filter-name>
     * ___ <url-pattern>/bonita/portal/form/processInstance</url-pattern>
     * ___ <url-pattern>/bonita/portal/resource/processInstance</url-pattern>
     * ___ <url-pattern>/bonita/API/bpm/archivedCase</url-pattern>
     * ___ <url-pattern>/bonita/portal/resource/processInstance/</url-pattern>
     * </filter-mapping>
     */
    public Logger logger = Logger.getLogger(ExplorerFilterOverview.class.getName());
    public String logHeader = "--------------------- filter Explorer ";

    public String datasource = "";

    public void init(final FilterConfig filterConfig) throws ServletException {
        datasource = filterConfig.getInitParameter("datasource");

    }

    // /bonita/portal/form/processInstance
    /*
     * private List<String> listUrlFilter = Arrays.asList(
     * "bonita/portal/resource/processInstance/",
     * // "/API/bpm/case/",
     * "../API/bpm/archivedCase?c=1&d=started_by&d=processDefinitionId&f=sourceObjectId%3D{{caseid}}&p=0",
     * "/bonita/portal/resource/processInstance/defaultoverview/1.0/API/bpm/case/15/context",
     * "/API/bpm/caseVariable/:caseId/:variableName",
     * "/bonita/portal/resource/processInstance/defaultoverview/1.0/API/bpm/archivedCaseDocument?f=caseId=16");
     */
    private List<Class> listUrlShadow = Arrays.asList(ShadowUrlProcessInstance.class,
            ShadowUrlCase.class,
            ShadowUrlArchivedCase.class,
            ShadowUrlArchiveCaseDocument.class,
            ShadowUrlPortalResourceContext.class);

    /**
     * Filter, then study if we are connected. If not, that's a false
     * connection. Yes, reset the number of tentative to 0.
     */
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
        // final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        // is this is a overview URL ? 
        logger.info(logHeader + "filterExplorer");
        ProcessAPI processAPI = null;
        try {
            final HttpServletRequestAccessor requestAccessor = new HttpServletRequestAccessor(httpRequest);
            if (requestAccessor != null) {
                final APISession apiSession = requestAccessor.getApiSession();
                if (apiSession != null) {
                    processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
                }
            }
        } catch (Exception e) {

        }

        String[] url = decodeUrl(httpRequest.getRequestURI());
        
        for (Class shadowUrlClass : listUrlShadow) {
            ShadowUrl shadowUrl;
            try {
                shadowUrl = (ShadowUrl) shadowUrlClass.newInstance();
                shadowUrl.setParameters(datasource, processAPI);
                if (shadowUrl.isMatch(url)) {
                    if (shadowUrl.isInExternalDatabase()) {
                        boolean isManaged = shadowUrl.doFilter(servletRequest, servletResponse);
                        
                        if ( ! isManaged)
                            chain.doFilter(httpRequest, servletResponse);
                        
                    } else {
                        chain.doFilter(httpRequest, servletResponse);
                    }
                    return;
                }
            } catch (Exception e) {
                logger.info(logHeader + "Error " + e.getLocalizedMessage());
            }

        }
        chain.doFilter(httpRequest, servletResponse);

        return;
    }

    @Override
    public void destroy() {

    }

    private String[] decodeUrl(String url) {
        return url.split("/");
    }
    // ../API/bpm/archivedCase?c=1&d=started_by&d=processDefinitionId&f=sourceObjectId%3D{{caseid}}&p=0
    /*
     * [{"end_date":"2020-10-16 11:39:27.384","archivedDate":"2020-10-16 11:39:27.384","searchIndex5Label":"age","processDefinitionId":{"displayDescription":"",
     * "deploymentDate":"2020-10-16 11:39:11.530","displayName":"GrandParent","name":"GrandParent","description":"","deployedBy":"4","id":"5416407333892612069",
     * "activationState":"ENABLED","version":"1.0","configurationState":"RESOLVED","last_update_date":"2020-10-16 11:39:13.708","actorinitiatorid":"119"},
     * "searchIndex3Value":"China","searchIndex4Value":"Male","searchIndex2Label":"hair","start":"2020-10-16 11:39:19.999","searchIndex1Value":"green",
     * "sourceObjectId":"12","searchIndex3Label":"country","startedBySubstitute":"4","searchIndex5Value":"age 12","searchIndex2Value":"chatain fonce"
     * ,"rootCaseId":"12","id":"38","state":"completed","searchIndex1Label":"eyes","started_by":{"firstname":"Walter","icon":"icons/default/icon_user.png",
     * "creation_date":"2020-10-14 14:33:16.623","userName":"walter.bates","title":"Mr","created_by_user_id":"-1","enabled":"true","lastname":"Bates",
     * "last_connection":"2020-10-16 11:39:14.500","password":"","manager_id":"3","id":"4","job_title":"Human resources benefits"
     * ,"last_update_date":"2020-10-14 14:33:16.623"},"searchIndex4Label":"gender","last_update_date":"2020-10-16 11:39:27.384"}]
     */

    /**
     * bonita/portal/resource/processInstance/defaultoverview/1.0/API/bpm/archivedCase/42/context
     * {
     * "grandparent_ref": {
     * "id": 44,
     * "processInstanceId": 30,
     * "name": "grandparent",
     * "author": 4,
     * "creationDate": 1602879500478,
     * "fileName": "aDoc 1.pdf",
     * "contentMimeType": "application/octet-stream",
     * "contentStorageId": "44",
     * "url": "documentDownload?fileName=aDoc 1.pdf&contentStorageId=44",
     * "description": "",
     * "version": "1",
     * "index": -1,
     * "contentFileName": "aDoc 1.pdf"
     * },
     * "emptyDocument_ref": null,
     * "invoiceGrandParent_ref": {
     * "name": "invoiceGrandParent",
     * "type": "com.company.model.Invoice",
     * "link": "API/bdm/businessData/com.company.model.Invoice/findByIds?ids=68,69",
     * "storageIds": [
     * 68,
     * 69
     * ],
     * "storageIds_string": [
     * "68",
     * "69"
     * ]
     * },
     * "listOneDocument_ref": [
     * {
     * "id": 46,
     * "processInstanceId": 30,
     * "name": "listOneDocument",
     * "author": 4,
     * "creationDate": 1602879500609,
     * "fileName": null,
     * "contentMimeType": null,
     * "contentStorageId": "46",
     * "url": "http://www.lemonde.fr",
     * "description": null,
     * "version": "1",
     * "index": 0,
     * "contentFileName": null
     * }
     * ],
     * "bildGrandParent_ref": {
     * "name": "bildGrandParent",
     * "type": "com.company.model.Invoice",
     * "link": "API/bdm/businessData/com.company.model.Invoice/70",
     * "storageId": 70,
     * "storageId_string": "70"
     * },
     * "urlDocument_ref": {
     * "id": 45,
     * "processInstanceId": 30,
     * "name": "urlDocument",
     * "author": 4,
     * "creationDate": 1602879500478,
     * "fileName": null,
     * "contentMimeType": "application/octet-stream",
     * "contentStorageId": "45",
     * "url": "http://comunity/truckmilk",
     * "description": "",
     * "version": "1",
     * "index": -1,
     * "contentFileName": null
     * },
     * "listExamples_ref": null
     * }
     */

    /**
     * bonita/portal/resource/processInstance/defaultoverview/1.0/API/bpm/archivedCaseDocument?f=caseId=16
     * [{"submittedBy":"4","archivedDate":"2020-10-16
     * 12:33:33.503","fileName":"","author":"4","contentStorageId":"22","description":"","index":"-1","sourceObjectId":"22","creationDate":"2020-10-16
     * 12:33:09.430","version":"1","contentMimetype":"application/octet-stream","url":"http://myged.com","isInternal":"false","caseId":"16","name":"deliverydoc","id":"22"}]
     */

}
