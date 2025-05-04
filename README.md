# Сервис обработки событий (em-processor)

Микросервис `em-processor` отвечает за получение объектов событий (`Event`) и упоминаний (`Mention`) из Kafka, временное хранение и группировку их по идентификатору батча (`batchId`) в Redis, выполнение предварительной обработки и последующую отправку обработанных данных в соответствующие топики Kafka для доставки в Elasticsearch.

## Основной рабочий процесс

1.  **Получение данных из Kafka:**
    *   Сервис слушает два топика Kafka (`adapter-event` и `adapter-mention`), содержащие JSON-представления объектов `Event` и `Mention`.
    *   Из заголовка каждого сообщения извлекается идентификатор `batchId`, который группирует пары файлов событий и упоминаний (например, `20250323151500`).

2.  **Регистрация батча и временное хранение в Redis:**
    *   При получении первого сообщения для нового `batchId`, сервис (`BatchStateService`) регистрирует его в Redis и запускает "временное окно" (настраивается, по умолчанию 60 секунд).
    *   Полученные объекты `Event` и `Mention` сериализуются в JSON и сохраняются в Redis (`EventProcessingService`) с ключами, включающими `batchId`. Устанавливается TTL (время жизни) для этих ключей, немного превышающее временное окно, для автоматической очистки в случае сбоев.
    *   Идентификаторы (`GlobalEventId` для событий, `GlobalEventId_MentionIdentifier` для упоминаний) также сохраняются в отдельные множества Redis для каждого `batchId`.

3.  **Контроль временного окна:**
    *   Планировщик (`BatchProcessingScheduler`) периодически проверяет (`BatchStateService.checkExpiredBatchWindows()`) активные батчи в Redis.
    *   Если временное окно для `batchId` истекло, он помечается как "готовый к обработке" (перемещается из множества активных в множество готовых батчей).

4.  **Обработка готовых батчей:**
    *   Планировщик периодически запрашивает (`BatchStateService.getNextReadyBatch()`) готовый `batchId` из Redis.
    *   Если найден готовый батч:
        *   `EventProcessingService.processBatch()` извлекает все связанные с `batchId` события и упоминания из Redis.
        *   Данные передаются в `BatchProcessor`, который выполняет предварительную обработку (на данный момент базовая фильтрация, но предназначен для будущей логики валидации, обогащения, анализа тональности и т.д.).
        *   Возвращается объект `BatchData` с обработанными списками событий и упоминаний.

5.  **Отправка обработанных данных в Kafka:**
    *   `KafkaMessagePublisher` отправляет обработанные объекты в исходящие топики Kafka:
        *   События (`Event`) => `processor-event` (ключ: `GlobalEventId`)
        *   Упоминания (`Mention`) => `processor-mention` (ключ: `GlobalEventId_MentionIdentifier`)
    *   Отправка выполняется асинхронно.

6.  **Очистка состояния в Redis:**
    *   После **успешной** отправки *всех* событий и упоминаний для данного `batchId` в Kafka, вызывается `RedisBatchCleaner.cleanupBatch()`.
    *   Этот компонент полностью удаляет все данные (события, упоминания) и метаданные состояния (время старта, идентификаторы в множествах активных/готовых) для обработанного `batchId` из Redis.

## Обработка ошибок

*   **Ошибки Kafka (Consumer):** Используется стандартный `DefaultErrorHandler` для повторных попыток при временных сбоях.
*   **Ошибки Redis:** Логируются. Проблемы с Redis могут привести к потере данных батча или некорректной обработке окна. TTL на ключах служит механизмом подстраховки для очистки.
*   **Ошибки сериализации/десериализации:** Логируются. Некорректные данные могут быть пропущены.
*   **Ошибки обработки (`BatchProcessor`):** Логируются. В текущей реализации при ошибке обработки возвращаются исходные данные батча.
*   **Ошибки Kafka (Producer):** Логируются. Если отправка хотя бы одного сообщения завершилась ошибкой, очистка Redis для этого `batchId` **не производится**, что позволяет повторно обработать батч при следующем запуске (если данные еще не удалены по TTL).

## Расширяемость

*   **Логика обработки:** Компонент `BatchProcessor` является основной точкой расширения для добавления сложной бизнес-логики обработки данных перед отправкой в Elasticsearch (фильтрация, валидация, анализ тональности, агрегация и т.д.).

## Диаграмма последовательности (клик на кнопку ⟷ развернет схему)

