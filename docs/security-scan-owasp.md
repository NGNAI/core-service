# Quét lỗ hổng bảo mật với OWASP Dependency-Check

Tài liệu vận hành cho việc quét CVE dependency trong repo `core-service`.

## Chạy nhanh

```bash
# Cách 1 — gọi trực tiếp goal (khuyến nghị cho dev)
./mvnw dependency-check:aggregate

# Cách 2 — bật profile, scan tự chạy ở phase verify
./mvnw -Psecurity-scan verify -DskipTests

# Bỏ qua khi CI offline / cần build nhanh
./mvnw -Psecurity-scan verify -Ddependency-check.skip=true
```

Report sinh ra trong `target/`:

| File | Dùng để |
| --- | --- |
| `dependency-check-report.html` | Xem bằng trình duyệt (dễ đọc nhất) |
| `dependency-check-report.json` | Parse tự động, lưu lịch sử CVE |
| `dependency-check-report.sarif` | Upload GitHub Code Scanning / Azure DevOps |

## Vì sao dùng `aggregate` chứ không phải `check`

Repo là Maven multi-module (`core`, `common`, `admin-server`).

- `check` chạy **theo từng module** → mỗi module sinh một report riêng. Khi gọi
  `./mvnw dependency-check:check` từ thư mục gốc, chỉ có project root
  (`packaging=pom`) được chạy, còn 3 module con **bị bỏ qua hoàn toàn**.
- `aggregate` là aggregator mojo, chạy **một lần duy nhất ở pom gốc** và gom
  dependency của cả reactor → **một report duy nhất** cho toàn dự án.

Bắt buộc phải có `<inherited>false</inherited>`, nếu không module con kế thừa
aggregator mojo và Maven sẽ báo lỗi.

Kiểm chứng: report hiện tại quét được 136 dependency, bao gồm `org.example:common`
và `spring-boot-admin-server` của các module con.

## NVD API key (nên có)

Không có key vẫn chạy nhưng:

- Lần đầu tải toàn bộ NVD rất chậm (chục phút).
- Dễ bị rate-limit 429, plugin log nhiều `Unable to update`.

Lấy key miễn phí: <https://nvd.nist.gov/developers/request-an-api-key>

```bash
# PowerShell
$env:NVD_API_KEY = "xxxxx"
./mvnw dependency-check:aggregate
```

Pom đọc key qua `<nvdApiKeyEnvironmentVariable>NVD_API_KEY</nvdApiKeyEnvironmentVariable>`
— **không** hardcode key vào pom (rò rỉ qua git và Maven debug log,
xem GHSA-qqhq-8r2c-c3f5). Trên CI, đặt GitHub secret `NVD_API_KEY`.

Cache NVD giữ 24h (`nvdValidForHours`), nên chạy lại trong ngày chỉ mất ~20 giây.

## Các analyzer đã tắt và lý do

| Tham số | Giá trị | Lý do |
| --- | --- | --- |
| `knownExploitedEnabled` | `false` | Feed CISA `known_exploited_vulnerabilities.json` trả **403 Forbidden** từ nhiều mạng → plugin coi là fatal, dừng toàn bộ, build đỏ |
| `ossindexAnalyzerEnabled` | `false` | API Sonatype trả **401** khi không có tài khoản → analyzer chạy ~145s, lỗi từng artifact, không đóng góp CVE nào |
| `assemblyAnalyzerEnabled` | `false` | Project Java thuần; không có `dotnet` trên path → log ERROR mỗi lần chạy vì netty/snappy jar chứa `.dll` |
| `versionCheckEnabled` | `false` | Không spam mạng kiểm tra version plugin mỗi lần build |

### Bẫy tên tham số (đã từng gây lỗi)

```
[WARNING] Parameter 'cisaAnalyzerEnabled' is unknown for plugin 'dependency-check-maven:12.1.0'
```

Đây **chỉ là cảnh báo** — Maven bỏ qua tham số sai và plugin chạy với giá trị
mặc định. Nghĩa là analyzer **vẫn bật**, và vẫn chết vì 403. Tên đúng:

