package org.saidone.aspects;

import lombok.extern.slf4j.Slf4j;
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
        "execution(* org.alfresco.core.handler.NodesApi.create*(..)) || " +
        "execution(* org.alfresco.core.handler.NodesApi.update*(..)) || " +
        "execution(* org.alfresco.core.handler.NodesApi.delete*(..)) || " +
        "execution(* org.alfresco.core.handler.NodesApi.move*(..)) || " +
        "execution(* org.alfresco.core.handler.NodesApi.set*(..))")
    public Object enforceReadOnly(ProceedingJoinPoint pjp) throws Throwable {
        if (readOnly) {
            log.warn("Read-only mode - skipping call to {} with args {}",
                    pjp.getSignature().getName(), Arrays.toString(pjp.getArgs()));
            return null;
        }
        return pjp.proceed();
    }
}
