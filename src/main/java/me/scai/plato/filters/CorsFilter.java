package me.scai.plato.filters;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by scai on 5/9/2015.
 */
public class CorsFilter extends OncePerRequestFilter {
    private final static String ORIGIN = "Origin";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
//        System.out.println(request.getHeader(ORIGIN));
//        System.out.println(request.getMethod());
        if (request.getHeader(ORIGIN) == null || request.getHeader(ORIGIN).equals("null")) {
//            String origin = request.getHeader(ORIGIN);
//            response.setHeader("Access-Control-Allow-Origin", "*");//* or origin as u prefer
//            response.setHeader("Access-Control-Allow-Credentials", "true");
//            response.setHeader("Access-Control-Allow-Headers",
//                request.getHeader("Access-Control-Request-Headers"));
        }

        if (request.getMethod().equals("OPTIONS")) {
            try {
                response.getWriter().print("OK");
                response.getWriter().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            filterChain.doFilter(request, response);
        }

    }
}
