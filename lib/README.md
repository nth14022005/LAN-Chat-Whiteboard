# Libraries for Eclipse / RUN.bat

Bản lưu trên GitHub **không** chứa các tệp JAR nhị phân. Dùng Maven (xem `pom.xml`) là cách được đề xuất; Maven tự tải phụ thuộc.

Nếu muốn chạy với file `RUN.bat` hoặc cấu hình Eclipse gốc, chuẩn bị thư mục `lib/` chứa đúng hai tệp:
- `mssql-jdbc-12.8.1.jre8.jar` (Microsoft JDBC Driver)
- `flatlaf-3.5.4.jar` (FlatLaf)

Nguồn chính thức: Maven Central / dự án Microsoft mssql-jdbc và FormDev FlatLaf. Không tải JAR từ trang không tin cậy.
