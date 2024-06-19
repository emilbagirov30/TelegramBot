package com.emil.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlavaBot extends TelegramLongPollingBot {

    private final Map<Long, String> userStages = new HashMap<>();
    private final Map<Long, ClientData> clientsData = new HashMap<>();
  private final long adminChatId = 623349419L;
  private final long developerChatId = 1638819245L;
    private static final long BLOCK_DURATION = 30 * 60 * 1000;
    private static final int START_LIMIT = 3;
    private static final long TIME_LIMIT = 5 * 60 * 1000;
    private Map<Long, Integer> startCounts = new ConcurrentHashMap<>();
    private Map<Long, Long> lastStartTimes = new ConcurrentHashMap<>();
    private Map<Long, Long> blockedUsers = new ConcurrentHashMap<>();
    private String username;

    private boolean isValidPlatform = false;
    private boolean isValidPlatformGeneral;
    private String userLink;
    private int maxChars = 3200;
    private int minPrice = 1500;
    private int maxPrice = 100000;
    private String manualSum = "Введите сумму, используя только цифры (От " + minPrice + " до " + maxPrice + " рублей).";
    private String telegram = "Telegram";
    private String instagram = "Instagram (принадлежит Meta, экстремисткой организации, запрещённой на территории РФ)";
    private String  youtube = "YouTube";
    private String tiktok = "TikTok";

    @Override
    public String getBotUsername() {
        return BotData.Companion.getUsername();
    }

    @Override
    public String getBotToken() {
        return BotData.Companion.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String userResponse = update.getMessage().getText();
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            System.out.println("ChatId: " + chatId);
            User user = update.getMessage().getFrom();
            username = user.getFirstName();
            userLink =  user.getUserName();
            if (blockedUsers.containsKey(chatId)) {
                long blockedUntil = blockedUsers.get(chatId);
                if (System.currentTimeMillis() < blockedUntil) {
                    spamPrevention(message);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                } else {
                    blockedUsers.remove(chatId);
                }
            }

            if (userResponse.equals("/start")) {
                long currentTime = System.currentTimeMillis();
                int count = startCounts.getOrDefault(chatId, 0);
                long lastStartTime = lastStartTimes.getOrDefault(chatId, 0L);
                isValidPlatformGeneral = false;
                if (count >= START_LIMIT && (currentTime - lastStartTime) < TIME_LIMIT) {
                    blockedUsers.put(chatId, currentTime + BLOCK_DURATION);
                    spamPrevention(message);
                } else {
                    if ((currentTime - lastStartTime) >= TIME_LIMIT) {
                        count = 0;
                    }
                }

                startCounts.put(chatId, count + 1);
                lastStartTimes.put(chatId, currentTime);
                userStages.put(chatId, "initial");
                clientsData.put(chatId, new ClientData(username));
                message.setText("Здравствуйте, " + username + "! Я бот по предложениям рекламы на канале slava.zxz. Что вы хотите прорекламировать?");
                setInitialKeyboard(message);
            } else {
                handleUserResponse(chatId, userResponse, message);
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUserResponse(long chatId, String userResponse, SendMessage message) {
        String currentStage = userStages.getOrDefault(chatId, "initial");
        ClientData clientData = clientsData.get(chatId);

        resetKeyboard(message);

        switch (currentStage) {
            case "initial":
                if (userResponse.equals("Канал")) {
                    userStages.put(chatId, "blogger");
                    clientData.setAdvertisementType("Канал");
                    message.setText("Какой у вас контент? Опишите его.");
                } else if (userResponse.equals("Товар")) {
                    userStages.put(chatId, "product");
                    clientData.setAdvertisementType("Товар");
                    qualityControl(message);
                } else if (userResponse.equals("Площадка по продаже")) {
                    userStages.put(chatId, "platform");
                    clientData.setAdvertisementType("Площадка по продаже");
                    qualityControl(message);
                } else {
                    message.setText("Пожалуйста, выберите один из предложенных вариантов");
                    setInitialKeyboard(message);
                }
                break;
            case "blogger":
                if (userResponse.length() < maxChars) {
                    if (isValidDescription(userResponse)) {
                        clientData.setContentDescription(userResponse);
                        choosingAdAdvertisingPlatform(message, chatId,clientData);
                    } else {
                        minOfWords(message);
                    }
                } else {
                    maxOfWords(message);
                }
                break;
            case "product":
            case "platform":
                if (userResponse.length() < maxChars) {
                    if (isValidDescription(userResponse)) {
                        clientData.setProductDescription(userResponse);
                        choosingAdAdvertisingPlatform(message, chatId,clientData);
                    } else {
                        minOfWords(message);
                    }
                } else {
                    maxOfWords(message);
                }
                break;
            case "final":
                if (userResponse.equals("Везде")){
                    userStages.put(chatId, "budget");
                    clientData.addPlatform(userResponse);
                    message.setText("Какой у вас бюджет? " + manualSum);
                }else if (userResponse.equals("Готово")) {
                    if (clientData.getPlatforms().isEmpty()) {
                        message.setText("Пожалуйста, выберите хотя бы одну платформу.");
                        setPlatformsKeyboard(message,clientData);
                    } else {
                        userStages.put(chatId, "budget");
                        message.setText("Какой у вас бюджет? " + manualSum);
                    }
                } else {
                    isValidPlatform=false;
                    String[] allowedPlatforms = {instagram, telegram, tiktok, youtube};

                    for (String platform : allowedPlatforms) {
                        if (userResponse.equalsIgnoreCase(platform)) {
                            isValidPlatform = true;
                            isValidPlatformGeneral = true;
                            break;
                        }
                    }
                    if (isValidPlatform) {
                        clientData.addPlatform(userResponse);
                        message.setText("Выберите другие платформы или нажмите 'Готово', если завершили выбор.");
                        setPlatformsKeyboard(message,clientData);
                    } else {
                        message.setText("Пожалуйста, выберите из предложенных вариантов.");
                        setPlatformsKeyboard(message,clientData);
                    }
                }
                break;
            case "budget":
                if (userResponse.matches("\\d+")) {
                    int budget = Integer.parseInt(userResponse.replaceAll(" ", ""));
                    if (budget >= minPrice && budget <= maxPrice) {
                        clientData.setBudget(String.valueOf(budget));
                        userStages.put(chatId, "completed");
                        message.setText("Отправьте ссылки на свои ресурсы и контакт для связи (Ссылка на аккаунт в Telegram будет отправлена автоматически, если она присутствует).");
                    } else {
                        message.setText("Введите сумму в диапазоне от " + minPrice + " до " + maxPrice + " рублей.");
                    }
                } else {
                    message.setText(manualSum);
                }
                break;
            case "completed":
                if (userResponse.length() < 1500) {
                    if (userResponse.length() < 4) {
                        message.setText("Слишком короткий ответ. Попробуйте еще раз.");
                    } else {
                        clientData.setContactInfo(userResponse);
                        message.setText("Спасибо! Мы свяжемся с вами.");
                        saveUserResponse(chatId, userResponse);
                        sendReport(clientData);
                        setReturnKeyboard(message);
                    }
                } else {
                    maxOfWords(message);
                }
                break;
            default:
                message.setText("Произошла ошибка.");
                break;
        }
    }


    private boolean isValidDescription(String description) {
        String[] words = description.split("\\s+");
        for (String word : words) {
            if (word.length() >= 3 && word.matches("\\p{L}{3,}")) {
                return true;
            }
        }
        return false;
    }


    private void setInitialKeyboard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Канал"));
        row.add(new KeyboardButton("Товар"));
        row.add(new KeyboardButton("Площадка по продаже"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void setPlatformsKeyboard(SendMessage message, ClientData clientData) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow rowInstagram = new KeyboardRow();
        KeyboardRow rowTTY = new KeyboardRow();
        KeyboardRow rowEverywhere = new KeyboardRow();
        KeyboardRow rowPrepared = new KeyboardRow();

        if (!clientData.getPlatforms().contains(instagram)) {
            rowInstagram.add(new KeyboardButton(instagram));
        }
        if (!clientData.getPlatforms().contains(telegram)) {
            rowTTY.add(new KeyboardButton(telegram));
        }
        if (!clientData.getPlatforms().contains(tiktok)) {
            rowTTY.add(new KeyboardButton(tiktok));
        }
        if (!clientData.getPlatforms().contains(youtube)) {
            rowTTY.add(new KeyboardButton(youtube));
        }

        rowPrepared.add(new KeyboardButton("Готово"));

        if (!isValidPlatformGeneral) {
            rowEverywhere.add(new KeyboardButton("Везде"));
        }

        keyboard.add(rowInstagram);
        keyboard.add(rowTTY);
        keyboard.add(rowEverywhere);
        keyboard.add(rowPrepared);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void qualityControl(SendMessage message) {
        message.setText("Как можно убедиться, что ваши продукты качественные и не являются опасными или вредными для здоровья?");
    }

    private void spamPrevention(SendMessage message) {
        message.setText("Вы слишком часто отправляли сообщение, Ваш id заблокирован");
    }

    private void minOfWords(SendMessage message) {
        message.setText("Введите хотя бы одно слово.");
    }

    private void maxOfWords(SendMessage message) {
        message.setText("Вы ввели слишком длинный ответ");
    }

    private void choosingAdAdvertisingPlatform(SendMessage message, long chatId,ClientData clientData) {
        userStages.put(chatId, "final");
        message.setText("Где хотите рекламировать?");
        setPlatformsKeyboard(message,clientData);
    }

    private void setReturnKeyboard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("/start"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }


    private void resetKeyboard(SendMessage message) {
        message.setReplyMarkup(new ReplyKeyboardRemove(true));
    }

    private void saveUserResponse(long chatId, String userResponse) {
        System.out.println("User response from chat " + chatId + ": " + userResponse);
    }

    private void sendReport(ClientData clientData) {
        String report = "Ник: " + clientData.getName() + "\n" +
                "Что хочет рекламировать: " + clientData.getAdvertisementType() + "\n" +
                "Описание: " + (clientData.getContentDescription() != null ? clientData.getContentDescription() : clientData.getProductDescription()) + "\n" +
                "Платформы для рекламы: " + String.join(", ", clientData.getPlatforms()) + "\n" +
                "Бюджет: " + clientData.getBudget()+ "р." + "\n" +
                "Контакт для связи и ссылки: " + clientData.getContactInfo();

        if (userLink!=null){
            report += "\n" + "Ссылка на Телеграм: @" + userLink + " .";
        }

        SendMessage messageAdmin = new SendMessage();
        messageAdmin.setChatId(adminChatId);
        messageAdmin.setText(report);
        SendMessage messageDeveloper = new SendMessage();
        messageDeveloper.setChatId(developerChatId);
        messageDeveloper.setText(report);

        try {
            execute(messageAdmin);
            execute(messageDeveloper);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static class ClientData {
        private final String name;
        private String advertisementType;
        private String contentDescription;
        private String productDescription;
        private List<String> platforms = new ArrayList<>();
        private String contactInfo;
        private String budget;

        public ClientData(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getAdvertisementType() {
            return advertisementType;
        }

        public void setAdvertisementType(String advertisementType) {
            this.advertisementType = advertisementType;
        }

        public String getContentDescription() {
            return contentDescription;
        }

        public void setContentDescription(String contentDescription) {
            this.contentDescription = contentDescription;
        }

        public String getProductDescription() {
            return productDescription;
        }

        public void setProductDescription(String productDescription) {
            this.productDescription = productDescription;
        }

        public List<String> getPlatforms() {
            return platforms;
        }

        public void addPlatform(String platform) {
            this.platforms.add(platform);
        }

        public String getContactInfo() {
            return contactInfo;
        }

        public void setContactInfo(String contactInfo) {
            this.contactInfo = contactInfo;
        }

        public String getBudget() {
            return budget;
        }

        public void setBudget(String budget) {
            this.budget = budget;
        }
    }
}