# LAN Chat & Whiteboard Pro

Ứng dụng Java Swing hướng tới nhắn tin, truyền file và bảng vẽ cộng tác trong mạng LAN, xây dựng theo mô hình Client–Server. Đây là **dự án cá nhân có sử dụng AI hỗ trợ viết và chỉnh sửa mã nguồn**; chủ dự án đã thiết lập môi trường và chạy thử chức năng trong quá trình học tập.

## Các thành phần chính
- Java Swing, TCP Socket, UDP discovery; dữ liệu ứng dụng sử dụng SQL Server qua JDBC.
- `src/com/lanchat/main`: khởi động ứng dụng; `network`: kết nối mạng; `database`: truy cập dữ liệu; `model`: mô hình; `ui`: giao diện.
- `config/database.properties.example`: cấu hình mẫu, không chứa tài khoản hoặc mật khẩu thật.

## Chạy thử với Maven (đề xuất)
1. Cài JDK 17+, Apache Maven và Microsoft SQL Server.
2. Clone/tải mã nguồn. Trong thư mục gốc, chạy `mvn compile` để Maven tự tải thư viện FlatLaf và JDBC.
3. Sao chép `config/database.properties.example` thành `config/database.properties` trên máy của bạn và tự điền cấu hình SQL Server. Không tải file thật lên GitHub.
4. Chuẩn bị schema SQL Server phù hợp với các lớp DAO của ứng dụng. **Repository hiện không có script tạo schema hoàn chỉnh và không có dữ liệu mẫu; các chức năng cần CSDL sẽ không chạy đầy đủ nếu thiếu phần này.**
5. Chạy `mvn exec:java` từ thư mục gốc khi đã cấu hình xong.

## Dùng Eclipse / RUN.bat
Có thể nhập dự án như Maven project nếu Eclipse có hỗ trợ Maven (m2e). File `.classpath` và `RUN.bat` gốc vẫn trỏ tới hai JAR trong `lib/`, **nhưng bản trên GitHub chưa đóng gói JAR**. Hãy dùng Maven theo hướng dẫn trên, hoặc tự lấy thư viện theo `lib/README.md` để dùng cách chạy cũ.

## Trạng thái và giới hạn
- Mã nguồn Java đã được đưa về đúng `src/com/lanchat/...` tương ứng với khai báo `package`.
- Tài liệu kiểm thử `KIEM_THU.md` được lưu lại, nhưng **mã kiểm thử gốc trong `tests/` chưa được tải lên repository**; tài liệu không đồng nghĩa đã kiểm chứng được môi trường của người tải.
- Chưa xác minh hoàn chỉnh việc chạy trên hai máy Windows kết nối LAN thật. Chưa áp dụng TLS cho kênh TCP; **không mở cổng ứng dụng công khai ra Internet**.
- Không tải kèm mật khẩu SQL Server, bản sao lưu `.bak` hoặc dữ liệu riêng tư.

## Kết nối mạng thường dùng
Ứng dụng có thể sử dụng TCP 9999 và UDP 9998 trên mạng nội bộ; cần kiểm tra cấu hình và tường lửa trên máy thử nghiệm.
