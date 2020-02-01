package com.pnp.infopoli.service;

import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.event.source.UserSource;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class BotTemplate {
    public TemplateMessage createButton(String message, String actionTitle, String actionText) {
        ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                null,
                null,
                message,
                Collections.singletonList(new MessageAction(actionTitle, actionText))
        );
        return new TemplateMessage(actionTitle, buttonsTemplate);
    }
    public TemplateMessage greetingMessage(Source source, UserProfileResponse sender) {
        String message = "Politeknik P.A.S.T.I \n Selamat Datang di Akun Info Politeknik Negeri \n" +
                         "Disini kita akan sharing tentang dunia vokasi dan info seputar politeknik \n" +
                         "untuk memulai silahkan ketik 'menu' ";
        String action  = "Menu";

        if (source instanceof GroupSource) {
            message = String.format(message, "Group");
        } else if (source instanceof RoomSource) {
            message = String.format(message, "Room");
        } else if (source instanceof UserSource) {
            message = String.format(message, sender.getDisplayName());
        } else {
            message = "Unknown Message Source!";
        }

        return createButton(message, action, action);
    }

    public String escape(String text) {
        return StringEscapeUtils.escapeJson(text.trim());
    }
}
