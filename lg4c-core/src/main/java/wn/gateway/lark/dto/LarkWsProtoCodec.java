package wn.gateway.lark.dto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public final class LarkWsProtoCodec {
    private static final int FIELD_SEQ_ID = 1;
    private static final int FIELD_LOG_ID = 2;
    private static final int FIELD_SERVICE = 3;
    private static final int FIELD_METHOD = 4;
    private static final int FIELD_HEADERS = 5;
    private static final int FIELD_PAYLOAD_ENCODING = 6;
    private static final int FIELD_PAYLOAD_TYPE = 7;
    private static final int FIELD_PAYLOAD = 8;
    private static final int FIELD_LOG_ID_NEW = 9;

    private static final int FIELD_HEADER_KEY = 1;
    private static final int FIELD_HEADER_VALUE = 2;

    public LarkWsProtoFrame decode(byte[] bytes) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(bytes);
        long seqId = 0L;
        long logId = 0L;
        int service = 0;
        int method = 0;
        List<LarkWsProtoFrame.Header> headers = new ArrayList<>();
        String payloadEncoding = null;
        String payloadType = null;
        byte[] payload = new byte[0];
        String logIdNew = null;

        while (!input.isAtEnd()) {
            int tag = input.readTag();
            if (tag == 0) {
                break;
            }
            switch (WireFormat.getTagFieldNumber(tag)) {
                case FIELD_SEQ_ID -> seqId = input.readUInt64();
                case FIELD_LOG_ID -> logId = input.readUInt64();
                case FIELD_SERVICE -> service = input.readInt32();
                case FIELD_METHOD -> method = input.readInt32();
                case FIELD_HEADERS -> headers.add(readHeader(input));
                case FIELD_PAYLOAD_ENCODING -> payloadEncoding = input.readStringRequireUtf8();
                case FIELD_PAYLOAD_TYPE -> payloadType = input.readStringRequireUtf8();
                case FIELD_PAYLOAD -> payload = input.readByteArray();
                case FIELD_LOG_ID_NEW -> logIdNew = input.readStringRequireUtf8();
                default -> input.skipField(tag);
            }
        }

        return new LarkWsProtoFrame(seqId, logId, service, method, headers, payloadEncoding, payloadType, payload, logIdNew);
    }

    public byte[] encode(LarkWsProtoFrame frame) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(out);
        output.writeUInt64(FIELD_SEQ_ID, frame.seqId());
        output.writeUInt64(FIELD_LOG_ID, frame.logId());
        output.writeInt32(FIELD_SERVICE, frame.service());
        output.writeInt32(FIELD_METHOD, frame.method());
        for (LarkWsProtoFrame.Header header : frame.headers()) {
            byte[] headerBytes = encodeHeader(header);
            output.writeTag(FIELD_HEADERS, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            output.writeUInt32NoTag(headerBytes.length);
            output.writeRawBytes(headerBytes);
        }
        if (frame.payloadEncoding() != null) {
            output.writeString(FIELD_PAYLOAD_ENCODING, frame.payloadEncoding());
        }
        if (frame.payloadType() != null) {
            output.writeString(FIELD_PAYLOAD_TYPE, frame.payloadType());
        }
        if (frame.payload().length > 0) {
            output.writeByteArray(FIELD_PAYLOAD, frame.payload());
        }
        if (frame.logIdNew() != null) {
            output.writeString(FIELD_LOG_ID_NEW, frame.logIdNew());
        }
        output.flush();
        return out.toByteArray();
    }

    private LarkWsProtoFrame.Header readHeader(CodedInputStream input) throws IOException {
        int length = input.readRawVarint32();
        int previousLimit = input.pushLimit(length);
        String key = "";
        String value = "";
        while (!input.isAtEnd()) {
            int tag = input.readTag();
            if (tag == 0) {
                break;
            }
            switch (WireFormat.getTagFieldNumber(tag)) {
                case FIELD_HEADER_KEY -> key = input.readStringRequireUtf8();
                case FIELD_HEADER_VALUE -> value = input.readStringRequireUtf8();
                default -> input.skipField(tag);
            }
        }
        input.popLimit(previousLimit);
        return new LarkWsProtoFrame.Header(key, value);
    }

    private byte[] encodeHeader(LarkWsProtoFrame.Header header) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(out);
        output.writeString(FIELD_HEADER_KEY, header.key());
        output.writeString(FIELD_HEADER_VALUE, header.value());
        output.flush();
        return out.toByteArray();
    }
}
