package org.alfresco.core.handler;

public class NodesApiImpl implements NodesApi {
    public boolean called = false;
    @Override
    public void deleteNode(String id, Boolean permanent) {
        called = true;
    }
}