| Tên SAI (không tồn tại) | Tên ĐÚNG |
| --- | --- |
| `cisaAnalyzerEnabled` | `knownExploitedEnabled` |
| `ossIndexAnalyzerEnabled` | `ossindexAnalyzerEnabled` (chữ `i` thường) |

Muốn tự kiểm tra tên tham số của plugin bất kỳ:

```powershell
$jar = "$env:USERPROFILE\.m2\repository\org\owasp\dependency-check-maven\12.1.0\dependency-check-maven-12.1.0.jar"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($jar)
$e = $z.Entries | Where-Object { $_.Name -eq "plugin.xml" }
$sr = New-Object System.IO.StreamReader($e.Open()); $xml = [xml]$sr.ReadToEnd(); $sr.Close(); $z.Dispose()
($xml.plugin.mojos.mojo | Where-Object { $_.goal -eq "aggregate" }).parameters.parameter.name
```

> Ghi chú: tài liệu chính thức trên mạng đang hiển thị bản **13.0.0**, có thể khác
> bản đang dùng (**12.1.0**). Luôn đối chiếu `plugin.xml` trong local repo.

## `failOnError=false` — vì sao

Mặc định plugin fail cả goal khi bất kỳ analyzer/datasource lỗi (mạng, 403, 429).
Điều đó khiến build đỏ vì lý do **không liên quan tới code**. Đặt `false` để lỗi
mạng chỉ cảnh báo, report vẫn sinh với dữ liệu local.

Muốn CI đỏ khi có CVE nghiêm trọng thì dùng `failBuildOnCVSS`, **không** cần
`failOnError`:

```xml
<!-- pom.xml: chặn build khi có CVE >= 7.0 (HIGH/CRITICAL) -->
<failBuildOnCVSS>7</failBuildOnCVSS>
```

Hiện đặt `11` = không bao giờ fail (mặc định plugin), vì chưa xử lý xong danh sách
CVE bên dưới.

## Đọc kết quả: cẩn thận false-positive

Nguyên nhân gốc của false-positive: NVD gán CPE theo **cấp project**, không theo từng jar.
Ví dụ `vmware:spring_framework:7.0.7` áp cho **mọi** artifact `spring-*` version 7.0.7,
nên CVE của `spring-jms` bị gắn cho `spring-core`, CVE của `spring-webflux` cũng bị
gắn cho `spring-core`.

**Quan trọng:** KHÔNG phải mọi CVE "gán sai artifact" đều là false-positive.
Phải kiểm tra **class bị lỗi có thật trong jar không**, không chỉ nhìn tên artifact.

### Cách đọc đúng (đã dùng trong lần đánh giá 2026-11-05)

1. Mở HTML report, đọc `description` để lấy **FQCN** hoặc **tên class** bị lỗi.
2. Kiểm tra class đó có nằm trong jar không:

```powershell
# Liệt kê class trong jar, tìm theo tên
$jar = "$env:USERPROFILE\.m2\repository\io\netty\netty-handler\4.2.12.Final\netty-handler-4.2.12.Final.jar"
jar tf $jar | Select-String "SslClientHelloHandler"
```

3. Phân loại:

| Kết quả | Kết luận |
| --- | --- |
| Class **có** trong jar | Lỗ hổng **THẬT** (chỉ bị gán sai artifact) → phải nâng version |
| Class **không có** trong jar | **False-positive** → suppress |
| Description nói về **sản phẩm khác** (Prometheus server, Jakarta Mail, Spring Tools for Eclipse, validator.nu, Kotlin build cache) | **False-positive** → suppress |

### Ví dụ đã kiểm chứng

**THẬT** — `netty-transport-4.2.12.Final.jar` bị báo 62 CVE. Nhiều CVE mô tả class
thuộc module khác của Netty, nhưng các class đó **thật sự có** trong các jar cùng bộ:

