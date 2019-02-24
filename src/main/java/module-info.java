/*
 * Copyright (c) 2019, Donovan Smith
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
//    requires grib;
    requires protobuf.java;
//    requires jdom;
    requires jsoup;
    requires jcip.annotations;
//    requires jj;
    requires com.google.common;
    requires failureaccess;
    requires listenablefuture;
//    requires jsr;
    requires checker.qual;
    requires error.prone.annotations;
    requires j2objc.annotations;
    requires animal.sniffer.annotations;
    requires cdm;
    requires udunits;
    requires httpservices;
    requires httpclient;
    requires commons.logging;
    requires commons.codec;
    requires httpmime;
    requires httpcore;
    requires joda.time;
    requires quartz;
//    requires c3p;
    requires mchange.commons.java;
    requires jcommander;
//    requires aws.java.sdk.s;
    requires aws.java.sdk.kms;
    requires aws.java.sdk.core;
    requires ion.java;
    requires jackson.dataformat.cbor;
    requires jmespath.java;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
//    requires netcdf;
    requires jna;
    requires slf4j.api;
    requires logback.classic;
    requires logback.core;
    requires javafx.controlsEmpty;
    requires javafx.controls;
    requires javafx.fxmlEmpty;
    requires javafx.fxml;
    requires javafx.swingEmpty;
    requires javafx.swing;
    requires javafx.graphicsEmpty;
    requires javafx.graphics;
    requires javafx.baseEmpty;
    requires javafx.base;
//    requires SkewTVSP;
    
    opens com.mccollinsmith.donovan.skewtvsp to javafx.fxml;
    exports com.mccollinsmith.donovan.skewtvsp;
}
