package com.example.vmserver.controller;

import com.example.vmserver.model.VMStation;
import com.example.vmserver.service.VMStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import com.example.vmserver.dto.ReportDTO;
import com.example.vmserver.enums.VMState;
import com.example.vmserver.service.ReportService;
import org.springframework.http.HttpHeaders;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Контроллер станций", description = "API для управления виртуальными машинами")
public class VMStationController {
    private final VMStationService stationService;
    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAuthority('STATION:CREATE')")
    @Operation(summary = "Создание новой станции", 
               description = "Создает новую виртуальную машину-станцию")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Станция успешно создана")
    })
    public ResponseEntity<VMStation> createStation(
            @Parameter(description = "Данные для создания станции", required = true)
            @RequestBody VMStation station) {
        return ResponseEntity.ok(stationService.createStation(station));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STATION:DELETE')")
    @Operation(summary = "Удаление станции по ID", 
               description = "Удаляет виртуальную машину-станцию по указанному идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Станция успешно удалена")
    })
    public ResponseEntity<Void> deleteStation(
            @Parameter(description = "ID станции", required = true, example = "1")
            @PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STATION:UPDATE')")
    @Operation(summary = "Обновление данных станции", 
               description = "Обновляет информацию о виртуальной машине-станции")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные станции успешно обновлены")
    })
    public ResponseEntity<VMStation> updateStation(
            @Parameter(description = "ID станции", required = true, example = "2")
            @PathVariable Long id, 
            @Parameter(description = "Обновленные данные станции", required = true)
            @RequestBody VMStation stationDetails) {
        return ResponseEntity.ok(stationService.updateStation(id, stationDetails));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STATION:GETALL')")
    @Operation(summary = "Получение списка всех станций", 
               description = "Возвращает список всех виртуальных машин-станций")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список станций успешно получен")
    })
    public ResponseEntity<List<VMStation>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STATION:GETID')")
    @Operation(summary = "Получение станции по ID", 
               description = "Возвращает данные виртуальной машины-станции по идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Станция найдена")
    })
    public ResponseEntity<VMStation> getStationById(
            @Parameter(description = "ID станции", required = true, example = "3")
            @PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }
    
    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('STATION:FILTER')")
    @Operation(summary = "Фильтрация станций с пагинацией", 
               description = "Возвращает отфильтрованный список станций с поддержкой пагинации. "
                           + "Можно фильтровать по логину и диапазону значений")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Отфильтрованный список успешно получен")
    })
    public ResponseEntity<Object> getByFilter(
            @Parameter(description = "Логин для фильтрации (частичное совпадение)", required = false, example = "user123")
            @RequestParam(required = false) String login,
            @Parameter(description = "Минимальное значение для фильтрации", required = false, example = "0")
            @RequestParam(required = false) Integer min,
            @Parameter(description = "Максимальное значение для фильтрации", required = false, example = "100")
            @RequestParam(required = false) Integer max,
            @Parameter(description = "Параметры пагинации и сортировки", required = false)
            @PageableDefault(page = 0, size = 10, sort = "login") Pageable pageable) {
        return ResponseEntity.ok(stationService.getByFilter(login, min, max, pageable));
    }
    
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('STATION:EXPORT')")
    @Operation(summary = "Экспорт станций в CSV", 
               description = "Экспортирует список всех станций в формате CSV файла для скачивания")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV файл успешно сгенерирован")
    })
    public ResponseEntity<byte[]> exportStationsToCsv() {
        String csvData = stationService.exportStationsToCsv();
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("VMStationsList.csv")
                .build());

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('STATION:IMPORT')")
    @Operation(summary = "Импорт станций из CSV файла", 
               description = "Загружает CSV файл с данными станций и добавляет/обновляет их в БД")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Файл успешно обработан"),
        @ApiResponse(responseCode = "400", description = "Неверный формат файла"),
        @ApiResponse(responseCode = "500", description = "Ошибка при обработке файла")
    })
    public ResponseEntity<String> importStationsFromCsv(
            @Parameter(description = "CSV файл с данными станций", required = true)
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) { 
            return ResponseEntity.badRequest().body("Файл пустой");
        }
        
        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Неверный формат файла. Ожидается CSV");
        }
        
        try {
            String result = stationService.importStationsFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при импорте: " + e.getMessage());
        }
    }

    @GetMapping("/report")
    @PreAuthorize("hasAuthority('STATION:REPORT')")
    @Operation(summary = "Получить системный отчёт", 
               description = "Возвращает детальный системный отчёт, включающий статистику станций, пользователей и очереди")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Отчёт успешно сформирован")
    })
    public ResponseEntity<?> getSystemReport(
            @Parameter(description = "Формат отчёта: json или text", required = false, example = "json")
            @RequestParam(defaultValue = "json") String format) {
        
        if ("text".equalsIgnoreCase(format)) {
            // Возвращаем текстовый отчёт
            String textReport = reportService.generateTextReport();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("system_report_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".txt")
                    .build());
            
            return new ResponseEntity<>(textReport, headers, HttpStatus.OK);
        } else {
            // Возвращаем JSON отчёт
            ReportDTO report = reportService.generateSystemReport();
            return ResponseEntity.ok(report);
        }
    }

    @GetMapping("/report/html")
    @PreAuthorize("hasAuthority('STATION:REPORT')")
    @Operation(summary = "Получить системный отчёт в HTML формате", 
            description = "Возвращает детальный системный отчёт в формате HTML файла")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "HTML отчёт успешно сформирован"),
        @ApiResponse(responseCode = "500", description = "Ошибка при формировании отчёта")
    })
    public ResponseEntity<byte[]> getSystemReportHtml() {
        try {
            // Генерируем отчёт
            ReportDTO report = reportService.generateSystemReport();
            
            // Создаём HTML строку
            String htmlContent = generateHtmlReport(report);
            
            // Преобразуем HTML в байты
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            
            // Создаём имя файла с датой
            String fileName = String.format("vm_system_report_%s.html",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
            
            // Устанавливаем заголовки для скачивания файла
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(fileName)
                    .build());
            headers.setContentLength(htmlBytes.length);
            
            // Логируем создание отчёта
            //log.debug("HTML отчёт сгенерирован успешно. Размер: {} байт, имя файла: {}", 
            //        htmlBytes.length, fileName);
            
            return new ResponseEntity<>(htmlBytes, headers, HttpStatus.OK);
            
        }
        catch (Exception e) {
            //log.error("Ошибка при формировании HTML отчёта", e);
            
            // Возвращаем ошибку в формате HTML
            String errorHtml = generateErrorHtml(e.getMessage());
            byte[] errorBytes = errorHtml.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return new ResponseEntity<>(errorBytes, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Генерирует HTML страницу с ошибкой
     */
    private String generateErrorHtml(String errorMessage) {
        return String.format("""
            <!DOCTYPE html>
            <html lang='ru'>
            <head>
                <meta charset='UTF-8'>
                <title>Ошибка формирования отчёта</title>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 40px;
                        background-color: #f8f9fa;
                    }
                    .error-container {
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 30px;
                        background: white;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        border-left: 5px solid #dc3545;
                    }
                    .error-title {
                        color: #dc3545;
                        font-size: 24px;
                        margin-bottom: 20px;
                    }
                    .error-message {
                        background-color: #f8d7da;
                        color: #721c24;
                        padding: 15px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                        font-family: monospace;
                    }
                    .timestamp {
                        color: #6c757d;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-title">❌ Ошибка при формировании системного отчёта</div>
                    <div class="error-message">%s</div>
                    <div class="timestamp">Время ошибки: %s</div>
                </div>
            </body>
            </html>
            """, 
            escapeHtml(errorMessage),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
        );
    }

    /**
     * Экранирует HTML символы для безопасного отображения
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Создаёт HTML карточку для статистики
     */
    private String createStatCard(String label, String value) {
        return "<div class='stat-card'><div class='stat-value'>" + value + 
               "</div><div class='stat-label'>" + label + "</div></div>";
    }

    /**
     * Генерирует HTML отчёт из DTO
     * @param report DTO с данными отчёта
     * @return HTML строка
     */
    private String generateHtmlReport(ReportDTO report) {
    StringBuilder html = new StringBuilder();
    
    // Начало HTML документа
    html.append("<!DOCTYPE html>");
    html.append("<html lang='ru'>");
    html.append("<head>");
    html.append("<meta charset='UTF-8'>");
    html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
    html.append("<title>Системный отчёт виртуальных машин</title>");
    html.append("<style>");
    
    // Основные стили
    html.append("""
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f5f5f5;
            padding: 20px;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        }
        h1 {
            font-size: 2.5rem;
            margin-bottom: 10px;
        }
        .report-info {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 20px;
            background: rgba(255,255,255,0.1);
            padding: 15px;
            border-radius: 8px;
        }
        .section {
            background: white;
            padding: 25px;
            border-radius: 10px;
            margin-bottom: 25px;
            box-shadow: 0 2px 15px rgba(0,0,0,0.08);
            border-left: 4px solid #667eea;
        }
        h2 {
            color: #444;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
            font-size: 1.8rem;
        }
        .stat-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        .stat-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            transition: transform 0.3s ease;
            border-top: 4px solid #667eea;
        }
        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .stat-value {
            font-size: 2.2rem;
            font-weight: bold;
            color: #667eea;
            margin: 10px 0;
        }
        .stat-label {
            color: #666;
            font-size: 0.9rem;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }
        th {
            background-color: #f8f9fa;
            color: #444;
            font-weight: 600;
            text-align: left;
            padding: 15px;
            border-bottom: 2px solid #dee2e6;
        }
        td {
            padding: 12px 15px;
            border-bottom: 1px solid #eee;
        }
        tr:hover {
            background-color: #f8f9fa;
        }
        .status {
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 500;
        }
        .status-free { background: #d4edda; color: #155724; }
        .status-work { background: #cce5ff; color: #004085; }
        .status-on { background: #d1ecf1; color: #0c5460; }
        .status-off { background: #f8d7da; color: #721c24; }
        .status-repair { background: #fff3cd; color: #856404; }
        .status-disconnect { background: #e2e3e5; color: #383d41; }
        .badge {
            display: inline-block;
            padding: 3px 8px;
            font-size: 0.75rem;
            font-weight: 600;
            border-radius: 12px;
            margin-right: 5px;
        }
        .badge-active { background: #28a745; color: white; }
        .badge-inactive { background: #dc3545; color: white; }
        .footer {
            text-align: center;
            margin-top: 40px;
            padding: 20px;
            color: #666;
            font-size: 0.9rem;
            border-top: 1px solid #eee;
        }
        .print-button {
            background: #28a745;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 1rem;
            transition: background 0.3s;
        }
        .print-button:hover {
            background: #218838;
        }
        @media print {
            .header { background: white !important; color: black; }
            .print-button { display: none; }
            .stat-card { break-inside: avoid; }
            body { padding: 0; }
        }
        """);
    
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    
    // Контейнер
    html.append("<div class='container'>");
    
    // Заголовок
    html.append("<div class='header'>");
    html.append("<h1>📊 Системный отчёт виртуальных машин</h1>");
    html.append("<p>Детальный отчёт о состоянии системы виртуальных машин</p>");
    html.append("<div class='report-info'>");
    html.append("<div>");
    html.append("<strong>Дата формирования:</strong> ");
    html.append(report.getReportDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
    html.append("</div>");
    html.append("<button class='print-button' onclick='window.print()'>🖨️ Печать отчёта</button>");
    html.append("</div>");
    html.append("</div>");
    
    // Статистика станций
    html.append("<div class='section'>");
    html.append("<h2>📈 Статистика станций</h2>");
    html.append("<div class='stat-grid'>");
    
    ReportDTO.StationStatistics stationStats = report.getStationStatistics();
    html.append(createStatCardHtml("Всего станций", stationStats.getTotalStations(), "🏢"));
    html.append(createStatCardHtml("Свободных", stationStats.getFreeStations(), "✅"));
    html.append(createStatCardHtml("В работе", stationStats.getWorkStations(), "💼"));
    html.append(createStatCardHtml("Включенных", stationStats.getOnStations(), "🔌"));
    html.append(createStatCardHtml("Выключенных", stationStats.getOffStations(), "🔋"));
    html.append(createStatCardHtml("В ремонте", stationStats.getRepairStations(), "🔧"));
    html.append(createStatCardHtml("Отключенных", stationStats.getDisconnectStations(), "📴"));
    
    html.append("</div>");
    html.append("</div>");
    
    // Статистика пользователей
    html.append("<div class='section'>");
    html.append("<h2>👥 Статистика пользователей</h2>");
    html.append("<div class='stat-grid'>");
    
    ReportDTO.UserStatistics userStats = report.getUserStatistics();
    html.append(createStatCardHtml("Всего пользователей", userStats.getTotalUsers(), "👤"));
    html.append(createStatCardHtml("Администраторов", userStats.getAdminCount(), "👑"));
    html.append(createStatCardHtml("Обычных пользователей", userStats.getUserCount(), "👨‍💻"));
    
    // Процент администраторов
    if (userStats.getTotalUsers() > 0) {
        double adminPercentage = (double) userStats.getAdminCount() / userStats.getTotalUsers() * 100;
        html.append(createStatCardHtml("Администраторов", 
                String.format("%.1f%%", adminPercentage), "📊"));
    }
    
    html.append("</div>");
    html.append("</div>");
    
    // Детали по станциям
    html.append("<div class='section'>");
    html.append("<h2>🖥️ Детальная информация по станциям</h2>");
    html.append("<table>");
    html.append("<thead>");
    html.append("<tr>");
    html.append("<th>ID</th>");
    html.append("<th>IP адрес</th>");
    html.append("<th>Порт</th>");
    html.append("<th>Состояние</th>");
    html.append("<th>Логин</th>");
    html.append("<th>Текущий пользователь</th>");
    html.append("<th>Назначена</th>");
    html.append("</tr>");
    html.append("</thead>");
    html.append("<tbody>");
    
    for (ReportDTO.StationDetail station : report.getStationDetails()) {
        String statusClass = getStatusClass(station.getState());
        
        html.append("<tr>");
        html.append("<td><strong>").append(station.getId()).append("</strong></td>");
        html.append("<td><code>").append(station.getIp()).append("</code></td>");
        html.append("<td>").append(station.getPort()).append("</td>");
        html.append("<td><span class='status ").append(statusClass).append("'>")
            .append(station.getState()).append("</span></td>");
        html.append("<td>").append(station.getLogin()).append("</td>");
        html.append("<td>").append(station.getCurrentUser() != null ? 
                "<strong>" + station.getCurrentUser() + "</strong>" : "-").append("</td>");
        html.append("<td>").append(station.getAssignedAt() != null ? 
                station.getAssignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : 
                "-").append("</td>");
        html.append("</tr>");
    }
    
    html.append("</tbody>");
    html.append("</table>");
    html.append("</div>");
    
    // Последние записи в очереди
    html.append("<div class='section'>");
    html.append("<h2>📋 Последние записи в очереди</h2>");
    
    if (report.getLastQueueRecords().isEmpty()) {
        html.append("<p style='text-align: center; color: #666; padding: 20px;'>Нет записей в очереди</p>");
        } else {
            html.append("<table>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th>ID</th>");
            html.append("<th>Пользователь</th>");
            html.append("<th>Станция</th>");
            html.append("<th>Создано</th>");
            html.append("<th>Освобождено</th>");
            html.append("<th>Статус</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");
            
            for (ReportDTO.QueueRecord queue : report.getLastQueueRecords()) {
                html.append("<tr>");
                html.append("<td>").append(queue.getId()).append("</td>");
                html.append("<td><strong>").append(queue.getUsername() != null ? queue.getUsername() : "-").append("</strong></td>");
                html.append("<td><code>").append(queue.getStationIp() != null ? queue.getStationIp() : "-").append("</code></td>");
                html.append("<td>").append(queue.getCreatedAt() != null ? 
                        queue.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : 
                        "-").append("</td>");
                html.append("<td>").append(queue.getReleasedAt() != null ? 
                        queue.getReleasedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : 
                        "-").append("</td>");
                html.append("<td>");
                if (queue.getActive()) {
                    html.append("<span class='badge badge-active'>Активна</span>");
                } else {
                    html.append("<span class='badge badge-inactive'>Завершена</span>");
                }
                html.append("</td>");
                html.append("</tr>");
            }
            
            html.append("</tbody>");
            html.append("</table>");
        }
        html.append("</div>");
        
        // Подвал
        html.append("<div class='footer'>");
        html.append("<p>Отчёт сгенерирован автоматически системой управления виртуальными машинами</p>");
        html.append("<p>© ").append(LocalDateTime.now().getYear()).append(" VM Management System</p>");
        html.append("</div>");
        
        html.append("</div>"); // закрываем container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Создаёт HTML карточку для статистики
     */
    private String createStatCardHtml(String label, Object value, String icon) {
        return String.format("""
            <div class='stat-card'>
                <div style='font-size: 1.5rem; margin-bottom: 10px;'>%s</div>
                <div class='stat-value'>%s</div>
                <div class='stat-label'>%s</div>
            </div>
            """, icon, value, label);
    }

    /**
     * Возвращает CSS класс для статуса станции
     */
    private String getStatusClass(VMState state) {
        switch (state) {
            case FREE: return "status-free";
            case WORK: return "status-work";
            case ON: return "status-on";
            case OFF: return "status-off";
            case REPAIR: return "status-repair";
            case DISCONNECT: return "status-disconnect";
            default: return "";
        }
    }
}