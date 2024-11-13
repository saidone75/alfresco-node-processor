/*
 * Alfresco Node Processor - Do things with nodes
 * Copyright (C) 2023-2024 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.saidone.processors;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.core.handler.NodesApi;
import org.saidone.model.config.ProcessorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class CheckMailAttachmentsProcessor extends AbstractNodeProcessor {

    @Autowired
    private NodesApi nodesApi;

    @Autowired
    private AtomicInteger hasPostaCertAttachmentCounter;

    @Override
    public void processNode(String nodeId, ProcessorConfig config) {
        var node = Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry();
        log.debug("node name --> {}", node.getName());
        try {
            var nodeContent = Objects.requireNonNull(nodesApi.getNodeContent(node.getId(), null, null, null).getBody()).getContentAsString(StandardCharsets.UTF_8);
            // log.debug(nodeContent);
            if (!nodeContent.isEmpty()) {
                var message = parseMimeMessage(nodeContent);
                if (hasPostaCertAttachment(message)) hasPostaCertAttachmentCounter.incrementAndGet();
            } else {
                log.warn("0 byte message");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    private static MimeMessage parseMimeMessage(String rawEmail) throws Exception {
        var props = new Properties();
        var session = Session.getDefaultInstance(props, null);
        var emailStream = new ByteArrayInputStream(rawEmail.getBytes());
        return new MimeMessage(session, emailStream);
    }

    private static boolean hasPostaCertAttachment(MimeMessage mimeMessage) throws Exception {
        var hasPostaCertAttachment = Boolean.FALSE;
        if (mimeMessage.isMimeType("multipart/*")) {
            var multipart = (Multipart) mimeMessage.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                var bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                        (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {
                    var fileName = bodyPart.getFileName();
                    log.debug("found attachment => {}", fileName);
                    if (fileName.equals("postacert.eml")) hasPostaCertAttachment = Boolean.TRUE;
                }
            }
        } else {
            log.error("no attachments");
        }
        return hasPostaCertAttachment;
    }

}