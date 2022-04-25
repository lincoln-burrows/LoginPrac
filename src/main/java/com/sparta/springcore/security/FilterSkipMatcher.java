package com.sparta.springcore.security;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

public class FilterSkipMatcher implements RequestMatcher {

    private final OrRequestMatcher orRequestMatcher;
    private final RequestMatcher   processingMatcher;

    public FilterSkipMatcher(
            List<String> pathToSkip,
            String processingPath
    ) {
        this.orRequestMatcher = new OrRequestMatcher(pathToSkip
                .stream()
                .map(this :: httpPath)
                .collect(Collectors.toList()));
        this.processingMatcher = new AntPathRequestMatcher(processingPath);
        System.out.println("FilterSkipMatcher 에서 나온 결과");

    }

    private AntPathRequestMatcher httpPath(String skipPath) {
        String[] splitStr = skipPath.split(",");

        /*
         * 배열 [1] httpMathod 방식 post get 인지 구분
         * 배열 [0] 제외하는 url
         * */
        System.out.println("FilterSkipMatcher AntPathRequestMatcher에서 나온 결과");
        return new AntPathRequestMatcher(
                splitStr[1],
                splitStr[0]
        );
    }

    @Override
    public boolean matches(HttpServletRequest req) {
        System.out.println("FilterSkipMatchers matches에서 나온 결과");

        return !orRequestMatcher.matches(req) && processingMatcher.matches(req);
    }
}
