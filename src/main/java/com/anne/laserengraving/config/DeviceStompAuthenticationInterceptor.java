package com.anne.laserengraving.config;

import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.service.DeviceCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class DeviceStompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String DEVICE_SESSION_KEY = "authenticatedDeviceCode";
    private final DeviceCredentialService credentialService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        boolean deviceEndpoint = accessor.getSessionAttributes() != null
                && Boolean.TRUE.equals(accessor.getSessionAttributes().get(DeviceEndpointHandshakeInterceptor.DEVICE_ENDPOINT_KEY));
        if (!deviceEndpoint) return message;

        if (accessor.getCommand() == StompCommand.CONNECT) {
            String code = accessor.getFirstNativeHeader("x-device-code");
            String authorization = accessor.getFirstNativeHeader("authorization");
            DeviceEntity device = credentialService.authenticate(code, authorization);
            if (accessor.getSessionAttributes() == null) accessor.setSessionAttributes(new HashMap<>());
            accessor.getSessionAttributes().put(DEVICE_SESSION_KEY, device.getDeviceCode());
            accessor.setUser((Principal) device::getDeviceCode);
            return message;
        }

        String deviceCode = authenticatedDeviceCode(accessor);
        String destination = accessor.getDestination();
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            if (destination == null || !destination.equals("/topic/device/" + deviceCode + "/jobs")) {
                throw new SecurityException("Thiết bị chỉ được subscribe topic job của chính nó.");
            }
        } else if (accessor.getCommand() == StompCommand.SEND) {
            if (destination == null || !destination.startsWith("/app/device/")) {
                throw new SecurityException("Thiết bị chỉ được gửi message tới endpoint device.");
            }
        }
        return message;
    }

    private String authenticatedDeviceCode(StompHeaderAccessor accessor) {
        Object code = accessor.getSessionAttributes() == null
                ? null : accessor.getSessionAttributes().get(DEVICE_SESSION_KEY);
        if (!(code instanceof String deviceCode) || deviceCode.isBlank()) {
            throw new SecurityException("STOMP session chưa được xác thực thiết bị.");
        }
        return deviceCode;
    }
}
