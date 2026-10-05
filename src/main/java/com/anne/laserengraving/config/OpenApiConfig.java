package com.anne.laserengraving.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PhotonCraft Laser Studio - RESTful API")
                        .description("Tài liệu đặc tả các giao diện lập trình ứng dụng (API) cho hệ thống điều khiển máy khắc Laser PhotonCraft GRBL Web Controller.\n\n"
                                + "### Các nhóm chức năng chính:\n"
                                + "- **Authentication (Xác thực)**: Đăng ký Gmail, Gửi & Xác thực mã OTP 6 số, Đăng nhập, Google OAuth 2.0.\n"
                                + "- **G-Code Generation**: Biên dịch ảnh raster (Dithering ITU-R BT.601, Floyd-Steinberg) và vector thành mã G-code chuẩn GRBL ($32=1).\n"
                                + "- **Lịch sử gia công**: Lưu trữ, truy xuất và tải về file `.nc` G-code.\n"
                                + "- **Cấu hình máy khắc**: Quản lý thông số phần cứng, giới hạn an toàn bước dịch chuyển, công suất PWM và tốc độ Feed rate.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PhotonCraft Laser Engineering Team")
                                .email("operator.laser@gmail.com")
                                .url("http://localhost:5173"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Máy chủ phát triển Local (Development Server)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập Bearer Token để xác thực các yêu cầu API")));
    }
}
