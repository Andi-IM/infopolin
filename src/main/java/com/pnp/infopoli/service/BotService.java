package com.pnp.infopoli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.Multicast;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class BotService {
    @Autowired
    private LineMessagingClient lineMessagingClient;

    public void push(PushMessage pushMessage) {
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void multicast(Set<String> to, Message message){
        try {
            Multicast multicast = new Multicast(to, message);
            lineMessagingClient.multicast(multicast).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void reply(ReplyMessage replyMessage) {
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void reply(String replyToken, Message message) {
        ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
        reply(replyMessage);
    }

    public void reply(String replyToken, List<Message> message) {
        ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
        reply(replyMessage);
    }

    public void replyText(String replyToken, String messageText) {
        TextMessage textMessage = new TextMessage(messageText);
        reply(replyToken, textMessage);
    }

    public void replyText(String replyToken, String[] messageTexts) {
        List<Message> textMessages = Arrays
                .stream(messageTexts)
                .map(TextMessage::new)
                .collect(Collectors.toList());
        reply(replyToken, textMessages);
    }

    public void replySticker(String replyToken, String packageId, String stickerId) {
        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, stickerMessage);
        reply(replyMessage);
    }

    public void replyFlexMessage(String replyToken){
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            String flexTemplate = IOUtils.toString(classLoader.getResourceAsStream("menus.json"));

            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            FlexContainer flexContainer = objectMapper.readValue(flexTemplate, FlexContainer.class);

            ReplyMessage replyMessage = new ReplyMessage(replyToken, new FlexMessage("dicoding", flexContainer));
            reply(replyMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UserProfileResponse getProfile(String userId) {
        try {
            return lineMessagingClient.getProfile(userId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void leaveGroup(String groupId) {
        try {
            lineMessagingClient.leaveGroup(groupId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void leaveRoom(String groupId) {
        try {
            lineMessagingClient.leaveGroup(groupId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
