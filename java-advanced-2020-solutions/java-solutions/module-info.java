/**
 * This module contains java-advanced-2020 home-works
 * @author Darui99
 */
module ru.ifmo.rain.kurbatov {
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires java.compiler;
    requires java.rmi;
    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;
    requires org.junit.platform.launcher;
    requires org.junit.platform.engine;
    exports ru.ifmo.rain.kurbatov.bank;
}
