package org.bonitasoft.explorer.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ShadowUrlCase extends ShadowUrl {
    public ShadowUrlCase() {
        super();
        urlMatcher = new String[] { "%", "XX", "portal", "form", "processInstance", "{{caseid}}" };
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse  servletResponse) {
        return true;

    }

}