| Class bị lỗi | Thật sự nằm trong |
| --- | --- |
| `DnsResolveContext` | `netty-resolver-dns-4.2.12.Final.jar` |
| `SslClientHelloHandler`, `SniHandler`, `IpSubnetFilterRule` | `netty-handler-4.2.12.Final.jar` |
| `HttpObjectDecoder`, `HttpClientCodec`, `SpdyHttpDecoder` | `netty-codec-http-4.2.12.Final.jar` |
| `Bzip2Decoder`, `Lz4FrameDecoder` | `netty-codec-compression-4.2.12.Final.jar` |
| `QpackDecoder` | `netty-codec-http3-4.2.12.Final.jar` |

→ Kết luận: **Netty 4.2.12 thật sự có lỗ hổng**, không phải false-positive.

**FALSE-POSITIVE** — đã kiểm chứng bằng `jar tf`:

| CVE | Artifact bị báo | Thực tế |
| --- | --- | --- |
| `CVE-2026-41855` | `spring-core` | Mô tả `org.springframework.jms.*`; spring-core không có package `jms` |
| `CVE-2022-31691` | `spring-boot-devtools` | Lỗi của **Spring Tools 4 for Eclipse** (IDE plugin) |
| `CVE-2026-53914` | `kotlin-stdlib*` | Lỗi ở **Kotlin build cache** (Gradle build-tools), không có trong stdlib |
| `CVE-2025-15104` | `hibernate-validator` | Lỗi của **validator.nu** (Nu Html Checker online) |
| `CVE-2026-42154` | `micrometer-registry-prometheus` | Lỗi của **Prometheus server** (endpoint `/api/v1/read`) |
| `CVE-2025-7962` | `angus-activation` | Lỗi SMTP của **angus-mail**; angus-activation chỉ có Mailcap/MimeType |

## Kết quả nâng cấp 2026-11-05 (đã đo thực tế)

Trước: **197 CVE duy nhất** (31 CRITICAL, 72 HIGH, 88 MEDIUM, 6 LOW) trên 136 dependency.
Sau: **0 CVE** trên 135 dependency.

### Thay đổi version

| Thành phần | Trước | Sau | Nguồn nâng |
| --- | --- | --- | --- |
| Spring Boot | 4.0.6 | **4.0.9** | pom gốc `<parent>` |
| spring-framework | 7.0.7 | 7.0.10 | Boot BOM |
| spring-security | 7.0.5 | 7.0.8 | Boot BOM |
| netty | 4.2.12.Final | 4.2.19.Final | Boot BOM |
| tomcat-embed-core | 11.0.21 | 11.0.27 | Boot BOM |
| jackson-databind (3.x) | 3.1.2 | 3.1.7 | Boot BOM |
| jackson-databind (2.x) | 2.21.2 | 2.21.7 | Boot BOM |
| spring-integration | 7.0.4 / **7.0.5 (pin)** | 7.0.7 | bỏ pin cứng trong `core/pom.xml` |
| spring-batch | 6.0.3 | 6.0.6 | Boot BOM |
| log4j-api | 2.25.4 | 2.25.5 | Boot BOM |
| hibernate-validator | 9.0.1.Final | 9.0.2.Final | Boot BOM |
| micrometer | 1.16.5 | 1.16.8 | Boot BOM |
| httpclient5 | 5.5.2 | **5.6.4** | override pom gốc (Boot không nâng) |
| httpcore5 | 5.3.6 | **5.4.3** | override pom gốc (Boot không nâng) |
| swagger-ui webjar | 5.18.3 | **5.34.1** | override pom gốc (DOMPurify 3.1.6 → 3.40.0) |
| spring-boot-admin | 4.0.3 / 4.0.4 | **4.1.4** | sửa trực tiếp (moment.js 2.30.1 → 2.31.0) |
| kotlin-stdlib-common | 1.9.10 (kéo bởi okio) | **loại bỏ** | exclude khỏi `okio-jvm` |

### 3 bẫy quan trọng khi nâng cấp

**1. Pin cứng version sẽ CHẶN BOM nâng cấp**

