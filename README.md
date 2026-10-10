<div align="center">

# ⚙️ LaserGUI Backend — REST API & G-code Core Engine

### Máy chủ Dịch vụ Spring Boot 3 & Công cụ sinh mã G-code bất đồng bộ với Java 21 Virtual Threads (Project Loom)

[![Java 21](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x_/_4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Virtual Threads](https://img.shields.io/badge/Project_Loom-Virtual_Threads-blue?style=for-the-badge&logo=java)](https://openjdk.org/projects/loom/)
[![SQL Server](https://img.shields.io/badge/Database-MS_SQL_Server-CC292B?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/en-us/sql-server)
[![Spring Security](https://img.shields.io/badge/Security-HttpOnly_JWT-green?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Swagger](https://img.shields.io/badge/API_Docs-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8080/swagger-ui/index.html)
[![Author](https://img.shields.io/badge/Tác_giả-Vũ_Đình_Ân-blue?style=for-the-badge&logo=github)](https://github.com/AnVu05)
[![OJT](https://img.shields.io/badge/Đơn_vị_OJT-HYPERDATA_LAB-purple?style=for-the-badge)](https://github.com/AnVu05)
[![License](https://img.shields.io/badge/Giấy_phép-MIT-brightgreen?style=for-the-badge)](LICENSE)

<br />

<p align="center">
  <b>LaserGUI Backend</b> là dịch vụ máy chủ trung tâm phụ trách xử lý thuật toán sinh mã G-code đa luồng siêu tốc, quản lý cơ sở dữ liệu dự án bàn khắc 2D, lưu trữ lịch sử gia công và cung cấp hệ thống xác thực bảo mật chuẩn Enterprise cho ứng dụng <b>LaserGUI Web Controller</b>.
</p>

</div>

---

## 🗄️ Sơ Đồ Cơ Sở Dữ Liệu (Database ERD Diagram)

Hệ thống cơ sở dữ liệu được thiết kế trên **Microsoft SQL Server**, chuẩn hóa liên kết khoá ngoại (Foreign Keys) chặt chẽ giữa bảng người dùng trung tâm và các bảng chức năng:

<div align="center">
  <img src="./docs/images/database-erd.png" alt="LaserGUI Database ERD Schema" width="850" />
</div>

### Đặc tả Chi tiết Các Bảng Dữ Liệu (Entities Specification)

| Tên Bảng (Table) | Khóa Chính (PK) | Quan Hệ (FK) | Chức Năng Chính |
| :--- | :--- | :--- | :--- |
| `users` | `user_id` (UUID) | — | Quản lý tài khoản người dùng, mã băm mật khẩu BCrypt, vai trò (`USER`), nhà cung cấp xác thực (`LOCAL`, `GOOGLE`). |
| `laser_projects` | `project_id` (BIGINT) | `user_id` $\rightarrow$ `users` | Lưu trữ dữ liệu thiết kế bàn khắc 2D (khổ $300 \times 200\text{ mm}$), chuỗi JSON danh sách đối tượng (`elements_json`) và cấu hình tham số (`parameters_json`). |
| `gcode_history` | `history_id` (BIGINT) | `user_id` $\rightarrow$ `users` *(nullable)* | Quản lý lịch sử tạo mã và gia công: tổng số dòng lệnh, tốc độ cắt, công suất phát PWM, DPI, kích thước thực tế và liên kết file `.nc` vật lý. |
| `material_presets` | `preset_id` (BIGINT) | `user_id` $\rightarrow$ `users` *(nullable)* | Thư viện vật liệu mẫu hệ thống (`is_system = true`, 6 loại cơ bản) và vật liệu tùy chỉnh do người dùng tự thiết lập. |
| `laser_settings` | `setting_id` (BIGINT) | `user_id` $\rightarrow$ `users` *(nullable)* | Cấu hình tham số máy khắc mặc định: tốc độ gia công, gia tốc động cơ, giới hạn an toàn công suất PWM, khổ bàn làm việc. |

---

## 🚀 Tính Năng & Kiến Trúc Cốt Lõi (Key Capabilities)

### 1. ⚡ Xử Lý Đa Luồng Hiệu Năng Cao Với Java 21 Virtual Threads (Project Loom)
- Kích hoạt tính năng `spring.threads.virtual.enabled: true`.
- Toàn bộ các tác vụ tính toán nặng (biến đổi ma trận điểm ảnh raster ảnh bitmap $\rightarrow$ chuyển đổi tọa độ $\rightarrow$ xuất chuỗi lệnh G-code hàng vạn dòng) được điều phối trên hàng nghìn **Virtual Threads (Loom)** nhẹ nhàng, không gây tắc nghẽn luồng nền tảng (OS Carrier Threads) và tiết kiệm tối đa bộ nhớ RAM máy chủ.

### 2. 🛡️ Cơ Chế Bảo Mật & Xác Thực Zero-XSS (HttpOnly Cookie JWT)
- **Token an toàn**: JWT Access Token được đóng gói trực tiếp vào `Set-Cookie` với các cờ bảo mật:
  - `HttpOnly`: Trình duyệt ngăn chặn tuyệt đối mã JavaScript (XSS) truy cập token.
  - `SameSite=Lax`: Chống tấn công mạo danh yêu cầu chéo trang (CSRF).
  - `Path=/`: Đồng bộ phiên xuyên suốt mọi endpoint API.
- **Xác thực OTP qua Gmail**: Tích hợp dịch vụ email **Resend API**, tự động sinh mã OTP ngẫu nhiên 6 chữ số có hiệu lực trong 5 phút để kích hoạt tài khoản thật của người dùng.
- **Google OAuth Integration**: Hỗ trợ đăng nhập một chạm nhanh chóng qua Google Identity.

### 3. 🎯 Engine Sinh Mã G-Code Bất Đồng Bộ Chuẩn GRBL v1.1f
- Tiếp nhận tệp ảnh tải lên định dạng PNG, JPG, BMP qua cổng `POST /api/v1/gcode/generate`.
- Tự động áp dụng thuật toán phân tích điểm ảnh, chuẩn hóa Laser Mode `$32=1`, tọa độ tuyệt đối `G90`, đơn vị mm `G21`, điều chế xung tia `M3 S[0..1000]` và ngắt an toàn `M5`.
- Tự động lưu file nhị phân `.nc` vào thư mục lưu trữ cục bộ `storage/gcode/` có phân quyền và cung cấp endpoint tải xuống `GET /api/v1/gcode/download/{historyId}`.

### 4. 📦 Quản Lý Dữ Liệu Khởi Tạo Chuẩn Production (Clean Data Initializer)
- Thành phần [DataInitializer.java](file:///d:/Learn/Knowledge/OJT/src/backend/laser-engraving/src/main/java/com/anne/laserengraving/config/DataInitializer.java) được tinh gọn ở mức **cần và đủ**:
  - Tự động khởi tạo duy nhất danh mục **6 vật liệu mẫu chuẩn hệ thống** (Plywood, Acrylic, Da, MDF, Carton, Nhôm) khi triển khai lần đầu trên database mới.
  - Tuyệt đối **không chèn tài khoản giả lập, không chèn lịch sử hay dự án ảo**, sẵn sàng cho môi trường Production thực tế.

---

## 🔌 Danh Mục RESTful API Endpoints

Tài liệu tương tác trực tiếp Swagger UI có sẵn tại: `http://localhost:8080/swagger-ui/index.html`

### 1. Xác thực & Tài khoản (`/api/v1/auth`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản Gmail mới & gửi mã OTP 6 số |
| `POST` | `/api/v1/auth/verify-otp` | Public | Xác thực mã OTP, kích hoạt tài khoản & cấp HttpOnly Cookie |
| `POST` | `/api/v1/auth/resend-otp` | Public | Gửi lại mã kích hoạt sau 59 giây đếm ngược |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập tài khoản & thiết lập HttpOnly Cookie |
| `POST` | `/api/v1/auth/google` | Public | Đăng nhập hoặc đăng ký nhanh qua Google OAuth |
| `POST` | `/api/v1/auth/logout` | Authenticated | Đăng xuất an toàn & thu hồi HttpOnly Cookie |
| `GET` | `/api/v1/auth/me` | Authenticated | Lấy thông tin tài khoản đang đăng nhập |

### 2. Sinh mã G-Code (`/api/v1/gcode`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/gcode/generate` | Public / User | Upload ảnh và biên dịch ra mã G-code bất đồng bộ |
| `GET` | `/api/v1/gcode/download/{historyId}` | Public / User | Tải tệp G-code vật lý `.nc` về máy tính |

### 3. Dự án Bàn khắc 2D (`/api/v1/projects`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/projects` | Authenticated | Lưu mới hoặc cập nhật dự án bàn khắc kèm các đối tượng JSON |
| `GET` | `/api/v1/projects` | Authenticated | Lấy danh sách dự án của người dùng |
| `GET` | `/api/v1/projects/{projectId}` | Authenticated | Lấy thông tin chi tiết một dự án để nạp lên bàn khắc |
| `DELETE` | `/api/v1/projects/{projectId}` | Authenticated | Xóa vĩnh viễn dự án khỏi CSDL |

### 4. Lịch sử Gia công (`/api/v1/history`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/history` | Public / User | Lưu thông số và mã G-code lịch sử sau khi hoàn tất gia công |
| `GET` | `/api/v1/history` | Authenticated | Lấy danh sách lịch sử có hỗ trợ phân trang (`page`, `size`) |
| `GET` | `/api/v1/history/{historyId}` | Authenticated | Xem chi tiết 1 bản ghi gia công |
| `DELETE` | `/api/v1/history/{historyId}` | Authenticated | Xóa bản ghi lịch sử |

### 5. Thư viện Vật liệu Mẫu (`/api/v1/materials`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/materials` | Public / User | Lấy danh sách vật liệu mẫu (Hệ thống + Tùy chỉnh của người dùng) |
| `GET` | `/api/v1/materials/{id}` | Public / User | Lấy thông tin vật liệu theo ID |
| `GET` | `/api/v1/materials/key/{key}` | Public / User | Lấy thông số theo mã nhận diện key (VD: `mat-plywood`) |
| `POST` | `/api/v1/materials` | Authenticated | Tạo hồ sơ thông số vật liệu mới |
| `PUT` | `/api/v1/materials/{id}` | Authenticated | Chỉnh sửa thông số vật liệu tùy chỉnh |
| `DELETE` | `/api/v1/materials/{id}` | Authenticated | Xóa vật liệu tùy chỉnh |
| `POST` | `/api/v1/materials/init-defaults`| Admin / System | Nạp lại 6 vật liệu mẫu mặc định |

### 6. Cấu hình Máy Laser (`/api/v1/settings`)
| Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/settings` | Public / User | Lấy thông số vận hành mặc định của máy laser |
| `PUT` | `/api/v1/settings` | Public / User | Cập nhật thông số giới hạn an toàn máy khắc |

---

## 📁 Cấu Trúc Thư Mục Backend

```text
src/backend/laser-engraving/
├── docs/                                   # Tài liệu & hình ảnh kiến trúc CSDL
│   └── images/
│       └── database-erd.png                # Sơ đồ quan hệ thực thể (ERD) SQL Server
├── src/
│   ├── main/
│   │   ├── java/com/anne/laserengraving/
│   │   │   ├── config/                     # Cấu hình Spring Security, CORS, DataInitializer
│   │   │   │   ├── ApplicationConfig.java  # Cấu hình Bean hệ thống, Mã hóa mật khẩu
│   │   │   │   ├── CorsConfig.java         # Cấu hình CORS cho cổng 5173
│   │   │   │   ├── DataInitializer.java    # Khởi tạo 6 vật liệu mẫu lúc khởi động
│   │   │   │   └── SecurityConfig.java     # Chuỗi lọc bảo mật Spring Security & JWT
│   │   │   ├── controller/                 # Tầng điều hướng REST API Endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── GcodeController.java
│   │   │   │   ├── HistoryController.java
│   │   │   │   ├── LaserSettingsController.java
│   │   │   │   ├── MaterialPresetController.java
│   │   │   │   └── ProjectController.java
│   │   │   ├── dto/                        # Đối tượng truyền dữ liệu (Data Transfer Objects)
│   │   │   │   ├── request/                # Các DTO nhận vào từ client
│   │   │   │   └── response/               # Các DTO phản hồi chuẩn hóa
│   │   │   ├── entity/                     # Các lớp thực thể JPA ánh xạ CSDL SQL Server
│   │   │   │   ├── GcodeHistoryEntity.java
│   │   │   │   ├── LaserSettingsEntity.java
│   │   │   │   ├── MaterialPresetEntity.java
│   │   │   │   ├── ProjectEntity.java
│   │   │   │   └── UserEntity.java
│   │   │   ├── exception/                  # Bộ xử lý ngoại lệ toàn cục (Global Exception)
│   │   │   ├── repository/                 # Giao diện truy vấn dữ liệu Spring Data JPA
│   │   │   ├── security/                   # Triển khai JWT, HttpOnly Cookie, Bộ lọc Auth
│   │   │   ├── service/                    # Tầng nghiệp vụ xử lý logic & Virtual Threads
│   │   │   │   ├── GcodeService.java       # G-code Generation Engine
│   │   │   │   ├── AuthService.java        # Quản lý tài khoản, mã hóa mật khẩu, OTP
│   │   │   │   ├── MailService.java        # Gửi email mã OTP qua Resend
│   │   │   │   ├── ProjectService.java
│   │   │   │   ├── HistoryService.java
│   │   │   │   └── MaterialPresetService.java
│   │   │   └── LaserEngravingApplication.java # Điểm khởi động Spring Boot
│   │   └── resources/
│   │       ├── application.yaml            # Tệp cấu hình máy chủ, CSDL SQL Server & Port
│   │       └── application-local.yaml      # Cấu hình môi trường phát triển cục bộ
│   └── test/                               # Bộ kiểm thử tự động (23/23 Tests Passed)
├── storage/                                # Thư mục lưu trữ các tệp G-code .nc vật lý
│   └── gcode/
├── mvnw & mvnw.cmd                         # Maven Wrapper thực thi không cần cài đặt Maven
└── pom.xml                                 # Quản lý thư viện phụ thuộc dự án
```

---

## 🛠️ Yêu Cầu Môi Trường & Cài Đặt (Prerequisites)

1. **Java Development Kit (JDK)**:
   - Phiên bản: **Java 21 LTS** (Oracle JDK hoặc Eclipse Temurin).
   - Kiểm tra phiên bản: `java -version`.
2. **Cơ sở dữ liệu**:
   - **Microsoft SQL Server** (2019, 2022 hoặc SQL Server Express).
   - Đã tạo sẵn Database có tên: `LASER`.
   - Tài khoản người dùng: `sa` (Mật khẩu: `sa` hoặc cấu hình tùy biến qua biến môi trường).

---

## 🚀 Hướng Dẫn Khởi Chạy Ứng Dụng (Quick Start)

### 1. Cấu hình Cơ sở dữ liệu (application.yaml)
Cấu hình mặc định kết nối đến SQL Server cục bộ trên cổng `1433`:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:sqlserver://localhost:1433;databaseName=LASER;encrypt=true;trustServerCertificate=true}
    username: ${SPRING_DATASOURCE_USERNAME:sa}
    password: ${SPRING_DATASOURCE_PASSWORD:sa}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
```

*Bạn có thể ghi đè các tham số này thông qua các biến môi trường hệ thống mà không cần sửa code.*

### 2. Khởi chạy máy chủ Spring Boot

```bash
# Di chuyển vào thư mục backend
cd src/backend/laser-engraving

# Chạy ứng dụng bằng Maven Wrapper (Windows PowerShell)
.\mvnw spring-boot:run

# Hoặc trên Linux / macOS:
./mvnw spring-boot:run
```

Sau khi khởi chạy thành công, máy chủ lắng nghe tại:
- **REST API Base URL**: `http://localhost:8080/api/v1`
- **Tài liệu Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Schema JSON**: `http://localhost:8080/v3/api-docs`

### 3. Chạy toàn bộ Bộ kiểm thử tự động (Automated Tests)

```bash
.\mvnw test
```
*Kết quả kiểm thử chuẩn: Toàn bộ **23/23 bài kiểm thử đơn vị và tích hợp đều đạt 100% BUILD SUCCESS**.*

---

## 👨‍💻 Thông Tin Tác Giả & Đơn Vị Phát Triển

- **Tác giả phát triển**: **Vũ Đình Ân**
- **GitHub**: [@AnVu05](https://github.com/AnVu05)
- **Đơn vị OJT / Nghiên cứu**: **HYPERDATA LAB**
- **Dự án**: Nghiên cứu & Chế tạo Ứng dụng Web Điều khiển Máy Khắc Laser CNC phục vụ đồ án OJT.

---

## 📄 Giấy Phép (License)

Dự án được phân phối dưới giấy phép **MIT License**. Bạn được tự do sử dụng, chỉnh sửa và tích hợp cho mục đích học tập và nghiên cứu. Chi tiết xem tại tệp `LICENSE`.

<div align="center">
  <sub>Xây dựng với Java 21, Spring Boot và niềm đam mê công nghệ phần mềm chuyên sâu 🚀</sub>
</div>
