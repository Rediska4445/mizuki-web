package rf.mizuka;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

/**
 * Тест класса {@link Application}, отвечающего за запуск Spring Boot приложения.
 *
 * <p>Основная цель этого теста — убедиться, что метод {@link Application#main(String[])}
 * корректно запускает Spring Boot контекст и не выбрасывает исключений при старте.
 * как если бы приложение запускалось обычным образом через main‑метод.
 */
class ApplicationTest {
    /**
     * Проверяет, что метод {@link Application#main(String[])} успешно стартует
     * приложение и не падает с исключением.
     *
     * <p>Тест запускает приложение с пустым массивом аргументов командной строки.
     * Если при запуске возникнет ошибка (например, неправильные конфигурации, бины с
     * некорректной инициализацией и т.п.), этот тест упадёт.
     */
    @Test
    void mainMethodStartsApplicationWithoutException() {
        Application.main(new String[] {});
    }
}