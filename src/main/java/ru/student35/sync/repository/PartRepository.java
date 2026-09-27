package ru.student35.sync.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.student35.sync.model.Part;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для доступа к коллекции деталей в БД.
 *
 * <p>Предоставляет CRUD-функционал от наследования {@link MongoRepository},
 * а также специализированные методы для поиска, обновления и выгрузки
 * исторических данных.</p>
 */
@Repository
public interface PartRepository extends MongoRepository<Part, String> {
    /**
     * Выполняет поиск детали по её коду из CMS.
     *
     * @param spareCode уникальный код детали из CMS (например, "SP12345")
     * @return {@link Optional}, содержащий найденную деталь, или пустой Optional, если деталь новая
     */
    Optional<Part> findBySpareCode(String spareCode);
    /**
     * Возвращает список всех активных деталей, которые в данный момент присутствуют в обороте.
     *
     * <p>Метод фильтрует записи по флагу мягкого удаления (isDeleted = false).
     * Исключает из выборки исторические данные о деталях, которые уже вышли из оборота.</p>
     *
     * @return список активных деталей
     */
    List<Part> findByIsDeletedFalse();
    /**
     * Извлекает абсолютно все записи о деталях из БД,
     * сортируя их по уникальному коду в алфавитном порядке.
     *
     * <p>Исторические записи для CSV, который отправляется в Report API.</p>
     *
     * @return отсортированный по возрастанию кодов список всех исторических записей
     */
    List<Part> findAllByOrderBySpareCodeAsc();
}