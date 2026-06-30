# Dev Container

This project ships with a dev container that isolates the Java and Maven toolchain from the host machine.

## Included toolchain

- JDK 25
- Maven 3.9.11

## How to use

1. Install Docker Desktop.
2. Install the VS Code extension `Dev Containers`.
3. In VS Code, run `Dev Containers: Reopen in Container`.

The container mounts the repository into `/workspaces/core-service` and keeps the local Maven repository in a named Docker volume so dependencies stay cached between rebuilds.

## Warning after run
# 1. Bảo Git bỏ qua việc check quyền file (Filemode)
git config core.filemode false

# 2. Bảo Git tự động xử lý ký tự xuống dòng (Bỏ qua sự khác biệt CRLF và LF) - nguyên nhân làm git thấy file thay đổi
git config core.autocrlf input