/**
 * Created by scai on 5/9/2015.
 */
package me.scai.plato.filters;

import org.apache.commons.lang.CharEncoding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet Filter implementation class RequestLoggingFilter
 */
@WebFilter("/RequestLoggingFilter")
public class RequestLoggingFilter implements Filter {

    private ServletContext context;

    public void init(FilterConfig fConfig) throws ServletException {
        this.context = fConfig.getServletContext();
        this.context.log("RequestLoggingFilter initialized");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        Enumeration<String> params = req.getParameterNames();
        while(params.hasMoreElements()){
            String name = params.nextElement();
            String value = request.getParameter(name);
            this.context.log(req.getRemoteAddr() + "::Request Params::{"+name+"="+value+"}");
        }

        StringBuilder sb = new StringBuilder();
        String reqPath = req.getServletPath();
        if (reqPath != null && reqPath.contains("j_spring_security_check")) {
            /* Get the body (payload) */
            sb.append("Query string =\"" + req.getQueryString() + "\"\n");

            /* Get the headers */
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                String headerVal = req.getHeader(headerName);
                sb.append("Header " + headerName + "=\"" + headerVal + "\"\n");
            }

//            BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));

//            StringBuilder bodyBuilder = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                bodyBuilder.append(line);
//            }
//
//            sb.append("Body: \"" + bodyBuilder.toString() + "\"");
        }

        Cookie[] cookies = req.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                this.context.log(req.getRemoteAddr() + "::Cookie::{"+cookie.getName()+","+cookie.getValue()+"}");
            }
        }



        // pass the request along the filter chain
        chain.doFilter(request, response);
    }

    public void destroy() {
        //we can close resources here
    }

}