```mermaid
sequenceDiagram
    participant InKafka as Входящие топики Kafka
    participant Listener as KafkaMessageListener
    participant BatchStateSvc as BatchStateService
    participant EventProcSvc as EventProcessingService
    participant Redis as Redis
    participant Scheduler as BatchProcessingScheduler
    participant BatchProcessor as BatchProcessor
    participant Publisher as KafkaMessagePublisher
    participant Cleaner as RedisBatchCleaner
    participant OutKafka as Исходящие топики Kafka

    %% Получение и сохранение данных
    InKafka->>Listener: Сообщение Event/Mention + Заголовок BatchId
    Listener->>BatchStateSvc: registerBatch(batchId)
    opt Новый батч
        BatchStateSvc->>Redis: Сохраняем время старта batch:start:<batchId> (с TTL)
        BatchStateSvc->>Redis: Добавляем в активные: active:batches <batchId>
    end
    Listener->>EventProcSvc: storeEvent/storeMention(batchId, data)
    EventProcSvc->>Redis: Сохраняем данные: data:<type>:<batchId>:<id> (JSON, с TTL)
    EventProcSvc->>Redis: Добавляем ID в множество: batch:<type>s:<batchId> <id> (с TTL)

    %% Проверка и обработка окна
    loop Периодически (напр., каждые 5 сек)
        Scheduler->>BatchStateSvc: checkExpiredBatchWindows()
        BatchStateSvc->>Redis: Получаем ID активных батчей: active:batches
        BatchStateSvc->>Redis: Получаем время старта batch:start:<batchId> для каждого активного
        opt Окно истекло для batchId
            BatchStateSvc->>Redis: Добавляем в готовые: ready:batches <batchId>
            BatchStateSvc->>Redis: Удаляем из активных: active:batches <batchId>
        end
    end

    %% Обработка готового батча
    loop Периодически (напр., каждые 3 сек)
        Scheduler->>BatchStateSvc: getNextReadyBatch()
        BatchStateSvc->>Redis: Извлекаем ID из готовых: ready:batches
        BatchStateSvc-->>Scheduler: batchId (или null)

        opt batchId получен
            Scheduler->>EventProcSvc: processBatch(batchId)
            EventProcSvc->>Redis: Получаем ID событий из множества: batch:events:<batchId>
            EventProcSvc->>Redis: Получаем ID упоминаний из множества: batch:mentions:<batchId>
            EventProcSvc->>Redis: Получаем данные событий по ID: data:event:<batchId>:<eventId>...
            EventProcSvc->>Redis: Получаем данные упоминаний по ID: data:mention:<batchId>:<mentionId>...
            EventProcSvc-->>EventProcSvc: Десериализация Event/Mention
            EventProcSvc->>BatchProcessor: process(events, mentions)
            BatchProcessor-->>EventProcSvc: Обработанные BatchData
            EventProcSvc-->>Scheduler: Обработанные BatchData

            %% Асинхронная отправка
            par Отправка Событий
                Scheduler->>Publisher: sendEvent(event, key) для каждого события
                Publisher->>OutKafka: Event JSON в processor-event
                OutKafka-->>Publisher: Подтверждение/Ошибка
            and Отправка Упоминаний
                Scheduler->>Publisher: sendMention(mention, key) для каждого упоминания
                Publisher->>OutKafka: Mention JSON в processor-mention
                OutKafka-->>Publisher: Подтверждение/Ошибка
            end

            %% Очистка после УСПЕШНОЙ отправки ВСЕХ сообщений
            alt Все отправки в Kafka успешны
                Scheduler->>Cleaner: cleanupBatch(batchId)
                Cleaner->>Redis: Удаляем множество ID событий: batch:events:<batchId>
                Cleaner->>Redis: Удаляем данные событий: data:event:<batchId>:*
                Cleaner->>Redis: Удаляем множество ID упоминаний: batch:mentions:<batchId>
                Cleaner->>Redis: Удаляем данные упоминаний: data:mention:<batchId>:*
                Cleaner->>Redis: Удаляем время старта: batch:start:<batchId>
                Cleaner->>Redis: Удаляем из активных: active:batches <batchId> (на всякий случай)
                Cleaner->>Redis: Удаляем из готовых: ready:batches <batchId> (уже извлечен, но для надежности)
            else Ошибка при отправке в Kafka
                Scheduler->>Scheduler: Логируем ошибку, НЕ очищаем Redis
            end
        end
    end
```