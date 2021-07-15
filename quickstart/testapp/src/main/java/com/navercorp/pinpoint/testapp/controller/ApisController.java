package com.navercorp.pinpoint.testapp.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author koo.taejin
 */
import com.navercorp.pinpoint.testapp.util.Description;

@RestController(value = "apisController")
public class ApisController {

    private final RequestMappingHandlerMapping handlerMapping;

    private final Map<String, SortedSet<RequestMappedUri>> apiMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ApisController(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = Objects.requireNonNull(handlerMapping, "handlerMapping");
    }

    @PostConstruct
    private void initApiMappings() {
        Map<RequestMappingInfo, HandlerMethod> requestMappedHandlers = this.handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> requestMappedHandlerEntry : requestMappedHandlers.entrySet()) {
            RequestMappingInfo requestMappingInfo = requestMappedHandlerEntry.getKey();
            HandlerMethod handlerMethod = requestMappedHandlerEntry.getValue();

            Class<?> handlerMethodBeanClazz = handlerMethod.getBeanType();
            if (handlerMethodBeanClazz == this.getClass()) {
                continue;
            }

            String controllerName = handlerMethodBeanClazz.getSimpleName();
            Set<String> mappedRequests = requestMappingInfo.getPatternsCondition().getPatterns();

            SortedSet<RequestMappedUri> alreadyMappedRequests = this.apiMappings.get(controllerName);
            if (alreadyMappedRequests == null) {
                alreadyMappedRequests = new TreeSet<RequestMappedUri>(RequestMappedUri.MAPPED_URI_ORDER);
                this.apiMappings.put(controllerName, alreadyMappedRequests);
            }
            alreadyMappedRequests.addAll(createRequestMappedApis(handlerMethod, mappedRequests));
        }
    }

    private Set<RequestMappedUri> createRequestMappedApis(HandlerMethod handlerMethod, Set<String> mappedUris) {
        if (CollectionUtils.isEmpty(mappedUris)) {
            return Collections.emptySet();
        }
        Set<RequestMappedUri> requestMappedUris = new HashSet<RequestMappedUri>(mappedUris.size());
        Description description = handlerMethod.getMethodAnnotation(Description.class);
        for (String mappedUri : mappedUris) {
            requestMappedUris.add(new RequestMappedUri(mappedUri, description));
        }
        return requestMappedUris;
    }

    @GetMapping(value = {"/", "/index.html", "/apis"})
    public String apis(Model model) {
        model.addAttribute("apiMappings", this.apiMappings);
        return "apis";
    }

    public static class RequestMappedUri {

        private final String mappedUri;
        private final String description;

        private RequestMappedUri(String mappedUri, Description description) {
            if (mappedUri == null) {
                throw new IllegalArgumentException("mappedUri");
            }
            this.mappedUri = mappedUri;
            this.description = description == null ? "" : description.value();
        }

        public String getMappedUri() {
            return this.mappedUri;
        }

        public String getDescription() {
            return this.description;
        }

        private static final Comparator<RequestMappedUri> MAPPED_URI_ORDER = new Comparator<RequestMappedUri>() {
            @Override
            public int compare(RequestMappedUri arg0, RequestMappedUri arg1) {
                if (arg0 == null && arg1 == null) return 0;
                if (arg1 == null) return -1;
                if (arg0 == null) return 1;
                return String.CASE_INSENSITIVE_ORDER.compare(arg0.mappedUri, arg1.mappedUri);
            }
        };

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((description == null) ? 0 : description.hashCode());
            result = prime * result + ((mappedUri == null) ? 0 : mappedUri.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RequestMappedUri other = (RequestMappedUri) obj;
            if (description == null) {
                if (other.description != null)
                    return false;
            } else if (!description.equals(other.description))
                return false;
            if (mappedUri == null) {
                if (other.mappedUri != null)
                    return false;
            } else if (!mappedUri.equals(other.mappedUri))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "RequestMappedUri [mappedUri=" + mappedUri + ", description=" + description + "]";
        }
    }
}
