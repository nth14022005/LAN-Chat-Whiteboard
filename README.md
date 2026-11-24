# LAN Chat & Whiteboard Pro

Dự án cá nhân Java Swing, sử dụng mô hình Client–Server cho nhắn tin và bảng vẽ trong mạng LAN. **Quá trình xây dựng mã nguồn có sử dụng AI hỗ trợ**; dự án được vận hành và kiểm tra chức năng trong quá trình học tập.

## Chức năng
- Chat nhóm/riêng, gửi file, bảng vẽ cộng tác với trạng thái khóa/mở.
- Giao tiếp qua TCP Socket; tìm máy Host bằng UDP; lưu trữ dữ liệu bằng SQL Server.
- Giao diện Swing và FlatLaf; có các bài kiểm thử trong `tests/`.

## Công nghệ
- Java 17+, Java Swing; TCP/UDP Socket
- Microsoft SQL Server, JDBC; FlatLaf
- Eclipse IDE (có `.project` và `.classpath`)

## Cách chạy bằng Eclipse
1. Chuẩn bị JDK 17 hoặc mới hơn và Eclipse.
2. Import thư mục project: **File → Import → General → Existing Projects into Workspace**.
3. Chuẩn bị một cơ sở dữ liệu SQL Server **phù hợp với schema ứng dụng yêu cầu**. Repository này **không** đính kèm cơ sở dữ liệu, file `.bak` hay script tạo schema hoàn chỉnh. Vì vậy không thể chạy đầy đủ chức năng dùng CSDL chỉ với riêng mã nguồn.
4. Trên máy Host, sao chép `config/database.properties.example` thành `config/database.properties`, rồi tự điền `url`, `user`, `password` theo SQL Server của mình. **Không commit file cấu hình chứa mật khẩu thật.**
5. Chạy `src/com/lanchat/main/MainApp.java` (Run As → Java Application). Hoặc dùng `RUN.bat` khi đã có JDK.
6. Khi dùng nhiều máy trong LAN, Client kết nối đến địa chỉ IP LAN của Host; ứng dụng mặc định có sử dụng TCP 9999 và UDP 9998.

## Cấu trúc
- `src/`: mã nguồn ứng dụng
- `tests/`: các bài kiểm thử và kết quả được cung cấp cùng dự án
- `lib/`: JDBC driver và FlatLaf, kèm thông tin giấy phép
- `config/database.properties.example`: mẫu cấu hình không chứa thông tin đăng nhập thật
- `KIEM_THU.md`: phạm vi và giới hạn kiểm thử được tài liệu gốc mô tả

## Giới hạn và lưu ý
- Mã nguồn hướng tới môi trường LAN tin cậy, chưa triển khai TLS giữa Client và Host. **Không mở cổng ứng dụng ra Internet công khai.**
- Tài liệu kiểm thử đi kèm cho biết đã thử Socket trong môi trường cục bộ, nhưng chưa xác nhận chạy thực tế trên hai máy Windows kết nối LAN với cơ sở dữ liệu của chủ dự án.
- File sao lưu SQL Server và dữ liệu phiên làm việc được cố ý loại khỏi repository vì quyền riêng tư.
- Để tạo một bản demo chạy độc lập cho nhà tuyển dụng, cần thêm cơ sở dữ liệu thử nghiệm không chứa dữ liệu cá nhân và hướng dẫn tạo schema.
