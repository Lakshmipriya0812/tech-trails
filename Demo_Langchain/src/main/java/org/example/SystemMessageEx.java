package org.example;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SystemMessageEx {

    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey("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")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
        Scanner scanner = new Scanner(System.in);
        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(SystemMessage.from("You are a helpful assistant that answers questions clearly and concisely."));
        conversation.add(UserMessage.from("What is solar energy?"));
        conversation.add(AiMessage.from("Solar energy is the energy that comes from the sun, which can be converted into electricity or heat."));

        conversation.add(UserMessage.from("How do solar panels work?"));
        conversation.add(AiMessage.from("Solar panels convert sunlight into electricity using photovoltaic cells that generate an electric current."));

        System.out.println("You can ask questions. Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("Goodbye!");
                break;
            }
            conversation.add(UserMessage.from(userInput));
            AiMessage aiResponse = model.chat(conversation).aiMessage();

            System.out.println("AI: " + aiResponse.text());
            conversation.add(aiResponse);
        }
        scanner.close();
    }
}
