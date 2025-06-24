package org.example;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder().apiKey("sk-proj-6cPvHOo9XCUiybYCCewDCkss2hDS3sGhdsiEhZ" +
                        "VzVCHtMSpnrnTrXA6MP0joTPMh8LpyzZS0yCT3BlbkFJftOVk7IR7bppT6zkVmymRB" +
                        "HA9i8b9K8oVD51scjRTj2GGy6WZhvtPNVHTV7Tb6dRH9qbyRu8oA")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI).build();

        String message = model.chat("Say hello World");
        System.out.println(message);
        List<ChatMessage> conversation = new ArrayList<>();

        // Scanner for user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("AI is ready! Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            // Add user message to conversation
            UserMessage userMessage = UserMessage.from(userInput);
            conversation.add(userMessage);

            // Get AI response
            AiMessage aiMessage = model.chat(conversation).aiMessage();
            System.out.println("AI: " + aiMessage.text());

            // Add AI response to conversation
            conversation.add(aiMessage);
        }

        scanner.close();
    }
}
