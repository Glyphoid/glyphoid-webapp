package me.scai.plato.filters;

import com.google.gson.JsonPrimitive;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by scai on 5/9/2015.
 */
public class bypassSecurityFilter implements Filter {
    private ServletContext context;

    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        this.context = fConfig.getServletContext();
        this.context.log("RequestLoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        /* Examine the path to determine if the bypass security check is required */
        boolean bypassSecurity = false;
        String reqPath = req.getServletPath();
        String [] reqPathParts = reqPath.split("/");
        if (reqPathParts.length > 0 && reqPathParts[reqPathParts.length - 1].endsWith("-bypass")) {
            bypassSecurity = true;
        }

        /* TODO */

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        //we can close resources here
    }
}
