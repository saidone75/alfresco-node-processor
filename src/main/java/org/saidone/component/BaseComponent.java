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

package org.saidone.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.saidone.misc.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
/**
 * Common base class for Spring-managed components in this application.
 * <p>
 * Provides lifecycle logging and a utility method to close the Spring context
 * and terminate the JVM with a specific exit code.
 */
public class BaseComponent implements ApplicationContextAware {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Stores the active Spring {@link ApplicationContext} for later shutdown.
     *
     * @param applicationContext the current application context
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Called after dependency injection is complete.
     * Logs a startup message indicating that the component is ready.
     */
    @PostConstruct
    public void start() {
        log.info("{} Starting {}", Constants.START_PREFIX, this.getClass().getSimpleName());
    }

    /**
     * Called just before the bean is destroyed.
     * Logs a shutdown message indicating that the component is stopping.
     */
    @PreDestroy
    public void stop() {
        log.info("{} Stopping {}", Constants.STOP_PREFIX, this.getClass().getSimpleName());
    }

    /**
     * Closes the Spring context and exits the JVM.
     *
     * @param exitCode process exit status returned to the operating system
     */
    public void shutDown(int exitCode) {
        log.debug("Shutting down application");
        ((ConfigurableApplicationContext) applicationContext).close();
        System.exit(exitCode);
    }

}
