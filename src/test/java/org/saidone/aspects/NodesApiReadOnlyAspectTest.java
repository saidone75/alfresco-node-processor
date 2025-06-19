package org.saidone.aspects;

import org.alfresco.core.handler.NodesApiImpl;
import org.alfresco.core.handler.NodesApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.aop.framework.ProxyFactory;

public class NodesApiReadOnlyAspectTest {

    @Test
    public void testDeleteSkippedWhenReadOnly() {
        NodesApiImpl target = new NodesApiImpl();
        NodesApiReadOnlyAspect aspect = new NodesApiReadOnlyAspect();
        ReflectionTestUtils.setField(aspect, "readOnly", true);
        ProxyFactory pf = new ProxyFactory(target);
        pf.addAspect(aspect);
        NodesApi proxy = (NodesApi) pf.getProxy();
        proxy.deleteNode("id", false);
        Assertions.assertFalse(target.called, "method should not be called in read-only mode");
    }

    @Test
    public void testDeleteProceedsWhenNotReadOnly() {
        NodesApiImpl target = new NodesApiImpl();
        NodesApiReadOnlyAspect aspect = new NodesApiReadOnlyAspect();
        ReflectionTestUtils.setField(aspect, "readOnly", false);
        ProxyFactory pf = new ProxyFactory(target);
        pf.addAspect(aspect);
        NodesApi proxy = (NodesApi) pf.getProxy();
        proxy.deleteNode("id", false);
        Assertions.assertTrue(target.called, "method should be called when not read-only");
    }
}
