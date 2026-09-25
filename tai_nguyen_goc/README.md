# Tài nguyên gốc (không đóng vào APK)

`menu_data/` — ảnh món ăn theo danh mục, có từ bản đầu tiên của dự án (trước
đây nằm ở `app/src/main/assets/`). Không đoạn mã nào trong app đọc thư mục này:
ảnh món app hiển thị được tải từ máy chủ (Cloudinary / `images/`). Để trong
`assets/` thì 50 MB ảnh bị đóng thẳng vào APK (APK 65 MB → ~15 MB khi dời ra).

Dời ra đây ngày 25/09/2026 khi tối ưu hiệu năng app; giữ lại làm nguồn ảnh gốc.
