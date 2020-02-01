package com.pnp.infopoli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
public class Controller {

    @Autowired
    @Qualifier("lineMssagingClient")
    private LineMessagingClient lineMessagingClient;

    @Autowired
    @Qualifier("lineSignatureValidator")
    private LineSignatureValidator lineSignatureValidator;

    @RequestMapping(value = "/api/callback", method = RequestMethod.POST)
    private ResponseEntity<String> callback(
            @RequestHeader("X-Line-Signature") String xLineSignature,
            @RequestBody String eventsPayLoad
    ){
        try {
            //
            //if (!lineSignatureValidator.validateSignature(eventsPayLoad.getBytes(), xLineSignature)){
            //    throw new RuntimeException("Invalid Signature Validation");
            //}

            //parsing event
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventsModel eventsModel = objectMapper.readValue(eventsPayLoad, EventsModel.class);

            eventsModel.getEvents().forEach(event -> {
                if (event instanceof MessageEvent){
                    if (event.getSource() instanceof GroupSource || event.getSource() instanceof RoomSource){
                        handleGroupRoomChats((MessageEvent) event);
                    } else {
                        handleOneOnOneChats((MessageEvent) event);
                    }
                }
            });

            return new ResponseEntity<>(HttpStatus.OK);
        }catch (IOException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void handleGroupRoomChats(MessageEvent event) {
        if (!event.getSource().getUserId().isEmpty()) {
            String userId = event.getSource().getUserId();
            UserProfileResponse profile = getProfile(userId);
            replyText(event.getReplyToken(), "Hello "+profile.getDisplayName());
        } else {
            replyText(event.getReplyToken(), "Hello, what's Ur Name?");
        }
    }

    private UserProfileResponse getProfile(String userId) {
        try {
            return lineMessagingClient.getProfile(userId).get();
        }catch (InterruptedException | ExecutionException e){
            throw new RuntimeException(e);
        }
    }

    private void handleOneOnOneChats(MessageEvent event) {
            if (event.getMessage() instanceof AudioMessageContent
                    || event.getMessage() instanceof ImageMessageContent
                    || event.getMessage() instanceof VideoMessageContent
                    || event.getMessage() instanceof FileMessageContent
            ){
                handleContentMessage(event);
            } else if (event.getMessage() instanceof StickerMessageContent){
                handleStickerMessage(event);
            } else if (event.getMessage() instanceof TextMessageContent){
                handleTextMessage(event);
            } else {
                replyText(event.getReplyToken(), "Unknown Message");
            }
        }

    private void handleStickerMessage(MessageEvent event) {
        StickerMessageContent stickerMessageContent = (StickerMessageContent) event.getMessage();
        replySticker(event.getReplyToken(), stickerMessageContent.getPackageId(), stickerMessageContent.getStickerId());
    }

    private void handleTextMessage(MessageEvent event) {
        TextMessageContent textMessageContent = (TextMessageContent) event.getMessage();

        if (textMessageContent.getText().toLowerCase().contains("menu")) {
            replyFlexMessage(event.getReplyToken());
        } else {
            replyText(event.getReplyToken(),"Maaf saya tidak mengerti maksud anda, silahkan ketik \"menu\" untuk memulai");
        }
    }

    private void handleContentMessage(MessageEvent event) {
        String baseURL      = "https://infopolin.herokuapp.com";
        String contentURL   = baseURL+"/content/"+event.getMessage().getId();
        String contentType  = event.getMessage().getClass().getSimpleName();

        replyText(event.getReplyToken(), "TerimaKasih");
        replySticker(event.getReplyToken(),"1","1");
    }

    private void replyText(String replyToken, String messageToUser) {
        TextMessage textMessage = new TextMessage(messageToUser);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, textMessage);
        reply(replyMessage);
    }

    private void replySticker(String replyToken, String packageId, String stickerId){
        StickerMessage stickerMessage = new StickerMessage(packageId, stickerId);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, stickerMessage);
        reply(replyMessage);
    }

    private void reply(ReplyMessage replyMessage) {
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        }catch (InterruptedException | ExecutionException e){
            throw new RuntimeException(e);
        }
    }

    private void replyFlexMessage(String replyToken){
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            String flexTemplate = IOUtils.toString(classLoader.getResourceAsStream("menus.json"));

            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            FlexContainer flexContainer = objectMapper.readValue(flexTemplate, FlexContainer.class);

            ReplyMessage replyMessage = new ReplyMessage(replyToken, new FlexMessage("Menu Utama", flexContainer));
            reply(replyMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
