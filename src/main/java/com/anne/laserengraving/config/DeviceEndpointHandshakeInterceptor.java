package com.anne.laserengraving.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class DeviceEndpointHandshakeInterceptor implements HandshakeInterceptor {

    public static final String DEVICE_ENDPOINT_KEY = "deviceEndpoint";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest
                && servletRequest.getServletRequest().getRequestURI().endsWith("/ws/device")) {
            attributes.put(DEVICE_ENDPOINT_KEY, Boolean.TRUE);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake action is required.
    }
}
