package ru.zyulyaev.ifmo.dkvs.server;

/**
 * Created by nikita on 16.05.15.
 */
public enum NodeStatus {
    CREATED,
    INITIALIZING,
    NORMAL,
    VIEW_CHANGE,
    RECOVERING,
    STOPPED
}
