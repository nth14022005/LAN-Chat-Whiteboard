# Kiểm thử bản nâng cấp

Ngày: 17/11/2026.

- Biên dịch toàn bộ mã nguồn và tests thành Java 17: PASS (Eclipse compiler).
- 24 kiểm tra tự động: PASS. Chi tiết: tests/RESULTS.txt; mã kiểm thử: tests/RegressionTest.java.
- Mở LoginFrame và MainFrame bằng Swing + FlatLaf trên màn hình ảo: PASS; đã xem ảnh ở kích thước 1300×780 và 1080×680.
- JDBC và FlatLaf được đóng gói cùng project, đường dẫn .classpath là đường dẫn tương đối.
- Bản sao lưu cơ sở dữ liệu không được cung cấp trên repository công khai để tránh lộ dữ liệu.

## Kiểm tra tự động bao phủ

PBKDF2 và mật khẩu cũ; thêm hình từ nhiều người; Undo/Redo theo người; chống hoàn tác đè thay đổi người khác; sao chép độc lập đường bút; bộ lọc đối tượng mạng; hoàn tác di chuyển; từ chối mật khẩu sai; quyền Host và mã phòng do Server cấp; lịch sử chat; quyền khóa và sửa bảng; đồng bộ bảng cho người vào sau; gắn người gửi theo tài khoản; chat riêng đúng đích và không phát cho người thứ ba; truyền file chia gói đúng từng byte; lưu phiên và nạp lại sau khi khởi động Host.

Kiểm thử mạng mở các Socket TCP thật trên loopback. Tầng DataStore dùng dữ liệu giả lập để không động vào SQL Server. Kiểm thử này không chứng minh kết nối SQL Server ở máy người dùng đã hoạt động.

## Cần xác nhận tại máy người dùng

1. Eclipse dùng JDK 17/21, project không có dấu lỗi.
2. Host đăng nhập tài khoản có sẵn trong SQL Server; sai mật khẩu bị chặn.
3. Tạo tài khoản, gửi tin và xem dữ liệu mới trong các bảng đang có.
4. Chạy hai máy Windows cùng LAN: IP, Windows Firewall, dò UDP 9998 và TCP 9999.
5. Vẽ đồng thời, khóa bảng, gửi file, lưu/mở phiên và thử mất mạng.

Chưa có quyền truy cập SQL Server của người dùng, chưa restore .bak và chưa thử hai máy LAN thật. Không có script xóa/tạo lại CSDL trong gói nâng cấp.

Muốn chạy lại kiểm thử bằng JDK trên Windows: mở tests/RUN_TESTS.bat. Script chạy trong thư mục tạm, không sửa CSDL hoặc storage của project.

## Bản vá khóa/mở khóa

Đã thêm 33 kiểm tra `BoardSyncRegressionTest`, tất cả đạt, cộng với 24 kiểm tra cũ vẫn đạt. Kết quả ở `tests/BOARD_SYNC_RESULTS.txt`. Kiểm thử thao tác chuột Swing thật qua Client/Server TCP, 20 chu kỳ khóa/mở, xác nhận thao tác đúng ID, phản hồi không liên quan, hủy nét vẽ đang kéo khi khóa, yêu cầu đồng bộ lại sau 6 giây, mất kết nối, SQL ghi nét bị chặn, vẽ đồng thời, Undo/Redo.

Hướng dẫn cập nhật giữ nguyên cấu hình ở `CAP_NHAT_LOI_DONG_BO.txt`.
