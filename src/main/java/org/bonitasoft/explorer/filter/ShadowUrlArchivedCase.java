package org.bonitasoft.explorer.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ShadowUrlArchivedCase extends ShadowUrl {
    public ShadowUrlArchivedCase() {
        super();
        urlMatcher = new String[] { "%", "XX", "portal", "form", "processInstance", "{{caseid}}" };
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse  servletResponse) {
        return true;

    }

}
