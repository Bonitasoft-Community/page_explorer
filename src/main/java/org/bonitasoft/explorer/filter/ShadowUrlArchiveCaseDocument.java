package org.bonitasoft.explorer.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ShadowUrlArchiveCaseDocument extends ShadowUrl {
    public ShadowUrlArchiveCaseDocument() {
        super();
        urlMatcher = new String[] { "%", "XX", "portal", "form", "processInstance", "{{caseid}}" };
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse  servletResponse) {
        // TODO Auto-generated method stub
        return true;
    }

}
