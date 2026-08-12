package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, FinanceTools financeTools) {
        return builder
                .defaultSystem("""
                        Você é um assistente financeiro em português brasileiro.
                        Interprete comandos de gastos e consultas financeiras.
                        Sempre use as tools disponíveis quando a solicitação exigir criar ou consultar transações reais.
                        Nunca invente uma transação que não tenha sido retornada pelas tools.
                        """)
                .defaultTools(financeTools)
                .build();
    }
}
