/*
 * Copyright (c) 2020, Donovan Smith
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

module SkewTVirtualSoundingPlotter {
    requires cdm.core;
    requires udunits;
    requires httpservices;
    requires org.apache.httpcomponents.httpclient;
    requires commons.logging;
    requires org.apache.commons.codec;
    requires org.apache.httpcomponents.httpcore;
    requires jcommander;
    requires com.google.common;
    requires failureaccess;
    requires listenablefuture;
    requires org.checkerframework.checker.qual;
    requires error.prone.annotations;
    requires j2objc.annotations;
    requires animal.sniffer.annotations;
    requires com.google.protobuf;
    requires re2j;
    requires org.joda.time;
    requires jdom2;
    requires jsr305;
    requires org.slf4j;
    requires logback.classic;
    requires logback.core;
    requires javafx.controlsEmpty;
    requires javafx.controls;
    requires javafx.graphicsEmpty;
    requires javafx.graphics;
    requires javafx.baseEmpty;
    requires javafx.base;
    requires javafx.fxmlEmpty;
    requires javafx.fxml;
    requires javafx.swingEmpty;
    requires javafx.swing;
//    requires SkewTVirtualSoundingPlotter;
    
//    opens me.donovansmith.skewtvsp to javafx.fxml;
    opens me.donovansmith.skewtvsp.controllers to javafx.fxml;
    exports me.donovansmith.skewtvsp;
    exports me.donovansmith.skewtvsp.controllers;
}
