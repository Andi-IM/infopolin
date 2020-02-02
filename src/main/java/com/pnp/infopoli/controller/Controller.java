package com.pnp.infopoli.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.MessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.event.source.UserSource;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.pnp.infopoli.model.EventsModel;
import com.pnp.infopoli.service.BotFlexContainer;
import com.pnp.infopoli.service.BotService;
import com.pnp.infopoli.service.BotTemplate;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
public class Controller {
    @Autowired
    @Qualifier("lineMessagingClient")
    private LineMessagingClient lineMessagingClient;

    @Autowired
    @Qualifier("lineSignatureValidator")
    private LineSignatureValidator lineSignatureValidator;

    @Autowired
    private BotService botService;

    @Autowired
    private BotTemplate botTemplate;

    @Autowired
    private BotFlexContainer botContainer;

    private UserProfileResponse sender = null;

    @RequestMapping(value = "/api/callback", method = RequestMethod.POST)
    public ResponseEntity<String> callback(
            @RequestHeader("X-Line-Signature") String xLineSignature,
            @RequestBody String eventsPayload
    ){
        try {
            //if (!lineSignatureValidator.validateSignature(eventsPayload.getBytes(), xLineSignature)){
            //    throw new RuntimeException("Invalid Signature Validation");
            //}

            //parsing events
            System.out.println(eventsPayload);
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventsModel eventsModel = objectMapper.readValue(eventsPayload, EventsModel.class);

            //Code utama
            eventsModel.getEvents().forEach((event) -> {
                //isi kode disini
                if (event instanceof JoinEvent || event instanceof FollowEvent) {
                    String replyToken = ((ReplyEvent) event).getReplyToken();
                    handleJointOrFollowEvent(replyToken, event.getSource());
                } else if (event instanceof MessageEvent) {
                    handleMessageEvent((MessageEvent) event);
                }
            });
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (IOException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void greetingMessage(String replyToken, Source source, String  additionalMessage) {
        if (sender == null) {
            String senderId = source.getSenderId();
            sender          = botService.getProfile(senderId);
        }

        TemplateMessage greetingMessage = botTemplate.greetingMessage(source, sender);

        if (additionalMessage != null) {
            List<Message> messages = new ArrayList<>();
            messages.add(new TextMessage(additionalMessage));
            messages.add(greetingMessage);
            botService.reply(replyToken, messages);
        } else {
            botService.reply(replyToken, greetingMessage);
        }
    }

    private void handleJointOrFollowEvent(String replyToken, Source source) {
        greetingMessage(replyToken, source, null);
    }

    private void handleMessageEvent(MessageEvent event) {
        String replyToken      = event.getReplyToken();
        MessageContent content = event.getMessage();
        Source source          = event.getSource();
        String senderId        = source.getUserId();
        sender                 = botService.getProfile(senderId);

        if (content instanceof TextMessageContent) {
            handleTextMessage(replyToken, (TextMessageContent) content, source);
        } else if (content instanceof StickerMessageContent){
            handleStickerMessage(event);
        } else {
            greetingMessage(replyToken, source, null);
        }
    }

    private void handleStickerMessage(MessageEvent event) {
        StickerMessageContent stickerMessageContent = (StickerMessageContent) event.getMessage();
        botService.replySticker(event.getReplyToken(), stickerMessageContent.getPackageId(), stickerMessageContent.getStickerId());
    }

    private void handleTextMessage(String replyToken, TextMessageContent content, Source source) {
        if (source instanceof GroupSource) {
            handleGroupChats(replyToken, content.getText(), ((GroupSource)source).getGroupId());
        } else if (source instanceof RoomSource) {
            handleRoomChats(replyToken, content.getText(), ((RoomSource)source).getRoomId());
        } else if (source instanceof UserSource) {
            handleOneOnOneChats(replyToken, content.getText());
        } else {
         botService.replyText(replyToken, "Unknown Message Source!");
        }

    }

    private void handleGroupChats(String replyToken, String textMessage, String groupId) {
        String msgText = textMessage.toLowerCase();
        if (msgText.contains("bot leave")) {
            if (sender == null) {
                botService.replyText(replyToken, "Halo, add InfoPolin sebagai teman dulu!");
            } else {
                botService.leaveGroup(groupId);
            }
        } else if (msgText.contains("info")
                || msgText.contains("apa itu")
        ) {
            processText(replyToken, textMessage);
        } else if (msgText.contains("menu")) {
            showMenu(replyToken);
        } else {
            handleFallbackMessage(replyToken, new GroupSource(groupId, sender.getUserId()));
        }
    }

    private void handleRoomChats(String replyToken, String textMessage, String roomId) {
        String msgText = textMessage.toLowerCase();
        if (msgText.contains("bot leave")) {
            if (sender == null) {
                botService.replyText(replyToken, "Halo, add InfoPolin sebagai teman dulu!");
            } else {
                botService.leaveRoom(roomId);
            }
        } else if (msgText.contains("info")
                || msgText.contains("apa itu")
        ) {
            processText(replyToken, textMessage);
        } else if (msgText.contains("menu")) {
            showMenu(replyToken);
        } else {
            handleFallbackMessage(replyToken, new RoomSource(roomId, sender.getUserId()));
        }
    }

    private void handleOneOnOneChats(String replyToken, String textMessage) {
        String msgText = textMessage.toLowerCase();
        if (msgText.contains("info")
                || msgText.contains("apa itu")
        ){
            processText(replyToken, msgText);
        } else if (msgText.contains("menu")){
            showMenu(replyToken);
        }
        else {
            handleFallbackMessage(replyToken, new UserSource(sender.getUserId()));
        }
    }

    private void processText(String replyToken, String messageText) {
        String[] words = messageText.trim().split("\\s+");
        String intent  = words[0];

        if (intent.equalsIgnoreCase("info")) {
            handleInfo(replyToken);
        } else if (intent.equalsIgnoreCase("apa itu politeknik")) {
            handleApaitu(replyToken);
        }
    }

    private void handleApaitu(String replyToken) {
        botContainer.replyApaItuPoli(replyToken);
    }

    private void handleInfo(String replyToken) {
        botService.replyText(replyToken, "IniText");
    }

    private void handleFallbackMessage(String replyToken, Source source) {
        greetingMessage(replyToken, source, "Hi "+sender.getDisplayName()+", aku belum mengerti maksud kamu. Silahkan ikuti petunjuk ya :)");
    }

    private void showMenu(String replyToken) {
        botContainer.replyMenu(replyToken);
    }
}
