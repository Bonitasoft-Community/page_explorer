package org.bonitasoft.explorer.filter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor;

public class ShadowUrlProcessInstance extends ShadowUrl {

    private final static Logger logger = Logger.getLogger(ShadowUrlProcessInstance.class.getName());
    private final static String LOGGER_LABEL = "ShadowUrlProcessInstance";

    /**
     * We manage http://localhost:8080/bonita/portal/form/processInstance/2001
     */
    public ShadowUrlProcessInstance() {
        super();
        urlMatcher = new String[] { "%", "bonita", "portal", "form", "processInstance", "{{caseid}}" };
    }

    @Override
    public boolean doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
        // this url return an HTML. So, let find an existing case, and replace the caseid by this new case: portal will send the html.
        return false; // not managed, continue
        /*
        SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 10);
        String newURI="";
        try {
            SearchResult<ProcessInstance> search = processAPI.searchProcessInstances(sob.done());
            Long processInstanceCandidate = null;
            if (search.getCount() > 0)
                processInstanceCandidate = search.getResult().get(0).getId();

            if (processInstanceCandidate == null)
                return false;

            // build the chaine now
            newURI = "/bonita/portal/form/processInstance/" + processInstanceCandidate;
            servletRequest.getRequestDispatcher(newURI).forward(servletRequest, servletResponse);
            return true;
        } catch (Exception e) {
            logger.severe(LOGGER_LABEL + "Error during redispatcher to [" + newURI + "] :"+e.getMessage());
        }
        return false;
*/
    }

}
