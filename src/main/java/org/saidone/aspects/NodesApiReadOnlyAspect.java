/*
 *  Alfresco Node Processor - Do things with nodes
 *  Copyright (C) 2023-2025 Saidone
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

package org.saidone.aspects;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect that prevents write operations on Alfresco when the application
 * is configured in read-only mode.
 */
@Aspect
@Component
@Slf4j
public class NodesApiReadOnlyAspect {

    @Value("${application.read-only:true}")
    private boolean readOnly;

    @Around(
            "execution(* org.alfresco.core.handler.NodesApi.copy*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.create*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.delete*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.lock*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.move*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.unlock*(..)) || " +
                    "execution(* org.alfresco.core.handler.NodesApi.update*(..))")

    public Object enforceReadOnly(ProceedingJoinPoint pjp) throws Throwable {
        // Define the package prefix for which enforcement applies
        final String enforcedCallerPackage = "org.saidone.processors";

        // Inspect the call stack to find the caller class
        val stack = Thread.currentThread().getStackTrace();

        // stack[0] = Thread.getStackTrace
        // stack[1] = this method (enforceReadOnly)
        // stack[2] = AspectJ infrastructure
        // stack[3] and onwards = caller frames

        boolean callerIsEnforcedPackage = false;
        for (int i = 3; i < stack.length; i++) {
            val className = stack[i].getClassName();
            if (className.startsWith(enforcedCallerPackage)) {
                callerIsEnforcedPackage = true;
                break;
            }
        }

        if (!callerIsEnforcedPackage) {
            // Caller not in the enforced package, proceed without enforcing read-only
            return pjp.proceed();
        }

        if (readOnly) {
            log.warn("Read-only mode - skipping call to {} with args {}",
                    pjp.getSignature().getName(), Arrays.toString(pjp.getArgs()));
            return null;
        }
        return pjp.proceed();
    }

}
