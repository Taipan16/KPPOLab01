package com.example.vmserver.controller;

import com.example.vmserver.model.VMStation;
import com.example.vmserver.service.VMStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

import java.util.List;


@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Контроллер станций", description = "API для управления виртуальными машинами")
public class VMStationController {
    private final VMStationService stationService;

    @PostMapping
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
    @Operation(summary = "Получение списка всех станций", 
               description = "Возвращает список всех виртуальных машин-станций")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список станций успешно получен")
    })
    public ResponseEntity<List<VMStation>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{id}")
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
}