1. Cách chạy quét lỗi vá bảo mật các gói
./mvnw dependency-check:aggregate                          # ~25s
./mvnw -Psecurity-scan verify -DskipTests                  # qua profile


P/s: Trường hợp cần key thì phải chạy lệnh set key trước:
$env:NVD_API_KEY = "3814AF4A-C4E6-4F5D-B518-7E78CD1E2A8A"