`core/pom.xml` từng ghi `<version>7.0.5</version>` cho `spring-integration-file`
để vá một CVE. Khi nâng Boot 4.0.9, BOM muốn đưa lên 7.0.7 nhưng bị pin cứng giữ lại
→ **14 CVE quay lại**. Quy tắc: chỉ pin khi Boot chưa vá; khi Boot vượt qua thì **xoá pin**.

**2. `kotlin-stdlib-common` từ Kotlin 2.x KHÔNG có file jar**

Từ Kotlin 2.x đây là artifact metadata-only (nội dung gộp vào `kotlin-stdlib`).
Ép `<version>${kotlin.version}</version>` sẽ fail:

```
Could not find artifact org.jetbrains.kotlin:kotlin-stdlib-common:jar:2.2.21
```

Cách đúng: **exclude** khỏi `okio-jvm` (dependency kéo nó vào), không ép version.

**3. CVE trong bundle JS/Swagger UI vẫn là bề mặt thật**

`swagger-ui` 5.18.3 nhúng DOMPurify 3.1.6 (23 CVE JS). Không phải code Java chạy
server, nhưng người dùng mở `/swagger-ui` vẫn bị ảnh hưởng XSS → cần nâng webjar.

Bật/tắt devtools và bảo mật môi trường: `spring-boot-devtools` là `optional`, không
vào production jar; CVE của nó (`CVE-2022-31691`) là false-positive.

## File suppression

`dependency-check-suppressions.xml` ở thư mục gốc. Đã cấu hình sẵn trong pom.

Mỗi rule có `<notes>` ghi lý do + ngày đánh giá và `<until>` buộc review lại:

```xml
<suppress until="2027-05-05Z">
    <notes><![CDATA[
        False-positive: CVE-2026-41855 mo ta org.springframework.jms.*
        nhung spring-core khong chua package nay. Danh gia 2026-11-05.
    ]]></notes>
    <packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@.*$</packageUrl>
    <cve>CVE-2026-41855</cve>
</suppress>
```

**Chỉ suppress false-positive hoặc đã chấp nhận rủi ro có ghi nhận.** CVE thật
phải xử lý bằng cách nâng version.

Lưu ý: nếu rule không match gì, plugin log
`Suppression Rule had zero matches: ...` → rule đã dư (CVE đã được vá bằng version),
xoá đi để tránh nhiễu.

## CI

`.github/workflows/security-scan.yml`:

- Chạy khi `pom.xml` thay đổi, khi mở PR, và **định kỳ thứ Hai hàng tuần**
  (bắt CVE mới công bố cho dependency cũ — dependency không đổi vẫn có thể thành lỗ hổng).
- Upload SARIF lên GitHub Code Scanning (nếu repo bật tính năng này).
- Upload HTML + JSON làm artifact, giữ 30 ngày.
- Ghi summary (số CVE theo severity + top 10 CRITICAL) vào trang Summary của lần chạy.

Cần đặt secret `NVD_API_KEY` trong repo settings.

## Checklist định kỳ (khuyến nghị hàng tháng)

1. Chạy `./mvnw dependency-check:aggregate`.
2. Mở HTML report, lọc **CRITICAL** và **HIGH**.
3. Với mỗi CVE: đọc description → xác định class bị ảnh hưởng → kiểm tra jar có
   chứa class đó không (dùng `jar tf`).
4. Phân loại:
   - **Thật** → nâng version dependency (hoặc lên kế hoạch nếu bị chặn bởi Spring Boot BOM).
   - **False-positive** → thêm `<suppress>` kèm notes + until.
5. Với dependency bị Spring Boot BOM quản lý version, ghi đè bằng `<properties>`
   hoặc `<dependencyManagement>` trong pom gốc khi cần patch gấp.
6. Xoá các `<suppress>` đã hết `<until>`.

## Liên quan

- `docs/monitoring-prometheus-grafana.md` — giám sát runtime
- `AGENTS.md` — quy ước chung của repo
