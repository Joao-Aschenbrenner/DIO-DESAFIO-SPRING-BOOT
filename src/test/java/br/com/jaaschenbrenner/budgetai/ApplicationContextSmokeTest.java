package br.com.jaaschenbrenner.budgetai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:budgetai-smoke;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.model.moderation=none",
        "spring.ai.openai.chat.api-key=test-nvidia-key",
        "spring.ai.openai.chat.base-url=http://localhost:9999"
})
class ApplicationContextSmokeTest {

    @Test
    void contextLoadsWithoutOpenAiModerationKey() {
        // O teste passa somente se todo o ApplicationContext conseguir inicializar.
    }
}
