package org.example;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class DocumentChatEx {
    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey("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")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
        String documentText;
        try {
            documentText = Files.readString(Path.of("src/main/resources/document.txt"));
        } catch (IOException e) {
            System.err.println("Failed to read document: " + e.getMessage());
            return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("You can now ask questions about the document. Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String question = scanner.nextLine();
            if (question.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            String prompt = documentText + "\n\n" + "Question: " + question;
            String response = model.chat(prompt);
            System.out.println("AI: " + response);
        }

        scanner.close();
    }
}
