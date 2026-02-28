/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2026 Saidone
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.saidone.component.BaseComponent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LockService extends BaseComponent {

    private FileLock lock;
    private FileChannel channel;
    private RandomAccessFile randomFile;

    @PostConstruct
    public void acquireLock() {
        // Attempts to acquire file lock; exits if unavailable
        try {
            randomFile = new RandomAccessFile("anp.lock", "rw");
            channel = randomFile.getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                log.warn("The application is already running. Exit.");
                super.shutDown(0);
            }
            log.info("Lock acquired. Program running.");
        } catch (Exception e) {
            log.error("Could not acquire lock", e);
            super.shutDown(1);
        }
    }

    @PreDestroy
    public void releaseLock() {
        // Releases lock; closes channel and file
        try {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
            if (randomFile != null) randomFile.close();
        } catch (Exception ignored) {
        }
    }

}